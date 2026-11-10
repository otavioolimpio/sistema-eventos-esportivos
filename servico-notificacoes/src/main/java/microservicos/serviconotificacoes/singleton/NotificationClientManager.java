package microservicos.serviconotificacoes.singleton;

import microservicos.serviconotificacoes.adapter.INotificador;
import microservicos.serviconotificacoes.dominio.Canal;
import microservicos.serviconotificacoes.dominio.Mensagem;
import microservicos.serviconotificacoes.dominio.ResultadoEnvio;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PADRÃO SINGLETON — gerenciador único dos clientes de notificação.
 *
 * Existe uma única instância em todo o serviço (RN01 do UC07), obtida por
 * {@link #getInstance()}. Ela guarda os adaptadores registrados (um por canal)
 * e é o ponto único por onde passam todos os envios.
 *
 * Implementação clássica do GoF, deixada explícita de propósito:
 *  - construtor privado (ninguém faz "new NotificationClientManager()");
 *  - instância guardada em um atributo estático;
 *  - criação preguiçosa e segura entre threads (double-checked locking).
 *
 * Observação: o Spring também cria seus componentes como singletons. Aqui o
 * padrão é implementado manualmente para que fique visível no código; a classe
 * {@link microservicos.serviconotificacoes.config.NotificacaoConfig} apenas registra
 * os adaptadores nesta instância única quando a aplicação sobe.
 */
public final class NotificationClientManager {

    private static volatile NotificationClientManager instancia;

    private final Map<Canal, INotificador> adaptadores = new ConcurrentHashMap<>();

    private NotificationClientManager() {
    }

    public static NotificationClientManager getInstance() {
        if (instancia == null) {
            synchronized (NotificationClientManager.class) {
                if (instancia == null) {
                    instancia = new NotificationClientManager();
                }
            }
        }
        return instancia;
    }

    /** Registra (ou substitui) o adaptador responsável por um canal. */
    public void registrar(INotificador adaptador) {
        adaptadores.put(adaptador.canal(), adaptador);
    }

    /** Canais que possuem adaptador registrado. */
    public Set<Canal> canaisDisponiveis() {
        return Collections.unmodifiableSet(adaptadores.keySet());
    }

    /**
     * Seleciona o adaptador do canal (passo 3 do UC07) e envia a mensagem (passo 4).
     * Nunca lança exceção: qualquer falha é devolvida como resultado, para que o
     * caso de uso base decida como tratá-la sem desfazer sua operação (RN03 do UC07).
     */
    public ResultadoEnvio enviar(Canal canal, Mensagem mensagem) {
        INotificador adaptador = adaptadores.get(canal);
        if (adaptador == null) {
            return ResultadoEnvio.falha("Nenhum adaptador registrado para o canal " + canal + ".");
        }
        try {
            adaptador.enviar(mensagem);
            return ResultadoEnvio.sucesso();
        } catch (RuntimeException e) {
            return ResultadoEnvio.falha(e.getMessage());
        }
    }
}
