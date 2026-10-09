package dev.mauricioandrade.shisho.domain.model;

import java.util.Set;

/**
 * What the author declares about the project, as the keys the catalog uses in
 * {@code faz_sentido_quando}: the situations that make an architecture's cost pay off.
 */
public record ProjectContext(Set<String> keys) {

    /** Every key the catalog may use, as documented in {@code catalog/README.md}. */
    public static final Set<String> KNOWN_KEYS = Set.of(
            "dominio_com_regras_complexas", "multiplas_integracoes_externas", "troca_provavel_de_infra",
            "vida_util_longa", "time_grande", "multiplos_times", "carga_leitura_escrita_assimetrica",
            "escala_independente_por_dominio", "processamento_assincrono", "auditoria_de_eventos",
            "crud_simples", "mvp", "muitas_features");

    public ProjectContext {
        keys = Set.copyOf(keys);
    }
}
