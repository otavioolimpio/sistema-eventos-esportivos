package microservicos.servicoinscricoes.facade;

import feign.FeignException;
import microservicos.servicoinscricoes.client.EventoClient;
import microservicos.servicoinscricoes.client.NotificacaoClient;
import microservicos.servicoinscricoes.client.UsuarioEquipeClient;
import microservicos.servicoinscricoes.domain.enums.StatusInscricao;
import microservicos.servicoinscricoes.domain.model.Inscricao;
import microservicos.servicoinscricoes.dto.AvaliarInscricaoRequestDTO;
import microservicos.servicoinscricoes.dto.EquipeDTO;
import microservicos.servicoinscricoes.dto.EventoDTO;
import microservicos.servicoinscricoes.dto.InscricaoResponseDTO;
import microservicos.servicoinscricoes.dto.NotificacaoResponseDTO;
import microservicos.servicoinscricoes.dto.SolicitarInscricaoRequestDTO;
import microservicos.servicoinscricoes.dto.UsuarioDTO;
import microservicos.servicoinscricoes.service.InscricaoService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PADRAO FACADE + ORQUESTRADOR DE SERVICOS (UC05 e UC06).
 *
 * <p><b>Facade:</b> o {@code InscricaoController} conhece apenas esta classe. Ela esconde
 * do controller a conversa com tres microsservicos (Usuarios, Eventos e Notificacoes)
 * e com o {@link InscricaoService} (regras e persistencia), oferecendo uma interface
 * simples: solicitar, avaliar e listar.</p>
 *
 * <p><b>Orquestrador:</b> cada caso de uso e uma sequencia de chamadas OpenFeign
 * sincronas, resolvidas pelo nome do servico no Eureka. Cada passo so comeca quando o
 * anterior termina e usa a resposta dele (ex.: o chefe notificado e o da inscricao lida
 * no passo 1). Os passos estao numerados nos metodos abaixo.</p>
 *
 * <pre>
 *   InscricaoController
 *          |
 *    InscricaoFacade ----> servico-usuarios-equipes (UsuarioEquipeClient)
 *          |         ----> servico-eventos          (EventoClient)
 *          |         ----> servico-notificacoes     (NotificacaoClient, UC07)
 *          v
 *    InscricaoService ---> InscricaoRepository (SQLite)
 * </pre>
 */
@Component
public class InscricaoFacade {

    private final InscricaoService inscricaoService;
    private final UsuarioEquipeClient usuarioEquipeClient;
    private final EventoClient eventoClient;
    private final NotificacaoClient notificacaoClient;

    public InscricaoFacade(
            InscricaoService inscricaoService,
            UsuarioEquipeClient usuarioEquipeClient,
            EventoClient eventoClient,
            NotificacaoClient notificacaoClient) {
        this.inscricaoService = inscricaoService;
        this.usuarioEquipeClient = usuarioEquipeClient;
        this.eventoClient = eventoClient;
        this.notificacaoClient = notificacaoClient;
    }

    /**
     * UC05 - Solicitar Inscricao em Evento (Chefe de Equipe).
     */
    public Inscricao solicitarInscricao(SolicitarInscricaoRequestDTO request) {
        // Passo 1 - Servico de Usuarios: busca a equipe e confere se quem pede e o chefe dela
        EquipeDTO equipe = buscarEquipe(request.getEquipeId());
        inscricaoService.validarChefe(equipe, request.getChefeId());

        // Passo 2 - Servico de Eventos: evento ABERTO, com vaga e modalidade compativel com a equipe
        EventoDTO evento = buscarEvento(request.getEventoId());
        inscricaoService.validarEventoParaSolicitacao(equipe, evento);

        // Passo 3 - Persistencia local: grava a inscricao como PENDENTE
        return inscricaoService.registrarPendente(request);
    }

