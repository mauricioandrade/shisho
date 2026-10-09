package dev.mauricioandrade.shisho.infrastructure.fix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A line diff: the common prefix and suffix are skipped, and the changed middle is aligned by its
 * longest common subsequence. Corrections touch a few lines, so the middle stays small.
 */
public final class LineDiff {

    private static final int CONTEXT = 3;

    private LineDiff() {
    }

    sealed interface Edit {

        String line();

        record Keep(String line) implements Edit {
        }

        record Delete(String line) implements Edit {
        }

        record Insert(String line) implements Edit {
        }
    }

    static List<Edit> edits(List<String> before, List<String> after) {
        int prefix = 0;
        while (prefix < before.size() && prefix < after.size() && before.get(prefix).equals(after.get(prefix))) {
            prefix++;
        }
        int suffix = 0;
        while (suffix < before.size() - prefix && suffix < after.size() - prefix
                && before.get(before.size() - 1 - suffix).equals(after.get(after.size() - 1 - suffix))) {
            suffix++;
        }
        List<Edit> edits = new ArrayList<>();
        before.subList(0, prefix).forEach(line -> edits.add(new Edit.Keep(line)));
        edits.addAll(middle(before.subList(prefix, before.size() - suffix), after.subList(prefix, after.size() - suffix)));
        before.subList(before.size() - suffix, before.size()).forEach(line -> edits.add(new Edit.Keep(line)));
        return edits;
    }

    private static List<Edit> middle(List<String> before, List<String> after) {
        int[][] common = new int[before.size() + 1][after.size() + 1];
        for (int i = before.size() - 1; i >= 0; i--) {
            for (int j = after.size() - 1; j >= 0; j--) {
                common[i][j] = before.get(i).equals(after.get(j))
                        ? common[i + 1][j + 1] + 1
                        : Math.max(common[i + 1][j], common[i][j + 1]);
            }
        }
        List<Edit> edits = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < before.size() || j < after.size()) {
            if (i < before.size() && j < after.size() && before.get(i).equals(after.get(j))) {
                edits.add(new Edit.Keep(before.get(i++)));
                j++;
            } else if (i < before.size() && (j == after.size() || common[i + 1][j] >= common[i][j + 1])) {
                edits.add(new Edit.Delete(before.get(i++)));
            } else {
                edits.add(new Edit.Insert(after.get(j++)));
            }
        }
        return edits;
    }

    static List<String> lines(String text) {
        List<String> lines = new ArrayList<>(Arrays.asList(text.split("\r?\n", -1)));
        if (lines.getLast().isEmpty()) {
            lines.removeLast();
        }
        return lines;
    }

    /** A unified diff ({@code diff -u}) with {@value #CONTEXT} lines of context, or empty if nothing changed. */
    public static String unified(String path, String before, String after) {
        List<Edit> edits = edits(lines(before), lines(after));
        StringBuilder diff = new StringBuilder();
        int index = 0;
        int beforeLine = 1;
        int afterLine = 1;
        while (index < edits.size()) {
            int change = index;
            while (change < edits.size() && edits.get(change) instanceof Edit.Keep) {
                change++;
            }
            if (change == edits.size()) {
                break;
            }
            int start = Math.max(index, change - CONTEXT);
            beforeLine += start - index;
            afterLine += start - index;
            int end = hunkEnd(edits, change);
            int beforeCount = 0;
            int afterCount = 0;
            StringBuilder hunk = new StringBuilder();
            for (Edit edit : edits.subList(start, end)) {
                switch (edit) {
                    case Edit.Keep keep -> {
                        hunk.append(' ').append(keep.line()).append('\n');
                        beforeCount++;
                        afterCount++;
                    }
                    case Edit.Delete delete -> {
                        hunk.append('-').append(delete.line()).append('\n');
                        beforeCount++;
                    }
                    case Edit.Insert insert -> {
                        hunk.append('+').append(insert.line()).append('\n');
                        afterCount++;
                    }
                }
            }
            if (diff.isEmpty()) {
                diff.append("--- a/").append(path).append('\n').append("+++ b/").append(path).append('\n');
            }
            diff.append("@@ -").append(range(beforeLine, beforeCount)).append(" +").append(range(afterLine, afterCount)).append(" @@\n")
                    .append(hunk);
            beforeLine += beforeCount;
            afterLine += afterCount;
            index = end;
        }
        return diff.toString();
    }

    /** A hunk ends {@value #CONTEXT} unchanged lines after its last change, unless another change comes sooner. */
    private static int hunkEnd(List<Edit> edits, int change) {
        int end = change;
        int unchanged = 0;
        while (end < edits.size() && unchanged <= 2 * CONTEXT) {
            unchanged = edits.get(end) instanceof Edit.Keep ? unchanged + 1 : 0;
            end++;
        }
        return end - Math.max(0, unchanged - CONTEXT);
    }

    private static String range(int start, int count) {
        return count == 0 ? (start - 1) + ",0" : start + "," + count;
    }
}
