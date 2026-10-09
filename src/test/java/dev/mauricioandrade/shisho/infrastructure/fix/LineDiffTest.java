package dev.mauricioandrade.shisho.infrastructure.fix;

import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Expected outputs were checked against GNU {@code diff -u}. */
class LineDiffTest {

    @Test
    void writesOneHunkWithThreeLinesOfContext() {
        String before = numbered(1, 10);
        String after = before.replace("linha 5\n", "linha cinco\n");

        assertThat(LineDiff.unified("src/A.java", before, after)).isEqualTo("""
                --- a/src/A.java
                +++ b/src/A.java
                @@ -2,7 +2,7 @@
                 linha 2
                 linha 3
                 linha 4
                -linha 5
                +linha cinco
                 linha 6
                 linha 7
                 linha 8
                """);
    }

    @Test
    void separatesDistantChangesAndJoinsCloseOnes() {
        String before = numbered(1, 30);
        String after = before.replace("linha 2\n", "linha dois\n")
                .replace("linha 8\n", "linha 8\nnova\n")
                .replace("linha 25\n", "");

        assertThat(LineDiff.unified("A.java", before, after)).isEqualTo("""
                --- a/A.java
                +++ b/A.java
                @@ -1,11 +1,12 @@
                 linha 1
                -linha 2
                +linha dois
                 linha 3
                 linha 4
                 linha 5
                 linha 6
                 linha 7
                 linha 8
                +nova
                 linha 9
                 linha 10
                 linha 11
                @@ -22,7 +23,6 @@
                 linha 22
                 linha 23
                 linha 24
                -linha 25
                 linha 26
                 linha 27
                 linha 28
                """);
    }

    @Test
    void isEmptyWithoutChanges() {
        assertThat(LineDiff.unified("A.java", "a\nb\n", "a\nb\n")).isEmpty();
    }

    private static String numbered(int from, int to) {
        return IntStream.rangeClosed(from, to).mapToObj(i -> "linha " + i + "\n").collect(Collectors.joining());
    }
}
