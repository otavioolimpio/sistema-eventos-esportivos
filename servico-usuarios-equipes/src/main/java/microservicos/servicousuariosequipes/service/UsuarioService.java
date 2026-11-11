package microservicos.servicousuariosequipes.service;

import microservicos.servicousuariosequipes.client.NotificacaoGateway;
import microservicos.servicousuariosequipes.domain.model.Usuario;
import microservicos.servicousuariosequipes.repository.UsuarioRepository;
import microservicos.servicousuariosequipes.service.excecoes.ConflitoException;
import microservicos.servicousuariosequipes.service.excecoes.RecursoNaoEncontradoException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    static final String ASSUNTO_CREDENCIAIS = "Suas credenciais de acesso";

    // Sem caracteres ambiguos (0/O, 1/l/I) para facilitar a digitacao da senha provisoria
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int TAMANHO_SENHA = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final NotificacaoGateway notificacoes;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          NotificacaoGateway notificacoes,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.notificacoes = notificacoes;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * UC02 - Cadastrar Usuario. Gera uma senha provisoria, salva o usuario e
     * envia as credenciais por e-mail («include» do UC07).
     *
     * Sem @Transactional de proposito: o usuario ja esta gravado quando a
     * notificacao sai, e uma falha no envio nao desfaz o cadastro (FA03).
     */
    public UsuarioCriado criarUsuario(Usuario usuario) {
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new ConflitoException("Ja existe um usuario com o e-mail " + usuario.getEmail());
        }
        if (usuarioRepository.findByCpf(usuario.getCpf()).isPresent()) {
            throw new ConflitoException("Ja existe um usuario com o CPF " + usuario.getCpf());
        }

        String senhaProvisoria = gerarSenhaProvisoria();
        usuario.setId(null);
        usuario.setSenhaHash(passwordEncoder.encode(senhaProvisoria));
        Usuario salvo = usuarioRepository.save(usuario);

        boolean enviadas = notificacoes.enviarEmail(
                salvo.getEmail(),
                ASSUNTO_CREDENCIAIS,
                "Ola, " + salvo.getNome() + "! Sua conta no Sistema de Eventos Esportivos foi criada.\n\n"
                        + "Login: " + salvo.getEmail() + "\n"
                        + "Senha provisoria: " + senhaProvisoria + "\n\n"
                        + "Troque a senha no primeiro acesso.");

        return new UsuarioCriado(salvo, enviadas);
    }

    /** Alterar Senha: confere a senha atual e grava a nova. */
    @Transactional
    public void alterarSenha(Long id, String senhaAtual, String novaSenha) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + id));
        if (usuario.getSenhaHash() == null || !passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    private static String gerarSenhaProvisoria() {
        StringBuilder senha = new StringBuilder(TAMANHO_SENHA);
        for (int i = 0; i < TAMANHO_SENHA; i++) {
            senha.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return senha.toString();
    }

    /** Resultado do cadastro: o usuario salvo e se as credenciais chegaram a ser enviadas. */
    public record UsuarioCriado(Usuario usuario, boolean credenciaisEnviadas) {
    }
}
