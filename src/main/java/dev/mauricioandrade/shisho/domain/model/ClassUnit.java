package dev.mauricioandrade.shisho.domain.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * One type declaration (top-level or nested) and what katas ask about it. Members belong to the
 * type that declares them directly: a nested type's fields are in its own {@code ClassUnit}.
 * Lines are 1-based; {@code file} is relative to the analyzed project root.
 *
 * @param supertypes the types it extends or implements, by simple name and without type
 *                   arguments: {@code JpaRepository<Pedido, Long>} is {@code JpaRepository}
 * @param modifiers  its modifiers as written, such as {@code public}, {@code sealed} or {@code abstract}
 * @param catches    its {@code catch} blocks, those in lambdas and anonymous classes included
 */
public record ClassUnit(
        Path file,
        String packageName,
        String name,
        Kind kind,
        int line,
        List<Annotation> annotations,
        List<Import> imports,
        List<Field> fields,
        List<Constructor> constructors,
        List<Method> methods,
        List<Invocation> invocations,
        List<String> supertypes,
        Set<String> modifiers,
        List<CatchClause> catches) {

    public ClassUnit {
        annotations = List.copyOf(annotations);
        imports = List.copyOf(imports);
        fields = List.copyOf(fields);
        constructors = List.copyOf(constructors);
        methods = List.copyOf(methods);
        invocations = List.copyOf(invocations);
        supertypes = List.copyOf(supertypes);
        modifiers = Set.copyOf(modifiers);
        catches = List.copyOf(catches);
    }

    /** Without supertypes, modifiers or catch blocks: most tests need none of them. */
    public ClassUnit(Path file, String packageName, String name, Kind kind, int line, List<Annotation> annotations,
                     List<Import> imports, List<Field> fields, List<Constructor> constructors, List<Method> methods,
                     List<Invocation> invocations) {
        this(file, packageName, name, kind, line, annotations, imports, fields, constructors, methods, invocations,
                List.of(), Set.of(), List.of());
    }

    public boolean isAnnotatedWith(String simpleName) {
        return annotations.stream().anyMatch(annotation -> annotation.simpleName().equals(simpleName));
    }

    /** Instance fields, or the components of a record: what the type holds. */
    public List<String> stateNames() {
        if (kind == Kind.RECORD) {
            return constructors.getFirst().parameters().stream().map(Parameter::name).toList();
        }
        return fields.stream().filter(field -> !field.isStatic()).map(Field::name).toList();
    }

    public enum Kind { CLASS, INTERFACE, ENUM, RECORD, ANNOTATION }

    /**
     * {@code name} is written as in the source: {@code Autowired} or a fully qualified name.
     * {@code attributes} holds string literals unquoted and any other value as written;
     * a single-member annotation such as {@code @Value("${x}")} has the attribute {@code value}.
     */
    public record Annotation(String name, int line, Map<String, String> attributes) {

        public Annotation {
            attributes = Map.copyOf(attributes);
        }

        public Annotation(String name, int line) {
            this(name, line, Map.of());
        }

        public Optional<String> attribute(String key) {
            return Optional.ofNullable(attributes.get(key));
        }

        public String simpleName() {
            return name.substring(name.lastIndexOf('.') + 1);
        }
    }

    public record Import(String name, boolean isStatic, boolean isOnDemand, boolean isModule, int line) {
    }

    /** One per declared variable: {@code int a, b;} yields two fields. */
    public record Field(
            String name,
            String type,
            List<Annotation> annotations,
            boolean isFinal,
            boolean isStatic,
            boolean isPrivate,
            boolean hasInitializer,
            int line) {

        public Field {
            annotations = List.copyOf(annotations);
        }
    }

    /** {@code line} is where the parameter begins: its first annotation, if it has any. */
    public record Parameter(String name, String type, List<Annotation> annotations, int line) {

        public Parameter {
            annotations = List.copyOf(annotations);
        }
    }

    /** A record's components are represented as its canonical constructor. */
    public record Constructor(List<Annotation> annotations, List<Parameter> parameters, int line) {

        public Constructor {
            annotations = List.copyOf(annotations);
            parameters = List.copyOf(parameters);
        }
    }

    /**
     * {@code returnType} is written as in the source, {@code void} included; {@code modifiers} too,
     * so an interface method without any has none.
     */
    public record Method(String name, String returnType, List<Annotation> annotations, List<Parameter> parameters, int line,
                         Set<String> modifiers) {

        public Method {
            annotations = List.copyOf(annotations);
            parameters = List.copyOf(parameters);
            modifiers = Set.copyOf(modifiers);
        }

        /** Without modifiers: most tests do not need them. */
        public Method(String name, String returnType, List<Annotation> annotations, List<Parameter> parameters, int line) {
            this(name, returnType, annotations, parameters, line, Set.of());
        }
    }

    /**
     * A {@code catch} block: the types it catches (several in a multi-catch), the name of the
     * caught variable ({@code _} when unnamed), whether its body has no statement at all and
     * whether it holds a comment.
     */
    public record CatchClause(List<String> types, String variable, boolean empty, boolean commented, int line) {

        public CatchClause {
            types = List.copyOf(types);
        }
    }

    /**
     * A method call or method reference. {@code target} is the receiver when it is a plain name
     * chain ({@code System.out}, {@code e}, {@code this}) and empty otherwise. {@code caller} is
     * the method of this type the call is written in, lambdas and anonymous classes inside it
     * included; empty in a constructor or an initializer. {@code arguments} is the number of
     * arguments, or {@link #UNKNOWN_ARGUMENTS} for a method reference.
     */
    public record Invocation(Optional<String> target, String name, int line, Optional<String> caller, int arguments) {

        public static final int UNKNOWN_ARGUMENTS = -1;
    }
}
