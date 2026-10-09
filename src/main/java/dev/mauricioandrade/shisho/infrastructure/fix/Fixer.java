package dev.mauricioandrade.shisho.infrastructure.fix;

import com.github.javaparser.ast.CompilationUnit;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.Lesson;

import java.util.Optional;

/** The correction of one kata. */
interface Fixer {

    String kataId();

    /**
     * Finds, in the tree as parsed, what the lesson points at and checks the correction is safe.
     * The returned change runs later, after every lesson of the file is prepared, so positions
     * are never read from a tree another correction already changed.
     *
     * @return empty when this occurrence cannot be fixed safely
     */
    Optional<Runnable> prepare(CompilationUnit unit, Lesson lesson, Analysis analysis);
}
