package dev.mauricioandrade.shisho.infrastructure.detector;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.Detector;
import dev.mauricioandrade.shisho.domain.model.Evidence;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@value #MINIMUM_USE_CASES} or more use case classes doing CRUD verbs on the same entity:
 * {@code CreateUserUseCase}, {@code FindUserByIdUseCase}, {@code DeleteUserUseCase}...
 */
final class OneUseCasePerCrudMethodDetector implements Detector {

    static final int MINIMUM_USE_CASES = 3;
    private static final Pattern CRUD_USE_CASE = Pattern.compile(
            "(Create|Get|Find|List|Update|Delete|Remove|Criar|Cadastrar|Buscar|Listar|Obter|Atualizar|Remover|Excluir|Deletar)"
                    + "(All|Todos|Todas)?(\\p{Lu}\\w*?)(ById|PorId)?(UseCase|Usecase)");

    @Override
    public String id() {
        return "um_use_case_por_metodo_crud";
    }

    @Override
    public Optional<Evidence> detect(ProjectModel project) {
        Map<String, Set<String>> verbsByEntity = new TreeMap<>();
        for (ClassUnit unit : project.classes()) {
            Matcher matcher = CRUD_USE_CASE.matcher(unit.name());
            if (matcher.matches()) {
                verbsByEntity.computeIfAbsent(singular(matcher.group(3)), _ -> new TreeSet<>()).add(matcher.group(1));
            }
        }
        return verbsByEntity.entrySet().stream()
                .filter(entity -> entity.getValue().size() >= MINIMUM_USE_CASES)
                .max(Comparator.comparingInt(entity -> entity.getValue().size()))
                .flatMap(entity -> evidence(entity.getKey(), entity.getValue().size()));
    }

    private static String singular(String entity) {
        return entity.endsWith("s") && entity.length() > 1 ? entity.substring(0, entity.length() - 1) : entity;
    }
}
