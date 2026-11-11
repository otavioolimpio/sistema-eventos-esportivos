package microservicos.servicousuariosequipes.domain.strategy;

import microservicos.servicousuariosequipes.domain.enums.Modalidade;
import microservicos.servicousuariosequipes.domain.model.Equipe;

public interface ValidacaoElencoStrategy {
    Modalidade getModalidade();
    void validarElenco(Equipe equipe);
}