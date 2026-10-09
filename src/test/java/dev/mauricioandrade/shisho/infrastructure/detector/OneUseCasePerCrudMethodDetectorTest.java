package dev.mauricioandrade.shisho.infrastructure.detector;

import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.detector.DetectorTestSupport.project;
import static org.assertj.core.api.Assertions.assertThat;

class OneUseCasePerCrudMethodDetectorTest {

    private final OneUseCasePerCrudMethodDetector detector = new OneUseCasePerCrudMethodDetector();

    @Test
    void findsThreeCrudUseCasesForTheSameEntity() {
        assertThat(detector.detect(project("""
                class CreateUserUseCase {}
                class FindUserByIdUseCase {}
                class ListAllUsersUseCase {}
                class DeleteUserUseCase {}
                class CriarPedidoUseCase {}
                """))).hasValueSatisfying(evidence -> {
            assertThat(evidence.detectorId()).isEqualTo("um_use_case_por_metodo_crud");
            assertThat(evidence.arguments()).containsExactly("User", 4);
            assertThat(evidence.messageKey()).isEqualTo("detector.um_use_case_por_metodo_crud");
        });
    }

    @Test
    void acceptsUseCasesWithBusinessNames() {
        assertThat(detector.detect(project("""
                class CriarPedidoUseCase {}
                class BuscarPedidoUseCase {}
                class FaturarPedidoUseCase {}
                class CancelarPedidoUseCase {}
                """))).isEmpty();
    }
}
