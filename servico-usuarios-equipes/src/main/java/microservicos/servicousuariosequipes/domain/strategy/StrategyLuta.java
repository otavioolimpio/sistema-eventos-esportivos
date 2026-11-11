package microservicos.servicousuariosequipes.domain.strategy;

import microservicos.servicousuariosequipes.domain.enums.Modalidade;
import microservicos.servicousuariosequipes.domain.model.Equipe;
import org.springframework.stereotype.Component;

@Component
public class StrategyLuta implements ValidacaoElencoStrategy {

    @Override
    public Modalidade getModalidade() {
        return Modalidade.LUTA;
    }

    @Override
    public void validarElenco(Equipe equipe) {
        int total = equipe.getJogadores() != null ? equipe.getJogadores().size() : 0;
        if (total != 1) {
            throw new IllegalArgumentException("Modalidades de Luta devem possuir exatamente 1 jogador.");
        }
    }
}