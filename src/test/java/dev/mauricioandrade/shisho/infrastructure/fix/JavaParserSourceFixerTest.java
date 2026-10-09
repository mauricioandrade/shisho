package dev.mauricioandrade.shisho.infrastructure.fix;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaParserSourceFixerTest {

    @Test
    void keepsWindowsLineEndingsAndCleansOnlyAddedLines() {
        String original = "class A {\r\n    int a;   \r\n}\r\n";
        String printed = "class A {\n    int a;   \n    \n    A() {}\n}\n";

        assertThat(JavaParserSourceFixer.tidy(original, printed))
                .isEqualTo("class A {\r\n    int a;   \r\n\r\n    A() {}\r\n}\r\n");
    }

    @Test
    void keepsAMissingFinalNewline() {
        assertThat(JavaParserSourceFixer.tidy("class A {}", "final class A {}")).isEqualTo("final class A {}");
    }
}