    /**
     * UC06 - Aprovar/Rejeitar Inscricao (Organizador do Evento) + «include» UC07 - Enviar Notificacao.
     */
    public InscricaoResponseDTO avaliarInscricao(Long inscricaoId, AvaliarInscricaoRequestDTO request) {
        // Passo 1 - Persistencia local: le a inscricao (precisa estar PENDENTE)
        Inscricao inscricao = inscricaoService.buscarPendente(inscricaoId);

        // Passo 2 - Servico de Usuarios: o avaliador e ORGANIZADOR?
        UsuarioDTO organizador = buscarUsuario(request.getOrganizadorId());
        inscricaoService.validarAvaliador(organizador);

        // Passo 3 - Servico de Eventos: busca o evento da inscricao
        EventoDTO evento = buscarEvento(inscricao.getEventoId());

        // Passo 4 - Servico de Usuarios: busca a equipe da inscricao
        EquipeDTO equipe = buscarEquipe(inscricao.getEquipeId());

        // Passo 5 - Servico de Eventos: se aprovada, ocupa uma vaga do evento
        boolean aprovada = Boolean.TRUE.equals(request.getAprovada());
        if (aprovada) {
            inscricaoService.validarVagaParaAprovacao(evento);
            eventoClient.ocuparVaga(evento.getId());
        }

        // Passo 6 - Persistencia local: grava APROVADA ou REJEITADA
        inscricao = inscricaoService.registrarAvaliacao(
                inscricao, aprovada, request.getOrganizadorId(), request.getJustificativa());

        // Passos 7 e 8 - Usuarios (busca o chefe) + Notificacoes (e-mail, UC07)
        return notificarChefe(inscricao, equipe, evento);
    }

    /** Consulta as inscricoes (todas ou de um evento). */
    public List<Inscricao> listarInscricoes(Long eventoId) {
        return eventoId != null
                ? inscricaoService.listarPorEvento(eventoId)
                : inscricaoService.listarTodas();
    }

    /**
     * Passos 7 e 8 do UC06. Uma falha aqui NAO desfaz a avaliacao (RN03 do UC07):
     * a resposta informa notificacaoEnviada=false e um aviso.
     */
    private InscricaoResponseDTO notificarChefe(Inscricao inscricao, EquipeDTO equipe, EventoDTO evento) {
        try {
            // Passo 7 - Servico de Usuarios: busca o chefe da equipe (destinatario do e-mail)
            UsuarioDTO chefe = buscarUsuario(inscricao.getChefeEquipeId());

            // Passo 8 - Servico de Notificacoes: envia o e-mail; so conta como enviado com status ENVIADA
            NotificacaoResponseDTO resposta = notificacaoClient.enviarNotificacao(
                    inscricaoService.montarNotificacaoResultado(inscricao, chefe, equipe, evento));

            if (resposta != null && resposta.foiEnviada()) {
                return new InscricaoResponseDTO(inscricao, true, null);
            }
            String aviso = "Inscricao " + nomeStatus(inscricao) + " com sucesso, mas o e-mail nao foi enviado"
                    + (resposta != null && resposta.erro() != null ? ": " + resposta.erro() : ".");
            return new InscricaoResponseDTO(inscricao, false, aviso);
        } catch (Exception e) {
            return new InscricaoResponseDTO(inscricao, false,
                    "Inscricao atualizada com sucesso, mas houve falha ao enviar notificacao por e-mail.");
        }
    }

    private static String nomeStatus(Inscricao inscricao) {
        return inscricao.getStatus() == StatusInscricao.APROVADA ? "aprovada" : "rejeitada";
    }

    // ---------------------------------------------------------------
    // Chamadas aos outros microsservicos: 404 vira IllegalArgumentException (HTTP 400 no controller)
    // ---------------------------------------------------------------

    private EquipeDTO buscarEquipe(Long equipeId) {
        try {
            return usuarioEquipeClient.buscarEquipePorId(equipeId);
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Equipe nao encontrada: " + equipeId);
        }
    }

    private UsuarioDTO buscarUsuario(Long usuarioId) {
        try {
            return usuarioEquipeClient.buscarUsuarioPorId(usuarioId);
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Usuario nao encontrado: " + usuarioId);
        }
    }

    private EventoDTO buscarEvento(Long eventoId) {
        try {
            return eventoClient.buscarEventoPorId(eventoId);
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Evento nao encontrado: " + eventoId);
        }
    }
}
