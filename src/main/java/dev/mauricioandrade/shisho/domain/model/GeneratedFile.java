package dev.mauricioandrade.shisho.domain.model;

import java.nio.file.Path;

/** A file to create, relative to the project root, and why it exists. */
public record GeneratedFile(Path file, String content, String explanation) {
}
