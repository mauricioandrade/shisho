package dev.mauricioandrade.shisho.domain.port.out;

import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.FileFix;

import java.util.List;

public interface SourceFixer {

    /**
     * Computes, without writing anything, the corrections for the lessons that have a safe
     * mechanical fix. Lessons that cannot be fixed safely are left out.
     */
    List<FileFix> fix(Analysis analysis);
}
