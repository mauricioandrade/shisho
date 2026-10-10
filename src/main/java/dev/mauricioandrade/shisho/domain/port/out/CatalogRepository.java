package dev.mauricioandrade.shisho.domain.port.out;

import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.AvoidedDependency;
import dev.mauricioandrade.shisho.domain.model.Concept;
import dev.mauricioandrade.shisho.domain.model.JavaFeature;
import dev.mauricioandrade.shisho.domain.model.KataGuide;
import dev.mauricioandrade.shisho.domain.model.Scaffold;

import java.util.List;

public interface CatalogRepository {

    /**
     * @throws dev.mauricioandrade.shisho.domain.exception.CatalogException if an entry is
     *         unreadable or breaks the schema documented in {@code catalog/README.md}
     */
    List<ArchitectureProfile> architectures();

    /**
     * @throws dev.mauricioandrade.shisho.domain.exception.CatalogException if an entry is
     *         unreadable or lacks its "Antes" or "Depois" code block
     */
    List<KataGuide> kataGuides();

    /** @throws dev.mauricioandrade.shisho.domain.exception.CatalogException if an entry is invalid */
    List<JavaFeature> javaFeatures();

    /** @throws dev.mauricioandrade.shisho.domain.exception.CatalogException if an entry is invalid */
    List<AvoidedDependency> avoidedDependencies();

    /** @throws dev.mauricioandrade.shisho.domain.exception.CatalogException if an entry is invalid */
    List<Scaffold> scaffolds();

    /** @throws dev.mauricioandrade.shisho.domain.exception.CatalogException if an entry is invalid */
    List<Concept> concepts();
}
