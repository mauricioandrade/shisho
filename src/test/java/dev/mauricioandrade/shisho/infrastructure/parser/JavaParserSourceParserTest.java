package dev.mauricioandrade.shisho.infrastructure.parser;

import com.github.javaparser.ParserConfiguration.LanguageLevel;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Annotation;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Field;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Import;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Invocation;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Kind;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Parameter;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.SourceFile;
import dev.mauricioandrade.shisho.domain.model.SourceFile.Parsed;
import dev.mauricioandrade.shisho.domain.model.SourceFile.Unparseable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class JavaParserSourceParserTest {

    private static final JavaVersion JAVA_21 = new JavaVersion(21);
    private static final JavaVersion JAVA_25 = new JavaVersion(25);
    private static final Path JAVA25_FIXTURE = Path.of("src/test/resources/fixtures/java25");

    private final JavaParserSourceParser parser = new JavaParserSourceParser();

    @TempDir
    Path project;

    @Nested
    class LanguageLevels {

        @Test
        void parsesJava25SyntaxAtLevel25() {
            List<SourceFile> files = parser.parse(JAVA25_FIXTURE, JAVA_25);

            assertThat(files).singleElement().isInstanceOfSatisfying(Parsed.class, parsed -> {
                ClassUnit fixture = parsed.classes().getFirst();
                assertThat(fixture.name()).isEqualTo("Java25Fixture");
                assertThat(fixture.imports()).contains(new Import("java.base", false, false, true, 3));
            });
        }

        @Test
        void rejectsJava25SyntaxAtLevel21() {
            List<SourceFile> files = parser.parse(JAVA25_FIXTURE, JAVA_21);

            assertThat(files).singleElement().isInstanceOfSatisfying(Unparseable.class, unparseable -> {
                assertThat(unparseable.file()).isEqualTo(Path.of("src/main/java/fixture/Java25Fixture.java"));
                assertThat(unparseable.line()).isEqualTo(3);
                assertThat(unparseable.problem()).contains("Module imports");
            });
        }

        @Test
        void mapsToTheMatchingParserLevel() {
            assertThat(JavaParserSourceParser.languageLevel(JAVA_21)).isEqualTo(LanguageLevel.JAVA_21);
            assertThat(JavaParserSourceParser.languageLevel(new JavaVersion(8))).isEqualTo(LanguageLevel.JAVA_8);
        }

        @Test
        void futureVersionsUseTheHighestKnownLevelAndNeverPreview() {
            LanguageLevel level = JavaParserSourceParser.languageLevel(new JavaVersion(99));

            assertThat(level.name()).matches("JAVA_\\d+");
            assertThat(level).isGreaterThanOrEqualTo(LanguageLevel.JAVA_25);
        }

        @Test
        void parsesShishoItself() {
            assertThat(parser.parse(Path.of(""), JAVA_25))
                    .isNotEmpty()
                    .allSatisfy(file -> assertThat(file).isInstanceOf(Parsed.class));
        }
    }

    @Nested
    class SourceDiscovery {

        @Test
        void skipsTestSourcesBuildOutputAndHiddenDirectories() throws IOException {
            write("src/main/java/app/App.java", "package app; class App {}");
            write("module/src/main/java/app/Module.java", "package app; class Module {}");
            write("src/test/java/app/AppTest.java", "package app; class AppTest {}");
            write("module/src/test/java/app/ModuleTest.java", "package app; class ModuleTest {}");
            write("target/generated-sources/Generated.java", "class Generated {}");
            write("build/Gradle.java", "class Gradle {}");
            write(".git/Hidden.java", "class Hidden {}");
            write("src/main/resources/notes.txt", "não é Java");

            assertThat(parser.parse(project, JAVA_21)).extracting(SourceFile::file).containsExactly(
                    Path.of("module/src/main/java/app/Module.java"),
                    Path.of("src/main/java/app/App.java"));
        }

        @Test
        void skipsEverySourceSetButMain() throws IOException {
            write("src/main/java/app/App.java", "package app; class App {}");
            write("src/integration-test/java/app/AppIT.java", "package app; class AppIT {}");
            write("module/src/testFixtures/java/app/Fixtures.java", "package app; class Fixtures {}");
            write("src/jmh/java/app/Benchmark.java", "package app; class Benchmark {}");
            write("src/functionalTest/groovy/app/AppFT.java", "package app; class AppFT {}");
            write("legacy/src/com/acme/Legacy.java", "package com.acme; class Legacy {}");

            assertThat(parser.parse(project, JAVA_21)).as("sources straight under src stay").extracting(SourceFile::file)
                    .containsExactly(Path.of("legacy/src/com/acme/Legacy.java"), Path.of("src/main/java/app/App.java"));
        }

        @Test
        void aBuildDirectoryNameInsideSourcesIsAPackage() throws IOException {
            write("out/production/Compiled.java", "class Compiled {}");
            write("src/main/java/app/port/out/Repository.java", "package app.port.out; interface Repository {}");
            write("src/main/java/app/build/Builder.java", "package app.build; class Builder {}");

            assertThat(parser.parse(project, JAVA_21)).extracting(SourceFile::file).containsExactly(
                    Path.of("src/main/java/app/build/Builder.java"),
                    Path.of("src/main/java/app/port/out/Repository.java"));
        }

        @Test
        void reportsUnparseableFileWithoutStoppingTheOthers() throws IOException {
            write("src/main/java/app/Broken.java", "package app;\n\nclass Broken {\n  void x( {}\n}");
            write("src/main/java/app/Fine.java", "package app; class Fine {}");

            assertThat(parser.parse(project, JAVA_21))
                    .satisfiesExactly(
                            broken -> assertThat(broken).isInstanceOfSatisfying(Unparseable.class, unparseable ->
                                    assertThat(unparseable.line()).isEqualTo(4)),
                            fine -> assertThat(fine).isInstanceOf(Parsed.class));
        }
    }

    @Nested
    class Mapping {

        @Test
        void mapsPackageKindAndClassAnnotations() throws IOException {
            List<ClassUnit> classes = parse("""
                    package app.web;

                    import java.util.List;
                    import static java.util.Objects.requireNonNull;
                    import org.springframework.web.bind.annotation.*;

                    @RestController
                    @org.springframework.web.bind.annotation.RequestMapping("/users")
                    class UserController {}

                    record UserResponse(String name) {}
                    interface UserPort {}
                    enum Role { ADMIN }
                    @interface Marker {}
                    """);

            ClassUnit controller = classes.getFirst();
            assertThat(controller.packageName()).isEqualTo("app.web");
            assertThat(controller.line()).isEqualTo(7);
            assertThat(controller.annotations()).extracting(Annotation::simpleName)
                    .containsExactly("RestController", "RequestMapping");
            assertThat(controller.annotations().get(1).name())
                    .isEqualTo("org.springframework.web.bind.annotation.RequestMapping");
            assertThat(controller.imports()).containsExactly(
                    new Import("java.util.List", false, false, false, 3),
                    new Import("java.util.Objects.requireNonNull", true, false, false, 4),
                    new Import("org.springframework.web.bind.annotation", false, true, false, 5));
            assertThat(classes).extracting(ClassUnit::name, ClassUnit::kind).containsExactly(
                    tuple("UserController", Kind.CLASS),
                    tuple("UserResponse", Kind.RECORD),
                    tuple("UserPort", Kind.INTERFACE),
                    tuple("Role", Kind.ENUM),
                    tuple("Marker", Kind.ANNOTATION));
        }

        @Test
        void mapsSupertypesWithoutTypeArgumentsAndModifiers() throws IOException {
            List<ClassUnit> classes = parse("""
                    package app;

                    public sealed interface Payment permits Pix {}
                    final record Pix(String key) implements Payment, java.io.Serializable {}
                    interface Orders extends org.springframework.data.jpa.repository.JpaRepository<Order, Long> {}
                    abstract class Base extends Thread implements Runnable {}
                    enum Role implements Comparable<Role> { ADMIN }
                    """);

            assertThat(classes).extracting(ClassUnit::name, ClassUnit::supertypes).containsExactly(
                    tuple("Payment", List.of()),
                    tuple("Pix", List.of("Payment", "Serializable")),
                    tuple("Orders", List.of("JpaRepository")),
                    tuple("Base", List.of("Thread", "Runnable")),
                    tuple("Role", List.of("Comparable")));
            assertThat(classes.get(0).modifiers()).containsExactlyInAnyOrder("public", "sealed");
            assertThat(classes.get(1).modifiers()).containsExactly("final");
            assertThat(classes.get(3).modifiers()).containsExactly("abstract");
        }

        @Test
        void mapsFieldsOnePerVariable() throws IOException {
            ClassUnit service = parse("""
                    class UserService {
                        @Autowired
                        private UserRepository repository;
                        private final Clock clock;
                        private static final int LIMIT = 10;
                        int a, b = 2;
                    }
                    """).getFirst();

            assertThat(service.fields()).containsExactly(
                    new Field("repository", "UserRepository", List.of(new Annotation("Autowired", 2)), false, false, true, false, 2),
                    new Field("clock", "Clock", List.of(), true, false, true, false, 4),
                    new Field("LIMIT", "int", List.of(), true, true, true, true, 5),
                    new Field("a", "int", List.of(), false, false, false, false, 6),
                    new Field("b", "int", List.of(), false, false, false, true, 6));
        }

        @Test
        void mapsConstructorsMethodsAndParameterAnnotations() throws IOException {
            ClassUnit controller = parse("""
                    class UserController {
                        UserController(@Value("${limit}") int limit) {}

                        @PostMapping
                        void create(@Valid @RequestBody CreateUser request, String other) {}
                    }
                    """).getFirst();

            assertThat(controller.constructors()).singleElement().satisfies(constructor -> {
                assertThat(constructor.line()).isEqualTo(2);
                assertThat(constructor.parameters()).containsExactly(
                        new Parameter("limit", "int", List.of(new Annotation("Value", 2, Map.of("value", "${limit}"))), 2));
            });
            assertThat(controller.methods()).singleElement().satisfies(method -> {
                assertThat(method.name()).isEqualTo("create");
                assertThat(method.returnType()).isEqualTo("void");
                assertThat(method.line()).isEqualTo(4);
                assertThat(method.annotations()).extracting(Annotation::name).containsExactly("PostMapping");
                assertThat(method.parameters()).containsExactly(
                        new Parameter("request", "CreateUser", List.of(new Annotation("Valid", 5), new Annotation("RequestBody", 5)), 5),
                        new Parameter("other", "String", List.of(), 5));
            });
        }

        @Test
        void mapsAnnotationAttributesAndReturnTypes() throws IOException {
            ClassUnit service = parse("""
                    @ConfigurationProperties(prefix = "mail", ignoreUnknownFields = false)
                    class MailService {
                        @Value("${mail.host:localhost}")
                        private String host;

                        @Transactional(readOnly = true, timeout = 5)
                        List<Mail> findAll() { return List.of(); }
                    }
                    """).getFirst();

            assertThat(service.annotations().getFirst().attributes())
                    .containsExactlyInAnyOrderEntriesOf(Map.of("prefix", "mail", "ignoreUnknownFields", "false"));
            assertThat(service.fields().getFirst().annotations().getFirst().attribute("value"))
                    .contains("${mail.host:localhost}");
            assertThat(service.methods()).singleElement().satisfies(method -> {
                assertThat(method.returnType()).isEqualTo("List<Mail>");
                assertThat(method.annotations().getFirst().attribute("readOnly")).contains("true");
                assertThat(method.annotations().getFirst().attribute("propagation")).isEmpty();
            });
        }

        @Test
        void representsRecordComponentsAsTheCanonicalConstructor() throws IOException {
            ClassUnit properties = parse("""
                    record MailProperties(@Value("${mail.host}") String host, int port) {}
                    """).getFirst();

            assertThat(properties.fields()).isEmpty();
            assertThat(properties.constructors()).singleElement().satisfies(constructor ->
                    assertThat(constructor.parameters()).containsExactly(
                            new Parameter("host", "String", List.of(new Annotation("Value", 1, Map.of("value", "${mail.host}"))), 1),
                            new Parameter("port", "int", List.of(), 1)));
        }

        @Test
        void mapsInvocationsAndMethodReferences() throws IOException {
            ClassUnit report = parse("""
                    class Report {
                        void print(List<String> lines) {
                            System.out.println("x");
                            lines.forEach(System.err::println);
                            try {
                                run();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            this.out.flush();
                            build().print();
                        }
                    }
                    """).getFirst();

            Optional<String> print = Optional.of("print");
            assertThat(report.invocations()).containsExactly(
                    new Invocation(Optional.of("System.out"), "println", 3, print, 1),
                    new Invocation(Optional.of("lines"), "forEach", 4, print, 1),
                    new Invocation(Optional.of("System.err"), "println", 4, print, Invocation.UNKNOWN_ARGUMENTS),
                    new Invocation(Optional.empty(), "run", 6, print, 0),
                    new Invocation(Optional.of("e"), "printStackTrace", 8, print, 0),
                    new Invocation(Optional.of("this.out"), "flush", 10, print, 0),
                    new Invocation(Optional.empty(), "print", 11, print, 0),
                    new Invocation(Optional.empty(), "build", 11, print, 0));
        }

        @Test
        void aCallBelongsToTheOutermostMethodOfItsType() throws IOException {
            ClassUnit service = parse("""
                    class Service {
                        private final Runnable startup = () -> load();

                        Service() {
                            load();
                        }

                        void close() {
                            items.forEach(item -> save(item));
                            executor.submit(new Runnable() {
                                public void run() { save(null); }
                            });
                        }
                    }
                    """).getFirst();

            assertThat(service.invocations()).extracting(Invocation::name, Invocation::caller).containsExactly(
                    tuple("load", Optional.empty()),
                    tuple("load", Optional.empty()),
                    tuple("forEach", Optional.of("close")),
                    tuple("save", Optional.of("close")),
                    tuple("submit", Optional.of("close")),
                    tuple("save", Optional.of("close")));
        }

        @Test
        void nestedTypesOwnTheirMembersButLambdasAndAnonymousClassesDoNot() throws IOException {
            List<ClassUnit> classes = parse("""
                    class Outer {
                        private int outerField;

                        void run() {
                            Runnable lambda = () -> System.out.println("lambda");
                            Runnable anonymous = new Runnable() {
                                public void run() { System.out.println("anonymous"); }
                            };
                        }

                        static class Inner {
                            private int innerField;
                            void log() { System.err.println("inner"); }
                        }
                    }
                    """);

            assertThat(classes).extracting(ClassUnit::name).containsExactly("Outer", "Inner");
            ClassUnit outer = classes.get(0);
            ClassUnit inner = classes.get(1);
            assertThat(outer.fields()).extracting(Field::name).containsExactly("outerField");
            assertThat(outer.methods()).extracting(ClassUnit.Method::name).containsExactly("run");
            assertThat(outer.invocations()).extracting(Invocation::line).containsExactly(5, 7);
            assertThat(inner.fields()).extracting(Field::name).containsExactly("innerField");
            assertThat(inner.invocations()).extracting(Invocation::target).containsExactly(Optional.of("System.err"));
        }

        @Test
        void mapsMethodModifiersAndCatchBlocks() throws IOException {
            ClassUnit service = parse("""
                    class Service {
                        private static final void a() {
                            try {
                                run();
                            } catch (java.io.IOException | IllegalStateException e) {
                                // nothing
                            }
                        }
                        void b() {
                            Runnable r = () -> {
                                try { run(); } catch (Exception _) { log(); }
                            };
                        }
                    }
                    """).getFirst();

            assertThat(service.methods()).extracting(ClassUnit.Method::modifiers)
                    .containsExactly(Set.of("private", "static", "final"), Set.of());
            assertThat(service.catches()).as("a comment is no statement; a lambda's block is the type's").containsExactly(
                    new ClassUnit.CatchClause(List.of("java.io.IOException", "IllegalStateException"), "e", true, true, 5),
                    new ClassUnit.CatchClause(List.of("Exception"), "_", false, false, 11));
        }

        private List<ClassUnit> parse(String source) throws IOException {
            write("src/main/java/Sample.java", source);
            return parser.parse(project, JAVA_25).stream()
                    .map(file -> assertThat(file).isInstanceOf(Parsed.class).actual())
                    .map(Parsed.class::cast)
                    .flatMap(parsed -> parsed.classes().stream())
                    .toList();
        }
    }

    private void write(String relativePath, String content) throws IOException {
        Path file = project.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
