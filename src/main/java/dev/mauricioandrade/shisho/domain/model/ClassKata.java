package dev.mauricioandrade.shisho.domain.model;

import java.util.List;

/** A kata applied to each class of the analyzed project on its own. */
public non-sealed interface ClassKata extends Kata {

    List<Lesson> evaluate(ClassUnit unit);

    default Lesson lesson(ClassUnit unit, int line, Object... arguments) {
        return lesson(unit.file(), line, arguments);
    }
}
