package dev.mauricioandrade.shisho.domain.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The lessons a project already had when its baseline was recorded: the analysis shows and fails
 * only on the ones that came after. A lesson is recognized by kata, file, message and arguments,
 * never by line, so code that moves inside its file stays known. See docs/adr/0023-baseline.md.
 */
public record Baseline(List<Entry> entries) {

    private static final Comparator<Entry> ORDER = Comparator.comparing(Entry::file)
            .thenComparing(Entry::kata)
            .thenComparing(Entry::message)
            .thenComparing(entry -> String.join("\u0000", entry.arguments()));

    public Baseline {
        entries = entries.stream().sorted(ORDER).toList();
    }

    public static Baseline of(List<Lesson> lessons) {
        return new Baseline(lessons.stream().map(Entry::of).toList());
    }

    /**
     * Splits {@code lessons}, in their order, into the new ones and the known ones. An entry
     * recorded twice covers two lessons, so a third identical lesson is new; when there are more
     * lessons than entries, the later ones are the new ones.
     */
    public Comparison compare(List<Lesson> lessons) {
        Map<Entry, Integer> remaining = new HashMap<>();
        entries.forEach(entry -> remaining.merge(entry, 1, Integer::sum));
        List<Lesson> fresh = new ArrayList<>();
        List<Lesson> known = new ArrayList<>();
        for (Lesson lesson : lessons) {
            Entry entry = Entry.of(lesson);
            if (remaining.getOrDefault(entry, 0) > 0) {
                remaining.merge(entry, -1, Integer::sum);
                known.add(lesson);
            } else {
                fresh.add(lesson);
            }
        }
        int fixed = remaining.values().stream().mapToInt(Integer::intValue).sum();
        return new Comparison(fresh, known, fixed);
    }

    /** A recorded lesson; {@code file} uses {@code /} on every system, so the baseline can be committed. */
    public record Entry(String kata, String file, String message, List<String> arguments) {

        public Entry {
            arguments = List.copyOf(arguments);
        }

        static Entry of(Lesson lesson) {
            return new Entry(lesson.kataId(), lesson.file().toString().replace('\\', '/'), lesson.messageKey(),
                    lesson.arguments().stream().map(String::valueOf).toList());
        }
    }

    /**
     * @param fresh the lessons the baseline does not have
     * @param known the lessons it has
     * @param fixed how many of its entries no lesson matched: lessons fixed since it was recorded
     */
    public record Comparison(List<Lesson> fresh, List<Lesson> known, int fixed) {

        public Comparison {
            fresh = List.copyOf(fresh);
            known = List.copyOf(known);
        }
    }
}
