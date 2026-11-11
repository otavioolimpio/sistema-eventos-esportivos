package microservicos.servicousuariosequipes.dto;

import microservicos.servicousuariosequipes.domain.enums.Modalidade;
import microservicos.servicousuariosequipes.domain.model.Equipe;

import java.util.List;

public record EquipeResponse(Long id, String nome, Modalidade modalidade,
                             UsuarioResponse chefeEquipe, List<UsuarioResponse> jogadores) {

    public static EquipeResponse de(Equipe equipe) {
        return new EquipeResponse(
                equipe.getId(),
                equipe.getNome(),
                equipe.getModalidade(),
                equipe.getChefeEquipe() == null ? null : UsuarioResponse.de(equipe.getChefeEquipe()),
                equipe.getJogadores().stream().map(UsuarioResponse::de).toList());
    }
}
