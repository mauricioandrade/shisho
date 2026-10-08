package dev.mauricioandrade.shisho.domain.model;

import java.util.List;

/** A good-practice rule applied to each class of the analyzed project. */
public interface Kata {

    String id();

    Severity severity();

    /** The kata only runs on projects at this Java version or above. */
    JavaVersion minJavaVersion();

    List<Lesson> evaluate(ClassUnit unit);

    /** A lesson of this kata, explained by the message key {@code kata.<id>.lesson}. */
    default Lesson lesson(ClassUnit unit, int line, Object... arguments) {
        return new Lesson(id(), severity(), unit.file(), line, "kata." + id() + ".lesson", List.of(arguments));
    }
}
