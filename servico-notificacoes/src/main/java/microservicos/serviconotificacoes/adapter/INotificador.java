package microservicos.serviconotificacoes.adapter;

import microservicos.serviconotificacoes.dominio.Canal;
import microservicos.serviconotificacoes.dominio.Mensagem;

/**
 * PADRÃO ADAPTER — interface-alvo (Target).
 *
 * É a única interface que o restante do sistema conhece para enviar mensagens.
 * Cada provedor externo (servidor de e-mail, Discord...) tem uma API diferente;
 * os adaptadores traduzem a chamada {@link #enviar(Mensagem)} para a API de cada um.
 *
 * Incluir um novo canal (ex.: SMS) significa apenas criar um novo adaptador,
 * sem alterar quem envia (RN02 do UC07).
 */
public interface INotificador {

    /** Canal atendido por este adaptador. */
    Canal canal();

    /**
     * Envia a mensagem pelo provedor externo.
     *
     * @throws FalhaEnvioException se o provedor não aceitar a mensagem
     */
    void enviar(Mensagem mensagem);
}
