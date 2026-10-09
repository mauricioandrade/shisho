package dev.mauricioandrade.shisho.domain.port.out;

import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.KataExample;

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
    List<KataExample> kataExamples();
}
