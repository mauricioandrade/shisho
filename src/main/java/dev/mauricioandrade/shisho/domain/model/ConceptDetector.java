package dev.mauricioandrade.shisho.domain.model;

import java.util.List;

/**
 * Finds a concept that no single type shows, such as an interface with several implementations.
 * The catalog names it in a {@code detector} signal.
 */
public interface ConceptDetector {

    String id();

    List<ConceptFinding.Occurrence> find(ProjectModel project);
}
