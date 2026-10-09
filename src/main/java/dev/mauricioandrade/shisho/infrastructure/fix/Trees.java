package dev.mauricioandrade.shisho.infrastructure.fix;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;

import java.util.Optional;

/** Lookups shared by the fixers. */
final class Trees {

    private Trees() {
    }

    static boolean startsAt(Node node, int line) {
        return node.getBegin().map(position -> position.line == line).orElse(false);
    }

    static boolean covers(Node node, int line) {
        return node.getRange().map(range -> range.begin.line <= line && line <= range.end.line).orElse(false);
    }

    /** The single-variable field called {@code name} whose declaration covers {@code line}. */
    static Optional<FieldDeclaration> field(CompilationUnit unit, String name, int line) {
        return unit.findAll(FieldDeclaration.class).stream()
                .filter(field -> field.getVariables().size() == 1)
                .filter(field -> field.getVariable(0).getNameAsString().equals(name))
                .filter(field -> covers(field, line))
                .findFirst();
    }

    static Optional<ClassOrInterfaceDeclaration> enclosingClass(Node node) {
        return node.findAncestor(ClassOrInterfaceDeclaration.class).filter(type -> !type.isInterface());
    }

    /** {@code name = ...} or {@code this.name = ...} anywhere under {@code scope}. */
    static boolean assigns(Node scope, String name) {
        return scope.findAll(AssignExpr.class).stream().anyMatch(assign -> isField(assign.getTarget(), name));
    }

    /** Assignments to the field outside constructors, where {@code final} would not compile. */
    static boolean assignedOutsideConstructors(ClassOrInterfaceDeclaration type, String name) {
        return type.findAll(AssignExpr.class).stream()
                .filter(assign -> isField(assign.getTarget(), name))
                .anyMatch(assign -> assign.findAncestor(ConstructorDeclaration.class).isEmpty());
    }

    private static boolean isField(Expression target, String name) {
        return switch (target) {
            case NameExpr simple -> simple.getNameAsString().equals(name);
            case FieldAccessExpr access -> access.getScope() instanceof ThisExpr && access.getNameAsString().equals(name);
            default -> false;
        };
    }
}
