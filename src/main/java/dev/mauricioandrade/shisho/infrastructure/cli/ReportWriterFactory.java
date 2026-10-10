package dev.mauricioandrade.shisho.infrastructure.cli;

import dev.mauricioandrade.shisho.domain.port.out.ReportWriter;

import java.io.PrintStream;

/** The report destination and options are only known once the command line is parsed. */
@FunctionalInterface
public interface ReportWriterFactory {

    /**
     * @param sensei  show how to fix each kata found (the HTML and SARIF reports always do)
     * @param shoshin explain the concepts found in the code (terminal report only)
     */
    ReportWriter create(ReportFormat format, PrintStream destination, boolean sensei, boolean shoshin);
}
