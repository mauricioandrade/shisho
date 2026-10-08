package dev.mauricioandrade.shisho.domain.port.out;

import dev.mauricioandrade.shisho.domain.model.Analysis;

public interface ReportWriter {

    void write(Analysis analysis);
}
