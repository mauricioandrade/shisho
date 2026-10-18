package dev.mauricioandrade.shisho.infrastructure.build;

import dev.mauricioandrade.shisho.domain.exception.BuildFileException;
import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.Dependency;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.port.out.BuildFileReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads the Java version and dependencies from a {@code pom.xml} and from the poms of its modules
 * ({@code <modules>}, or Maven 4's {@code <subprojects>}), nested ones and those of any
 * {@code <profile>} included. Only what each pom declares locally is considered: a remote parent (such as {@code spring-boot-starter-parent}) is
 * never downloaded. See {@code docs/adr/0015-multi-module.md}.
 */
public final class MavenBuildFileReader implements BuildFileReader {

    private static final String POM = "pom.xml";
    private static final String COMPILER_PLUGIN = "maven-compiler-plugin";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");
    private static final int MAX_PLACEHOLDER_DEPTH = 10;

    @Override
    public Optional<BuildInfo> read(Path projectRoot) {
        Path pom = projectRoot.resolve(POM);
        if (!Files.isRegularFile(pom)) {
            return Optional.empty();
        }
        Element project = parse(pom);
        List<BuildInfo> modules = new ArrayList<>();
        addModules(projectRoot, Path.of(""), project, new HashSet<>(Set.of(Path.of(""))), modules);
        return Optional.of(new BuildInfo(Path.of(POM), javaVersion(pom, project), dependencies(project), modules, platformVersions(project)));
    }

    /**
     * The modules of {@code project}, whose pom is in {@code directory}, and theirs, in the order the
     * poms list them. A profile's modules count whether or not it is active: their code is analyzed
     * anyway, so their dependencies must be known too. A module outside the project, or one already read, is skipped: the poms come
     * from an untrusted project, and two poms may list each other.
     */
    private static void addModules(Path projectRoot, Path directory, Element project, Set<Path> read, List<BuildInfo> modules) {
        List<String> names = Stream.concat(Stream.of(project), profiles(project))
                .flatMap(owner -> Stream.concat(listed(owner, "modules", "module"), listed(owner, "subprojects", "subproject")))
                .toList();
        for (String name : names) {
            Path module = directory.resolve(name).normalize();
            Path pom = projectRoot.resolve(module).resolve(POM);
            if (module.isAbsolute() || module.startsWith("..") || !Files.isRegularFile(pom) || !read.add(module)) {
                continue;
            }
            Element element = parse(pom);
            modules.add(new BuildInfo(module.resolve(POM), javaVersion(pom, element), dependencies(element), List.of(), platformVersions(element)));
            addModules(projectRoot, module, element, read, modules);
        }
    }

    private static Stream<Element> profiles(Element project) {
        return child(project, "profiles").stream().flatMap(profiles -> children(profiles, "profile").stream());
    }

    /** The directories in {@code <section><item>...</item></section>}. */
    private static Stream<String> listed(Element owner, String section, String item) {
        return child(owner, section).stream()
                .flatMap(list -> children(list, item).stream())
                .map(element -> element.getTextContent().strip())
                .filter(name -> !name.isEmpty());
    }

    /**
     * Candidates in the order Maven itself gives them precedence: {@code release} wins over
     * {@code source}, and plugin configuration wins over properties. Spring Boot's
     * {@code java.version} feeds {@code maven.compiler.release} in its parent pom.
     */
    private static Optional<JavaVersion> javaVersion(Path pom, Element project) {
        Map<String, String> properties = properties(project);
        Optional<Element> compiler = compilerConfiguration(project);
        List<String> candidates = Stream.of(
                        compiler.flatMap(configuration -> text(configuration, "release")),
                        Optional.ofNullable(properties.get("maven.compiler.release")),
                        Optional.ofNullable(properties.get("java.version")),
                        compiler.flatMap(configuration -> text(configuration, "source")),
                        Optional.ofNullable(properties.get("maven.compiler.source")))
                .flatMap(Optional::stream)
                .toList();
        for (String candidate : candidates) {
            String resolved = resolve(candidate, properties);
            if (PLACEHOLDER.matcher(resolved).find()) {
                continue; // defined in a parent pom we cannot see
            }
            return Optional.of(JavaVersion.parse(resolved).orElseThrow(() ->
                    new BuildFileException("build.error.invalid-java-version", pom.toString(), candidate)));
        }
        return Optional.empty();
    }

    private static String resolve(String value, Map<String, String> properties) {
        String resolved = value;
        for (int depth = 0; depth < MAX_PLACEHOLDER_DEPTH; depth++) {
            Matcher matcher = PLACEHOLDER.matcher(resolved);
            String next = matcher.replaceAll(match ->
                    Matcher.quoteReplacement(properties.getOrDefault(match.group(1), match.group())));
            if (next.equals(resolved)) {
                return resolved;
            }
            resolved = next;
        }
        return resolved;
    }

    private static Map<String, String> properties(Element project) {
        Map<String, String> properties = new HashMap<>();
        child(project, "properties").ifPresent(section ->
                childElements(section).forEach(property ->
                        properties.put(property.getTagName(), property.getTextContent().strip())));
        return properties;
    }

    private static Optional<Element> compilerConfiguration(Element project) {
        return child(project, "build")
                .flatMap(build -> child(build, "plugins"))
                .stream()
                .flatMap(plugins -> children(plugins, "plugin").stream())
                .filter(plugin -> text(plugin, "artifactId").filter(COMPILER_PLUGIN::equals).isPresent())
                .findFirst()
                .flatMap(plugin -> child(plugin, "configuration"));
    }

    /**
     * The {@code <parent>} ({@code spring-boot-starter-parent}) and the BOMs imported in
     * {@code <dependencyManagement>} ({@code quarkus-bom}), by groupId. A version left as a
     * placeholder the pom does not define is skipped.
     */
    private static Map<String, String> platformVersions(Element project) {
        Map<String, String> properties = properties(project);
        Stream<Element> parent = child(project, "parent").stream();
        Stream<Element> boms = child(project, "dependencyManagement")
                .flatMap(management -> child(management, "dependencies"))
                .stream()
                .flatMap(section -> children(section, "dependency").stream())
                .filter(dependency -> text(dependency, "scope").filter("import"::equals).isPresent());
        Map<String, String> versions = new HashMap<>();
        Stream.concat(parent, boms).forEach(platform -> text(platform, "groupId").ifPresent(groupId ->
                text(platform, "version")
                        .map(version -> resolve(version, properties))
                        .filter(version -> !PLACEHOLDER.matcher(version).find())
                        .ifPresent(version -> versions.putIfAbsent(resolve(groupId, properties), version))));
        return versions;
    }

    private static List<Dependency> dependencies(Element project) {
        return child(project, "dependencies").stream()
                .flatMap(section -> children(section, "dependency").stream())
                .flatMap(dependency -> text(dependency, "groupId")
                        .flatMap(groupId -> text(dependency, "artifactId")
                                .map(artifactId -> new Dependency(groupId, artifactId)))
                        .stream())
                .toList();
    }

    private static Element parse(Path pom) {
        Element root;
        try {
            root = newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
        } catch (SAXException e) {
            throw new BuildFileException("build.error.invalid-xml", e, pom.toString(), e.getMessage());
        } catch (IOException e) {
            throw new BuildFileException("build.error.read", e, pom.toString());
        }
        if (!root.getTagName().equals("project")) {
            throw new BuildFileException("build.error.not-a-pom", pom.toString(), root.getTagName());
        }
        return root;
    }

    /** Analyzed projects are untrusted input: no DOCTYPE, so no external entities (XXE). */
    private static DocumentBuilder newDocumentBuilder() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ThrowingErrorHandler());
            return builder;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("JDK XML parser does not support secure processing", e);
        }
    }

    private static Optional<String> text(Element parent, String name) {
        return child(parent, name).map(element -> element.getTextContent().strip()).filter(text -> !text.isEmpty());
    }

    private static Optional<Element> child(Element parent, String name) {
        return children(parent, name).stream().findFirst();
    }

    private static List<Element> children(Element parent, String name) {
        return childElements(parent).stream().filter(element -> element.getTagName().equals(name)).toList();
    }

    private static List<Element> childElements(Element parent) {
        NodeList nodes = parent.getChildNodes();
        List<Element> elements = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) nodes.item(i));
            }
        }
        return elements;
    }

    /** The default handler prints to stderr; we want the error as an exception instead. */
    private static final class ThrowingErrorHandler implements ErrorHandler {

        @Override
        public void warning(SAXParseException exception) {
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
    }
}
