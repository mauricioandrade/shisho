package dev.mauricioandrade.shisho.infrastructure.cli;

import dev.mauricioandrade.shisho.domain.port.out.ReportWriter;

import java.io.PrintStream;

/** The report destination is only known once the options are parsed: stdout or {@code --output}. */
@FunctionalInterface
public interface ReportWriterFactory {

    ReportWriter create(ReportFormat format, PrintStream destination);
}
