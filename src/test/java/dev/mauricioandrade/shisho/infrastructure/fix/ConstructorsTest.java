package dev.mauricioandrade.shisho.infrastructure.fix;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstructorsTest {

    @Test
    void indentsANewConstructorLikeTheFieldAboveIt() {
        CompilationUnit unit = StaticJavaParser.parse("""
                class A {
                \tprivate final B b;

                \tvoid run() {
                \t}
                }
                """);
        LexicalPreservingPrinter.setup(unit);
        ClassOrInterfaceDeclaration type = unit.getClassByName("A").orElseThrow();

        Constructors.inject(Constructors.addAfterFields(type, false), type.getFields().getFirst().getElementType(), "b");

        assertThat(Constructors.indentMarked(LexicalPreservingPrinter.print(unit))).contains("""
                \tprivate final B b;

                \tA(B b) {
                \t    this.b = b;
                \t}
                """).doesNotContain("shisho-new-constructor");
    }
}
