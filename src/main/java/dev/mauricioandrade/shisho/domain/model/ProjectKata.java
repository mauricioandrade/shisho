package dev.mauricioandrade.shisho.domain.model;

import java.util.List;

/** A kata applied once to the whole project, for rules no single class can answer. */
public non-sealed interface ProjectKata extends Kata {

    List<Lesson> evaluate(ProjectModel project);
}
