package dev.mauricioandrade.shisho.infrastructure.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.Problem;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import dev.mauricioandrade.shisho.domain.exception.SourceException;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Annotation;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Constructor;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Field;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Import;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Invocation;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Kind;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Method;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Parameter;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.SourceFile;
import dev.mauricioandrade.shisho.domain.port.out.SourceParser;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@link SourceParser} backed by JavaParser. The language level is always explicit: JavaParser's
 * default is Java 11, which rejects records and sealed types (see docs/adr/0001-parser.md).
 */
public final class JavaParserSourceParser implements SourceParser {

    private static final Set<String> IGNORED_DIRECTORIES = Set.of("target", "build", "out", "node_modules");
    private static final String JAVA_EXTENSION = ".java";

    @Override
    public List<SourceFile> parse(Path projectRoot, JavaVersion languageLevel) {
        JavaParser parser = new JavaParser(new ParserConfiguration().setLanguageLevel(languageLevel(languageLevel)));
        return productionSources(projectRoot).stream()
                .map(file -> parse(parser, projectRoot, file))
                .toList();
    }

    /** The highest JavaParser level not above {@code version}; never a preview level. */
    static LanguageLevel languageLevel(JavaVersion version) {
        return Arrays.stream(LanguageLevel.values())
                .filter(level -> level.name().matches("JAVA_\\d+"))
                .filter(level -> feature(level) <= version.feature())
                .max(Comparator.comparingInt(JavaParserSourceParser::feature))
                .orElse(LanguageLevel.JAVA_1_0);
    }

    private static int feature(LanguageLevel level) {
        return Integer.parseInt(level.name().substring("JAVA_".length()));
    }

