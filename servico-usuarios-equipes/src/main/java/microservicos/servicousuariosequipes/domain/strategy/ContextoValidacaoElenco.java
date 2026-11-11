package microservicos.servicousuariosequipes.domain.strategy;

import microservicos.servicousuariosequipes.domain.enums.Modalidade;
import microservicos.servicousuariosequipes.domain.model.Equipe;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ContextoValidacaoElenco {

    private final Map<Modalidade, ValidacaoElencoStrategy> estrategias;

    public ContextoValidacaoElenco(List<ValidacaoElencoStrategy> listaEstrategias) {
        this.estrategias = listaEstrategias.stream()
                .collect(Collectors.toMap(ValidacaoElencoStrategy::getModalidade, Function.identity()));
    }

    public void executarValidacao(Equipe equipe) {
        ValidacaoElencoStrategy estrategia = estrategias.get(equipe.getModalidade());
        if (estrategia == null) {
            throw new IllegalArgumentException("Estrategia nao encontrada para a modalidade: " + equipe.getModalidade());
        }
        estrategia.validarElenco(equipe);
    }
}