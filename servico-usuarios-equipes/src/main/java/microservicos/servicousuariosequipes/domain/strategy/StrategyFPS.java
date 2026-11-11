package microservicos.servicousuariosequipes.domain.strategy;

import microservicos.servicousuariosequipes.domain.enums.Modalidade;
import microservicos.servicousuariosequipes.domain.model.Equipe;
import org.springframework.stereotype.Component;

@Component
public class StrategyFPS implements ValidacaoElencoStrategy {

    @Override
    public Modalidade getModalidade() {
        return Modalidade.FPS;
    }

    @Override
    public void validarElenco(Equipe equipe) {
        int total = equipe.getJogadores() != null ? equipe.getJogadores().size() : 0;
        if (total != 5) {
            throw new IllegalArgumentException("Equipes de FPS devem possuir exatamente 5 jogadores.");
        }
    }
}