package dev.mauricioandrade.shisho.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The analysis of a project and the corrections that can be applied to it, ordered by file. */
public record FixPlan(Analysis analysis, List<FileFix> fixes) {

    public FixPlan {
        fixes = List.copyOf(fixes);
    }

    /** Lessons no correction covers: they need the author, guided by the sensei's examples. */
    public List<Lesson> unfixed() {
        Set<Lesson> fixed = new HashSet<>();
        fixes.forEach(fix -> fixed.addAll(fix.lessons()));
        return analysis.allLessons().stream().filter(lesson -> !fixed.contains(lesson)).toList();
    }
}
