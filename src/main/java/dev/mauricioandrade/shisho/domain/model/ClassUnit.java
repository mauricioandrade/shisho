package dev.mauricioandrade.shisho.domain.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * One type declaration (top-level or nested) and what katas ask about it. Members belong to the
 * type that declares them directly: a nested type's fields are in its own {@code ClassUnit}.
 * Lines are 1-based; {@code file} is relative to the analyzed project root.
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
        List<Invocation> invocations) {

    public ClassUnit {
        annotations = List.copyOf(annotations);
        imports = List.copyOf(imports);
        fields = List.copyOf(fields);
        constructors = List.copyOf(constructors);
        methods = List.copyOf(methods);
        invocations = List.copyOf(invocations);
    }

    public enum Kind { CLASS, INTERFACE, ENUM, RECORD, ANNOTATION }

    /** {@code name} is written as in the source: {@code Autowired} or a fully qualified name. */
    public record Annotation(String name, int line) {

        public String simpleName() {
            return name.substring(name.lastIndexOf('.') + 1);
        }
    }

    public record Import(String name, boolean isStatic, boolean isOnDemand, boolean isModule) {
    }

    /** One per declared variable: {@code int a, b;} yields two fields. */
    public record Field(
            String name,
            String type,
            List<Annotation> annotations,
            boolean isFinal,
            boolean isStatic,
            boolean hasInitializer,
            int line) {

        public Field {
            annotations = List.copyOf(annotations);
        }
    }

    public record Parameter(String name, String type, List<Annotation> annotations) {

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

    public record Method(String name, List<Annotation> annotations, List<Parameter> parameters, int line) {

        public Method {
            annotations = List.copyOf(annotations);
            parameters = List.copyOf(parameters);
        }
    }

    /**
     * A method call or method reference. {@code target} is the receiver when it is a plain name
     * chain ({@code System.out}, {@code e}, {@code this}) and empty otherwise.
     */
    public record Invocation(Optional<String> target, String name, int line) {
    }
}