    private static List<Path> productionSources(Path projectRoot) {
        List<Path> sources = new ArrayList<>();
        try {
            Files.walkFileTree(projectRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                    if (directory.equals(projectRoot)) {
                        return FileVisitResult.CONTINUE;
                    }
                    String name = directory.getFileName().toString();
                    boolean skip = name.startsWith(".") || IGNORED_DIRECTORIES.contains(name) || isTestSources(projectRoot.relativize(directory));
                    return skip ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    if (attributes.isRegularFile() && file.getFileName().toString().endsWith(JAVA_EXTENSION)) {
                        sources.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new SourceException("source.error.read", e, projectRoot.toString());
        }
        sources.sort(Comparator.naturalOrder());
        return sources;
    }

    /** {@code src/test}, at any depth, so multi-module layouts are covered too. */
    private static boolean isTestSources(Path relativeDirectory) {
        int names = relativeDirectory.getNameCount();
        return names >= 2
                && relativeDirectory.getName(names - 2).toString().equals("src")
                && relativeDirectory.getName(names - 1).toString().equals("test");
    }

    private static SourceFile parse(JavaParser parser, Path projectRoot, Path file) {
        Path relativeFile = projectRoot.relativize(file);
        ParseResult<CompilationUnit> result;
        try {
            result = parser.parse(file);
        } catch (IOException e) {
            return new SourceFile.Unparseable(relativeFile, 0, e.getMessage());
        }
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            Problem problem = result.getProblems().getFirst();
            int line = problem.getLocation().flatMap(TokenRange::toRange).map(range -> range.begin.line).orElse(0);
            return new SourceFile.Unparseable(relativeFile, line, problem.getMessage());
        }
        return new SourceFile.Parsed(relativeFile, classes(relativeFile, result.getResult().get()));
    }

    private static List<ClassUnit> classes(Path file, CompilationUnit unit) {
        String packageName = unit.getPackageDeclaration().map(NodeWithName::getNameAsString).orElse("");
        List<Import> imports = unit.getImports().stream()
                .map(declaration -> new Import(
                        declaration.getNameAsString(), declaration.isStatic(), declaration.isAsterisk(), declaration.isModule()))
                .toList();
        List<ClassUnit> classes = new ArrayList<>();
        for (TypeDeclaration<?> type : unit.findAll(TypeDeclaration.class)) {
            classes.add(new ClassUnit(
                    file,
                    packageName,
                    type.getNameAsString(),
                    kind(type),
                    line(type),
                    annotations(type.getAnnotations()),
                    imports,
                    fields(type),
                    constructors(type),
                    methods(type),
                    invocations(type)));
        }
        return classes;
    }

    private static Kind kind(TypeDeclaration<?> type) {
        return switch (type) {
            case RecordDeclaration _ -> Kind.RECORD;
            case EnumDeclaration _ -> Kind.ENUM;
            case AnnotationDeclaration _ -> Kind.ANNOTATION;
            case ClassOrInterfaceDeclaration declaration when declaration.isInterface() -> Kind.INTERFACE;
            case ClassOrInterfaceDeclaration _ -> Kind.CLASS;
            default -> throw new IllegalStateException("Unexpected type declaration: " + type.getClass());
        };
    }

    private static List<Field> fields(TypeDeclaration<?> type) {
        List<Field> fields = new ArrayList<>();
        for (FieldDeclaration declaration : type.getFields()) {
            List<Annotation> annotations = annotations(declaration.getAnnotations());
            declaration.getVariables().forEach(variable -> fields.add(new Field(
                    variable.getNameAsString(),
                    variable.getTypeAsString(),
                    annotations,
                    declaration.isFinal(),
                    declaration.isStatic(),
                    variable.getInitializer().isPresent(),
                    line(declaration))));
        }
        return fields;
    }

    private static List<Constructor> constructors(TypeDeclaration<?> type) {
        Stream<Constructor> canonical = type instanceof RecordDeclaration record
                ? Stream.of(new Constructor(List.of(), parameters(record.getParameters()), line(record)))
                : Stream.empty();
        Stream<Constructor> declared = type.getConstructors().stream()
                .map(constructor -> new Constructor(
                        annotations(constructor.getAnnotations()), parameters(constructor.getParameters()), line(constructor)));
        return Stream.concat(canonical, declared).toList();
    }

    private static List<Method> methods(TypeDeclaration<?> type) {
        return type.getMethods().stream()
                .map(method -> new Method(
                        method.getNameAsString(),
                        annotations(method.getAnnotations()),
                        parameters(method.getParameters()),
                        line(method)))
                .toList();
    }

    private static List<Parameter> parameters(List<com.github.javaparser.ast.body.Parameter> parameters) {
        return parameters.stream()
                .map(parameter -> new Parameter(
                        parameter.getNameAsString(), parameter.getTypeAsString(), annotations(parameter.getAnnotations())))
                .toList();
    }

    /** Calls inside lambdas and anonymous classes belong to the enclosing type; nested types own theirs. */
    private static List<Invocation> invocations(TypeDeclaration<?> type) {
        List<Invocation> invocations = new ArrayList<>();
        type.walk(Node.TreeTraversal.PREORDER, node -> {
            if (enclosingType(node) != type) {
                return;
            }
            switch (node) {
                case MethodCallExpr call -> invocations.add(
                        new Invocation(call.getScope().flatMap(JavaParserSourceParser::nameChain), call.getNameAsString(), line(call)));
                case MethodReferenceExpr reference -> invocations.add(
                        new Invocation(nameChain(reference.getScope()), reference.getIdentifier(), line(reference)));
                default -> {
                }
            }
        });
        return invocations;
    }

    private static Node enclosingType(Node node) {
        Optional<Node> parent = node.getParentNode();
        while (parent.isPresent() && !(parent.get() instanceof TypeDeclaration<?>)) {
            parent = parent.get().getParentNode();
        }
        return parent.orElse(null);
    }

    private static Optional<String> nameChain(Expression expression) {
        return switch (expression) {
            case NameExpr name -> Optional.of(name.getNameAsString());
            case FieldAccessExpr access -> nameChain(access.getScope()).map(scope -> scope + "." + access.getNameAsString());
            case TypeExpr typeExpression -> Optional.of(typeExpression.getType().asString());
            case ThisExpr _ -> Optional.of("this");
            default -> Optional.empty();
        };
    }

    private static List<Annotation> annotations(List<AnnotationExpr> annotations) {
        return annotations.stream()
                .map(annotation -> new Annotation(annotation.getNameAsString(), line(annotation)))
                .toList();
    }

    private static int line(Node node) {
        return node.getBegin().orElseThrow().line;
    }
}
