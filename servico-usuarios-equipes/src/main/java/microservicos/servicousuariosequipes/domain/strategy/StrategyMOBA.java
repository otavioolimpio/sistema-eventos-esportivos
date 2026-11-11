package microservicos.servicousuariosequipes.domain.strategy;

import microservicos.servicousuariosequipes.domain.enums.Modalidade;
import microservicos.servicousuariosequipes.domain.model.Equipe;
import org.springframework.stereotype.Component;

@Component
public class StrategyMOBA implements ValidacaoElencoStrategy {

    @Override
    public Modalidade getModalidade() {
        return Modalidade.MOBA;
    }

    @Override
    public void validarElenco(Equipe equipe) {
        int total = equipe.getJogadores() != null ? equipe.getJogadores().size() : 0;
        if (total < 5 || total > 6) {
            throw new IllegalArgumentException("Equipes de MOBA devem possuir entre 5 e 6 jogadores.");
        }
    }
}