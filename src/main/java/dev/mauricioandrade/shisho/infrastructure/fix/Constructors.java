package dev.mauricioandrade.shisho.infrastructure.fix;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Writing constructors the way a person would: after the fields, one assignment per parameter. */
final class Constructors {

    /**
     * The layout-preserving printer does not indent a member inserted between two others, so new
     * constructors carry this comment and {@link #indentMarked} aligns them after printing.
     */
    private static final String MARKER = "shisho-new-constructor-" + UUID.randomUUID();

    private Constructors() {
    }

    /** A new, empty constructor placed right after the last field. */
    static ConstructorDeclaration addAfterFields(ClassOrInterfaceDeclaration type, boolean isPublic) {
        ConstructorDeclaration constructor = new ConstructorDeclaration(type.getNameAsString());
        constructor.setComment(new LineComment(MARKER));
        // the convenience constructor above makes it public; set the visibility explicitly
        constructor.setModifiers(isPublic ? new NodeList<>(Modifier.publicModifier()) : new NodeList<>());
        List<BodyDeclaration<?>> members = type.getMembers();
        int afterFields = 0;
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i) instanceof FieldDeclaration) {
                afterFields = i + 1;
            }
        }
        type.getMembers().add(afterFields, constructor);
        return constructor;
    }

    /** Adds {@code type name} as a parameter and {@code this.name = name;} at the end of the body. */
    static void inject(ConstructorDeclaration constructor, Type type, String name) {
        constructor.addParameter(type.clone(), name);
        constructor.getBody().addStatement(new ExpressionStmt(new AssignExpr(
                new FieldAccessExpr(new ThisExpr(), name), new NameExpr(name), AssignExpr.Operator.ASSIGN)));
    }

    /**
     * Removes each marker line and indents the constructor under it like the field above it: from
     * the marker to the first line that closes at the marker's own indentation.
     */
    static String indentMarked(String printed) {
        List<String> lines = new ArrayList<>(printed.lines().toList());
        for (int marker = indexOfMarker(lines); marker >= 0; marker = indexOfMarker(lines)) {
            String markerIndent = indentation(lines.get(marker));
            String memberIndent = markerIndent;
            for (int previous = marker - 1; previous >= 0; previous--) {
                if (!lines.get(previous).isBlank()) {
                    memberIndent = indentation(lines.get(previous));
                    break;
                }
            }
            lines.remove(marker);
            String shift = memberIndent.length() > markerIndent.length() ? memberIndent.substring(markerIndent.length()) : "";
            for (int line = marker; line < lines.size(); line++) {
                String text = lines.get(line);
                if (!text.isBlank()) {
                    lines.set(line, shift + text);
                }
                if (text.equals(markerIndent + "}")) {
                    break;
                }
            }
        }
        return String.join("\n", lines) + (printed.endsWith("\n") ? "\n" : "");
    }

    private static int indexOfMarker(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).strip().endsWith(MARKER)) {
                return i;
            }
        }
        return -1;
    }

    private static String indentation(String line) {
        return line.substring(0, line.length() - line.stripLeading().length());
    }

    static boolean delegates(ConstructorDeclaration constructor) {
        return constructor.getBody().getStatements().stream()
                .anyMatch(statement -> statement instanceof ExplicitConstructorInvocationStmt call && call.isThis());
    }
}
