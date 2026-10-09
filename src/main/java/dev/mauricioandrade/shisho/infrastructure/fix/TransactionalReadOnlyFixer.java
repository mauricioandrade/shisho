package dev.mauricioandrade.shisho.infrastructure.fix;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.Lesson;

import java.util.Optional;

/**
 * Sets {@code readOnly = true} on the read method's {@code @Transactional}, or, when the method
 * inherits a read-write transaction from its class, gives the method its own
 * {@code @Transactional(readOnly = true)}.
 */
final class TransactionalReadOnlyFixer implements Fixer {

    private static final String TRANSACTIONAL = "Transactional";
    private static final String READ_ONLY = "readOnly";

    @Override
    public String kataId() {
        return "transactional-read-only";
    }

    @Override
    public Optional<Runnable> prepare(CompilationUnit unit, Lesson lesson, Analysis analysis) {
        String name = (String) lesson.arguments().getFirst();
        for (MethodDeclaration method : unit.findAll(MethodDeclaration.class)) {
            if (!method.getNameAsString().equals(name)) {
                continue;
            }
            Optional<AnnotationExpr> own = method.getAnnotations().stream()
                    .filter(annotation -> annotation.getName().getIdentifier().equals(TRANSACTIONAL))
                    .findFirst();
            if (own.isPresent() && Trees.startsAt(own.get(), lesson.line())) {
                AnnotationExpr annotation = own.get();
                return Optional.of(() -> setReadOnly(annotation));
            }
            if (own.isEmpty() && Trees.startsAt(method, lesson.line())) {
                Optional<Name> inherited = method.findAncestor(ClassOrInterfaceDeclaration.class)
                        .flatMap(type -> type.getAnnotationByName(TRANSACTIONAL))
                        .map(annotation -> annotation.getName().clone());
                return inherited.map(annotationName -> () -> method.addAnnotation(
                        new NormalAnnotationExpr(annotationName, new NodeList<>(readOnlyPair()))));
            }
        }
        return Optional.empty();
    }

    /** Pairs are edited in place: the printer keeps the author's order and spacing around them. */
    private static void setReadOnly(AnnotationExpr annotation) {
        switch (annotation) {
            case MarkerAnnotationExpr marker ->
                    marker.replace(new NormalAnnotationExpr(marker.getName().clone(), new NodeList<>(readOnlyPair())));
            case SingleMemberAnnotationExpr single -> single.replace(new NormalAnnotationExpr(single.getName().clone(),
                    new NodeList<>(new MemberValuePair("value", single.getMemberValue().clone()), readOnlyPair())));
            case NormalAnnotationExpr normal -> normal.getPairs().stream()
                    .filter(pair -> pair.getNameAsString().equals(READ_ONLY))
                    .findFirst()
                    .ifPresentOrElse(pair -> pair.setValue(new BooleanLiteralExpr(true)), () -> normal.getPairs().add(readOnlyPair()));
            default -> {
            }
        }
    }

    private static MemberValuePair readOnlyPair() {
        return new MemberValuePair(READ_ONLY, new BooleanLiteralExpr(true));
    }
}
