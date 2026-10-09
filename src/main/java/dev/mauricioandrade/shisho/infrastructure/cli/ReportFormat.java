package dev.mauricioandrade.shisho.infrastructure.cli;

/** {@code --format}: who reads the report. */
public enum ReportFormat {
    /** People at a terminal. */
    TERMINAL,
    /** GitHub code scanning and other SARIF 2.1.0 tools. */
    SARIF
}
