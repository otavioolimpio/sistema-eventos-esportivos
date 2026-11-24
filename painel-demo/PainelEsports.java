/*
 * Painel de demonstração — Sistema de Eventos de E-Sports (Grupo 2, APSOO/UFRPE)
 *
 * Interface gráfica simples (Java Swing, sem bibliotecas externas) que usa os
 * microsserviços SOMENTE pelo API Gateway. O painel não conhece as portas dos
 * serviços de negócio (elas são aleatórias): quem descobre onde cada serviço
 * está é o Gateway, consultando o Eureka.
 *
 * Nenhuma porta fica no código. O endereço do Gateway vem, nesta ordem:
 *   (a) -Dgateway=URL na linha de comando;
 *   (b) a variável de ambiente GATEWAY_URL;
 *   (c) GATEWAY_PORT lido do arquivo .env (pasta atual ou pasta pai),
 *       montando http://localhost:<porta>.
 * O mesmo vale para o Eureka (-Deureka, EUREKA_URL, EUREKA_PORT) e para o
 * Mailpit (-Dmailpit, MAILPIT_URL, MAILPIT_WEB_PORT).
 *
 * Como rodar (precisa só do JDK 21, sem Maven), a partir da raiz do repositório:
 *     java painel-demo/PainelEsports.java
 * Antes: docker compose up -d na raiz do repositório e esperar os serviços
 * aparecerem no painel do Eureka.
 */

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PainelEsports {

    // Endereços preenchidos por carregarConfiguracao(), antes de abrir a janela
    static String GATEWAY;
    static String EUREKA;
    static String MAILPIT;
    /** De onde veio cada endereço (mostrado no topo do painel). */
    static String ORIGEM_CONFIG;

    static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .proxy(HttpClient.Builder.NO_PROXY)
            .build();
    /** As chamadas HTTP rodam fora da thread da interface, uma de cada vez. */
    static final ExecutorService FILA = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "http");
        t.setDaemon(true);
        return t;
    });

    record Resposta(int codigo, String corpo) {}

    private JTextArea log;
    private JLabel statusEureka;
    private final String sufixo = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
    private int contadorExemplo = 0;

    // Tabelas (atualizadas depois de cada operação)
    private final DefaultTableModel tUsuarios = modelo("ID", "Nome", "E-mail", "Perfil");
    private final DefaultTableModel tEquipes = modelo("ID", "Nome", "Modalidade", "Chefe (id)", "Chefe", "Nº jogadores", "IDs dos jogadores");
    private final DefaultTableModel tModalidades = modelo("ID", "Nome", "Mín.", "Máx.");
    private final DefaultTableModel tEventos = modelo("ID", "Nome", "Modalidade", "Vagas disp.", "Vagas totais", "Status");
    private final DefaultTableModel tInscricoes = modelo("ID", "Equipe", "Evento", "Chefe", "Status", "Justificativa");
    private final DefaultTableModel tNotificacoes = modelo("Quando", "Canal", "Destinatário", "Assunto", "Status", "Erro");

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) UIManager.setLookAndFeel(info.getClassName());
            }
        } catch (Exception ignorada) { /* fica o visual padrão */ }
        Font base = new Font("SansSerif", Font.PLAIN, 15);   // fonte maior, para o projetor
        UIManager.put("defaultFont", base);
        for (Object chave : List.copyOf(UIManager.getDefaults().keySet())) {
            if (chave.toString().endsWith(".font")) UIManager.put(chave, base);
        }
        String erro = carregarConfiguracao();
        if (erro != null) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null, erro,
                        "Painel E-Sports — configuração não encontrada", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            });
            return;
        }
        SwingUtilities.invokeLater(() -> new PainelEsports().abrir());
    }

    // ─────────────────────── Configuração (sem portas no código) ───────────────────────

    /**
     * Descobre os endereços do Gateway, do Eureka e do Mailpit.
     * Devolve null se deu certo, ou a mensagem de erro para mostrar ao usuário.
     */
    static String carregarConfiguracao() {
        Path arquivoEnv = acharArquivoEnv();
        Map<String, String> env = lerArquivoEnv(arquivoEnv);
        List<String> origens = new ArrayList<>();
        List<String> faltando = new ArrayList<>();

        GATEWAY = resolverEndereco("Gateway", "gateway", "GATEWAY_URL", "GATEWAY_PORT", env, origens, faltando);
        EUREKA = resolverEndereco("Eureka", "eureka", "EUREKA_URL", "EUREKA_PORT", env, origens, faltando);
        MAILPIT = resolverEndereco("Mailpit", "mailpit", "MAILPIT_URL", "MAILPIT_WEB_PORT", env, origens, faltando);
        // EUREKA_URL pode vir no formato usado pelos serviços (.../eureka/): o painel quer só a raiz
        if (EUREKA != null) EUREKA = EUREKA.replaceFirst("/eureka/?$", "");

        if (!faltando.isEmpty()) {
            return "Não encontrei o endereço de: " + String.join(", ", faltando) + ".\n\n"
                    + "Rode o painel a partir da pasta do repositório, onde fica o arquivo .env:\n"
                    + "    cd sistema-eventos-esportivos\n"
                    + "    java painel-demo/PainelEsports.java\n\n"
                    + "Ou informe o endereço com -Dgateway=URL (ou a variável GATEWAY_URL),\n"
                    + "e o mesmo para -Deureka / EUREKA_URL e -Dmailpit / MAILPIT_URL.\n\n"
                    + "Pasta atual: " + Path.of("").toAbsolutePath();
        }
        String sobreEnv = arquivoEnv != null ? "   [.env: " + arquivoEnv.toAbsolutePath().normalize() + "]" : "";
        ORIGEM_CONFIG = "Configuração: " + String.join(" · ", origens) + sobreEnv;
        return null;
    }

    /** Ordem: (a) -Dpropriedade; (b) variável de ambiente XXX_URL; (c) XXX_PORT do .env. */
    static String resolverEndereco(String nome, String propriedade, String variavelUrl, String chavePorta,
                                   Map<String, String> env, List<String> origens, List<String> faltando) {
        String valor = System.getProperty(propriedade);
        if (temValor(valor)) {
            origens.add(nome + " de -D" + propriedade);
            return semBarraFinal(valor);
        }
        valor = System.getenv(variavelUrl);
        if (temValor(valor)) {
            origens.add(nome + " de " + variavelUrl);
            return semBarraFinal(valor);
        }
        valor = env.get(chavePorta);
        if (temValor(valor)) {
            origens.add(nome + " de " + chavePorta + " (.env)");
            return "http://localhost:" + valor.trim();
        }
        faltando.add(nome + " (-D" + propriedade + ", " + variavelUrl + " ou " + chavePorta + " no .env)");
        return null;
    }

    /** Procura o .env na pasta atual e, se não houver, na pasta pai. */
    static Path acharArquivoEnv() {
        Path atual = Path.of("").toAbsolutePath();
        for (Path pasta : new Path[]{atual, atual.getParent()}) {
            if (pasta != null && Files.isRegularFile(pasta.resolve(".env"))) return pasta.resolve(".env");
        }
        return null;
    }

    /** Lê linhas CHAVE=VALOR do .env, ignorando comentários (#) e linhas vazias. */
    static Map<String, String> lerArquivoEnv(Path arquivo) {
        Map<String, String> valores = new HashMap<>();
        if (arquivo == null) return valores;
        try {
            for (String linha : Files.readAllLines(arquivo)) {
                linha = linha.trim();
                int igual = linha.indexOf('=');
                if (linha.isEmpty() || linha.startsWith("#") || igual < 1) continue;
                String valor = linha.substring(igual + 1).trim().replaceAll("^[\"']|[\"']$", "");
                valores.put(linha.substring(0, igual).trim(), valor);
            }
        } catch (IOException e) {
            System.err.println("Não consegui ler " + arquivo + ": " + e.getMessage());
        }
        return valores;
    }

    static boolean temValor(String s) { return s != null && !s.isBlank(); }

    static String semBarraFinal(String url) { return url.trim().replaceFirst("/+$", ""); }

    // ───────────────────────────── Janela ─────────────────────────────

    void abrir() {
        JFrame janela = new JFrame("Sistema de Eventos de E-Sports — Grupo 2");
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Usuários (UC02)", abaUsuarios());
        abas.addTab("Equipes (UC03)", abaEquipes());
        abas.addTab("Modalidades e Eventos (UC01, UC04)", abaEventos());
        abas.addTab("Inscrições (UC05, UC06)", abaInscricoes());
        abas.addTab("Notificações (UC07)", abaNotificacoes());
        abas.addChangeListener(e -> atualizarTudo());

        log = new JTextArea(10, 80);
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        JScrollPane rolagemLog = new JScrollPane(log);
        rolagemLog.setBorder(new TitledBorder("Requisições HTTP (todas pelo API Gateway)"));

        JSplitPane divisao = new JSplitPane(JSplitPane.VERTICAL_SPLIT, abas, rolagemLog);
        divisao.setResizeWeight(0.65);

        janela.add(barraTopo(), BorderLayout.NORTH);
        janela.add(divisao, BorderLayout.CENTER);
        janela.setSize(1280, 860);
        janela.setLocationRelativeTo(null);
        janela.setVisible(true);

        verificarEureka();
        atualizarTudo();
    }

    JPanel barraTopo() {
        JPanel topo = new JPanel(new BorderLayout(10, 4));
        topo.setBorder(new EmptyBorder(8, 10, 8, 10));
        JLabel gateway = new JLabel("API Gateway: " + GATEWAY + "   (o painel não conhece as portas dos serviços)");
        gateway.setFont(gateway.getFont().deriveFont(Font.BOLD));
        JLabel origem = new JLabel(ORIGEM_CONFIG);
        origem.setFont(origem.getFont().deriveFont(Font.ITALIC, origem.getFont().getSize2D() - 2f));
        statusEureka = new JLabel("Eureka: verificando...");
        JPanel textos = new JPanel(new GridLayout(3, 1));
        textos.add(gateway);
        textos.add(origem);
        textos.add(statusEureka);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botoes.add(botao("Verificar Eureka", this::verificarEureka));
        botoes.add(botao("Abrir Eureka", () -> abrirNoNavegador(EUREKA)));
        botoes.add(botao("Abrir Mailpit", () -> abrirNoNavegador(MAILPIT)));
        botoes.add(botao("Limpar log", () -> log.setText("")));
        topo.add(textos, BorderLayout.CENTER);
        topo.add(botoes, BorderLayout.EAST);
        return topo;
    }

    // ───────────────────────────── Abas ─────────────────────────────

    JComponent abaUsuarios() {
        JTextField nome = new JTextField(18), email = new JTextField(22), cpf = new JTextField(14);
        JComboBox<String> perfil = new JComboBox<>(new String[]{"JOGADOR", "CHEFE_EQUIPE", "ORGANIZADOR"});

        JButton exemplo = botao("Preencher exemplo", () -> {
            contadorExemplo++;
            String p = (String) perfil.getSelectedItem();
            String base = switch (p) {
                case "ORGANIZADOR" -> "Organizador";
                case "CHEFE_EQUIPE" -> "Chefe";
                default -> "Jogador";
            };
            nome.setText(base + " " + contadorExemplo);
            email.setText(base.toLowerCase() + contadorExemplo + "." + sufixo + "@exemplo.com");
            cpf.setText("cpf-" + sufixo + "-" + contadorExemplo);
        });
        JButton cadastrar = botao("Cadastrar usuário", () -> {
            String json = "{" + campo("nome", nome.getText()) + "," + campo("email", email.getText()) + ","
                    + campo("cpf", cpf.getText()) + "," + campo("tipoPerfil", (String) perfil.getSelectedItem()) + "}";
            enviar("POST", "/api/usuarios", json, () -> carregarUsuarios());
        });

        JPanel form = formulario("Cadastrar usuário — gera senha provisória e envia por e-mail (UC02 + UC07)",
                new String[]{"Perfil", "Nome", "E-mail", "CPF"},
                new JComponent[]{perfil, nome, email, cpf}, exemplo, cadastrar);
        return comTabela(form, tUsuarios, this::carregarUsuarios);
    }

    JComponent abaEquipes() {
        JTextField nome = new JTextField(16), chefe = new JTextField(6), jogadores = new JTextField(18);
        JComboBox<String> modalidade = new JComboBox<>(new String[]{"FPS", "MOBA", "LUTA"});
        JButton criar = botao("Criar equipe", () -> {
            String json = "{" + campo("nome", nome.getText()) + "," + campo("modalidade", (String) modalidade.getSelectedItem())
                    + ",\"chefeId\":" + numero(chefe, "Chefe (id)")
                    + ",\"jogadoresIds\":" + listaDeNumeros(jogadores) + "}";
            enviar("POST", "/api/equipes", json, this::carregarEquipes);
        });
        JPanel form = formulario("Cadastrar equipe — elenco validado pela Strategy da modalidade "
                        + "(FPS: 5 jogadores; MOBA: 5 ou 6; LUTA: 1)",
                new String[]{"Nome", "Modalidade", "Chefe (id)", "Jogadores (ids, ex.: 2,3,4,5,6)"},
                new JComponent[]{nome, modalidade, chefe, jogadores}, criar);
        return comTabela(form, tEquipes, this::carregarEquipes);
    }

    JComponent abaEventos() {
        // UC01 — Modalidade
        JTextField mNome = new JTextField("FPS", 10), mMin = new JTextField("5", 4), mMax = new JTextField("5", 4),
                mOrg = new JTextField(5);
        JButton cadModalidade = botao("Cadastrar modalidade", () -> {
            String json = "{" + campo("nome", mNome.getText()) + ",\"minJogadores\":" + numero(mMin, "Mín.")
                    + ",\"maxJogadores\":" + numero(mMax, "Máx.") + ",\"organizadorId\":" + numero(mOrg, "Organizador (id)") + "}";
            enviar("POST", "/api/modalidades", json, this::carregarEventos);
        });
        JPanel formModalidade = formulario("UC01 — Cadastrar modalidade",
                new String[]{"Nome", "Mín. jogadores", "Máx. jogadores", "Organizador (id)"},
                new JComponent[]{mNome, mMin, mMax, mOrg}, cadModalidade);

        // UC04 — Evento
        JTextField eNome = new JTextField("Copa FPS", 14), eModalidade = new JTextField("FPS", 8),
                eOrg = new JTextField(5), eVagas = new JTextField("2", 4);
        JButton criarEvento = botao("Criar evento", () -> {
            String json = "{" + campo("nome", eNome.getText()) + "," + campo("modalidade", eModalidade.getText())
                    + ",\"organizadorId\":" + numero(eOrg, "Organizador (id)")
                    + ",\"vagasTotais\":" + numero(eVagas, "Vagas") + "}";
            enviar("POST", "/api/eventos", json, this::carregarEventos);
        });
        JPanel formEvento = formulario("UC04 — Criar evento",
                new String[]{"Nome", "Modalidade", "Organizador (id)", "Vagas"},
                new JComponent[]{eNome, eModalidade, eOrg, eVagas}, criarEvento);

        JPanel formularios = new JPanel(new GridLayout(1, 2, 10, 0));
        formularios.add(formModalidade);
        formularios.add(formEvento);

        JPanel tabelas = new JPanel(new GridLayout(1, 2, 10, 0));
        tabelas.add(rolagem(tabela(tModalidades), "Modalidades"));
        tabelas.add(rolagem(tabela(tEventos), "Eventos"));

        JPanel painel = new JPanel(new BorderLayout(0, 6));
        painel.setBorder(new EmptyBorder(6, 6, 6, 6));
        painel.add(formularios, BorderLayout.NORTH);
        painel.add(tabelas, BorderLayout.CENTER);
        painel.add(linhaBotoes(botao("Atualizar listas", this::carregarEventos)), BorderLayout.SOUTH);
        return painel;
    }

    JComponent abaInscricoes() {
        JTextField equipe = new JTextField(5), evento = new JTextField(5), chefe = new JTextField(5);
        JButton solicitar = botao("Solicitar inscrição", () -> {
            String json = "{\"equipeId\":" + numero(equipe, "Equipe (id)") + ",\"eventoId\":" + numero(evento, "Evento (id)")
                    + ",\"chefeId\":" + numero(chefe, "Chefe (id)") + "}";
            enviar("POST", "/api/inscricoes", json, this::carregarInscricoes);
        });
        JPanel formSolicitar = formulario("UC05 — Chefe solicita inscrição (InscricaoFacade)",
                new String[]{"Equipe (id)", "Evento (id)", "Chefe (id)"},
                new JComponent[]{equipe, evento, chefe}, solicitar);

        JTextField inscricao = new JTextField(5), organizador = new JTextField(5), justificativa = new JTextField(18);
        JButton aprovar = botao("Aprovar", () -> avaliar(inscricao, organizador, justificativa, true));
        JButton rejeitar = botao("Rejeitar", () -> avaliar(inscricao, organizador, justificativa, false));
        JPanel formAvaliar = formulario("UC06 — Organizador avalia (ocupa vaga + notifica o chefe)",
                new String[]{"Inscrição (id)", "Organizador (id)", "Justificativa"},
                new JComponent[]{inscricao, organizador, justificativa}, aprovar, rejeitar);

        JPanel formularios = new JPanel(new GridLayout(1, 2, 10, 0));
        formularios.add(formSolicitar);
        formularios.add(formAvaliar);

        JTable tab = tabela(tInscricoes);
        tab.getSelectionModel().addListSelectionListener(e -> {   // clicar numa linha preenche o id
            int linha = tab.getSelectedRow();
            if (linha >= 0) inscricao.setText(String.valueOf(tab.getValueAt(linha, 0)));
        });

        JPanel painel = new JPanel(new BorderLayout(0, 6));
        painel.setBorder(new EmptyBorder(6, 6, 6, 6));
        painel.add(formularios, BorderLayout.NORTH);
        painel.add(rolagem(tab, "Inscrições (clique numa linha para avaliá-la)"), BorderLayout.CENTER);
        painel.add(linhaBotoes(botao("Atualizar lista", this::carregarInscricoes)), BorderLayout.SOUTH);
        return painel;
    }

    void avaliar(JTextField inscricao, JTextField organizador, JTextField justificativa, boolean aprovada) {
        String id = numero(inscricao, "Inscrição (id)");
        String json = "{\"organizadorId\":" + numero(organizador, "Organizador (id)") + ",\"aprovada\":" + aprovada
                + (justificativa.getText().isBlank() ? "" : "," + campo("justificativa", justificativa.getText())) + "}";
        enviar("PATCH", "/api/inscricoes/" + id + "/avaliar", json, () -> {
            carregarInscricoes();
            carregarEventos();
        });
    }

    JComponent abaNotificacoes() {
        JLabel texto = new JLabel("<html>Cada e-mail enviado pelo sistema passa pelo Serviço de Notificações "
                + "(Singleton <b>NotificationClientManager</b> + Adapter <b>EmailAdapter</b>). "
                + "Falha no envio fica registrada como <b>FALHA</b> e não desfaz a operação (RN03).</html>");
        texto.setBorder(new EmptyBorder(4, 4, 8, 4));
        JPanel painel = new JPanel(new BorderLayout(0, 6));
        painel.setBorder(new EmptyBorder(6, 6, 6, 6));
        painel.add(texto, BorderLayout.NORTH);
        painel.add(rolagem(tabela(tNotificacoes), "Notificações registradas (mais recentes primeiro)"), BorderLayout.CENTER);
        painel.add(linhaBotoes(botao("Atualizar lista", this::carregarNotificacoes),
                botao("Abrir caixa de e-mail (Mailpit)", () -> abrirNoNavegador(MAILPIT))), BorderLayout.SOUTH);
        return painel;
    }

    // ───────────────────────────── Carregar tabelas ─────────────────────────────

    void atualizarTudo() {
        carregarUsuarios();
        carregarEquipes();
        carregarEventos();
        carregarInscricoes();
        carregarNotificacoes();
    }

    void carregarUsuarios() {
        carregar("/api/usuarios", tUsuarios, "id", "nome", "email", "tipoPerfil");
    }

    void carregarEquipes() {
        carregar("/api/equipes", tEquipes, "id", "nome", "modalidade", "chefeEquipe.id", "chefeEquipe.nome", "jogadores#", "jogadores*id");
    }

    void carregarEventos() {
        carregar("/api/modalidades", tModalidades, "id", "nome", "minJogadores", "maxJogadores");
        carregar("/api/eventos", tEventos, "id", "nome", "modalidade", "vagasDisponiveis", "vagasTotais", "status");
    }

    void carregarInscricoes() {
        carregar("/api/inscricoes", tInscricoes, "id", "equipeId", "eventoId", "chefeEquipeId", "status", "justificativa");
    }

    void carregarNotificacoes() {
        carregar("/notificacoes", tNotificacoes, "criadaEm", "canal", "destinatario", "assunto", "status", "erro");
    }

    /** GET silencioso: preenche a tabela com os campos pedidos (sem escrever no log). */
    void carregar(String caminho, DefaultTableModel modelo, String... campos) {
        FILA.submit(() -> {
            try {
                Resposta r = chamar("GET", GATEWAY + caminho, null);
                if (r.codigo() != 200) return;
                Object dados = Json.ler(r.corpo());
                List<Object[]> linhas = new ArrayList<>();
                if (dados instanceof List<?> lista) {
                    for (Object item : lista) {
                        if (!(item instanceof Map<?, ?> mapa)) continue;
                        Object[] linha = new Object[campos.length];
                        for (int i = 0; i < campos.length; i++) linha[i] = valor(mapa, campos[i]);
                        linhas.add(linha);
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    modelo.setRowCount(0);
                    linhas.forEach(modelo::addRow);
                });
            } catch (Exception e) {
                // serviço ainda subindo: a tabela fica como está
            }
        });
    }

    /** "a.b" navega em objetos; "lista#" devolve o tamanho; "lista*campo" junta um campo de cada item. */
    static String valor(Map<?, ?> mapa, String caminho) {
        if (caminho.contains("*")) {
            String[] partes = caminho.split("\\*");
            if (mapa.get(partes[0]) instanceof List<?> lista) {
                StringJoiner j = new StringJoiner(", ");
                for (Object o : lista) if (o instanceof Map<?, ?> m) j.add(texto(m.get(partes[1])));
                return j.toString();
            }
            return "";
        }
        if (caminho.endsWith("#")) {
            Object v = mapa.get(caminho.substring(0, caminho.length() - 1));
            return v instanceof List<?> l ? String.valueOf(l.size()) : "";
        }
        Object atual = mapa;
        for (String parte : caminho.split("\\.")) {
            if (!(atual instanceof Map<?, ?> m)) return "";
            atual = m.get(parte);
        }
        return texto(atual);
    }

    static String texto(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        int quebra = s.indexOf('\n');
        return quebra >= 0 ? s.substring(0, quebra) : s;
    }

    // ───────────────────────────── HTTP ─────────────────────────────

    static Resposta chamar(String metodo, String url, String json) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json");
        if (json == null) {
            b.method(metodo, HttpRequest.BodyPublishers.noBody());
        } else {
            b.header("Content-Type", "application/json; charset=utf-8")
                    .method(metodo, HttpRequest.BodyPublishers.ofString(json));
        }
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Resposta(r.statusCode(), r.body());
    }

    /** Envia a requisição, mostra pedido e resposta no log e, se der certo, roda "depois". */
    void enviar(String metodo, String caminho, String json, Runnable depois) {
        FILA.submit(() -> {
            String url = GATEWAY + caminho;
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("] ")
                    .append(metodo).append(' ').append(url).append('\n');
            if (json != null) sb.append(Json.formatar(json)).append('\n');
            try {
                Resposta r = chamar(metodo, url, json);
                sb.append("→ HTTP ").append(r.codigo()).append(' ').append(significado(r.codigo())).append('\n');
                sb.append(Json.formatar(r.corpo()));
                if (r.codigo() >= 500) {
                    sb.append("\n(Se os serviços acabaram de subir, espere de 30 a 60 s até aparecerem no Eureka.)");
                }
            } catch (java.net.ConnectException e) {
                sb.append("→ Sem conexão com o Gateway. Rode \"docker compose up -d\" na pasta do repositório.");
            } catch (Exception e) {
                sb.append("→ ERRO: ").append(e.getMessage());
            }
            escrever(sb.toString());
            if (depois != null) depois.run();
        });
    }

    static String significado(int codigo) {
        return switch (codigo) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 400 -> "Bad Request (regra de negócio ou dado inválido)";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 500 -> "Internal Server Error";
            case 503 -> "Service Unavailable (serviço ainda não registrado no Eureka?)";
            default -> "";
        };
    }

    void escrever(String texto) {
        SwingUtilities.invokeLater(() -> {
            if (!log.getText().isEmpty()) log.append("\n────────────────────────────────────────\n");
            log.append(texto);
            log.setCaretPosition(log.getDocument().getLength());
        });
    }

    /** Lista os serviços registrados no Eureka (é só para mostrar; o painel não usa isso para chamar ninguém). */
    void verificarEureka() {
        FILA.submit(() -> {
            String texto;
            try {
                Resposta r = chamar("GET", EUREKA + "/eureka/apps", null);
                Object dados = Json.ler(r.corpo());
                List<String> nomes = new ArrayList<>();
                if (dados instanceof Map<?, ?> m && m.get("applications") instanceof Map<?, ?> apps) {
                    Object lista = apps.get("application");
                    if (lista instanceof Map<?, ?>) lista = List.of(lista);
                    if (lista instanceof List<?> l) {
                        for (Object o : l) if (o instanceof Map<?, ?> app) nomes.add(texto(app.get("name")));
                    }
                }
                Collections.sort(nomes);
                texto = nomes.isEmpty()
                        ? "Eureka no ar, mas nenhum serviço registrado ainda (aguarde 30–60 s)."
                        : "Registrados no Eureka (" + nomes.size() + "): " + String.join(", ", nomes);
            } catch (Exception e) {
                texto = "Eureka fora do ar (" + EUREKA + "). Rode \"docker compose up -d\".";
            }
            String fim = texto;
            SwingUtilities.invokeLater(() -> statusEureka.setText(fim));
        });
    }

    // ───────────────────────────── Ajudantes de interface ─────────────────────────────

    static DefaultTableModel modelo(String... colunas) {
        return new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int linha, int coluna) { return false; }
        };
    }

    static JTable tabela(DefaultTableModel modelo) {
        JTable t = new JTable(modelo);
        t.setRowHeight(26);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return t;
    }

    static JScrollPane rolagem(JComponent c, String titulo) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(new TitledBorder(titulo));
        return s;
    }

    static JPanel linhaBotoes(JButton... botoes) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JButton b : botoes) p.add(b);
        return p;
    }

    JComponent comTabela(JPanel form, DefaultTableModel modelo, Runnable atualizar) {
        JPanel painel = new JPanel(new BorderLayout(0, 6));
        painel.setBorder(new EmptyBorder(6, 6, 6, 6));
        painel.add(form, BorderLayout.NORTH);
        painel.add(rolagem(tabela(modelo), "Cadastrados"), BorderLayout.CENTER);
        painel.add(linhaBotoes(botao("Atualizar lista", atualizar)), BorderLayout.SOUTH);
        return painel;
    }

    /** Botão cuja ação mostra erros de preenchimento numa caixa de diálogo. */
    JButton botao(String texto, Runnable acao) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> {
            try {
                acao.run();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(b, ex.getMessage(), "Preenchimento", JOptionPane.WARNING_MESSAGE);
            }
        });
        return b;
    }

    static JPanel formulario(String titulo, String[] rotulos, JComponent[] campos, JButton... botoes) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new TitledBorder(titulo));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 6, 3, 6);
        c.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < rotulos.length; i++) {
            c.gridy = i;
            c.gridx = 0;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0;
            p.add(new JLabel(rotulos[i] + ":"), c);
            c.gridx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            p.add(campos[i], c);
        }
        c.gridy = rotulos.length;
        c.gridx = 1;
        c.fill = GridBagConstraints.NONE;
        p.add(linhaBotoes(botoes), c);
        return p;
    }

    static String numero(JTextField f, String nome) {
        String t = f.getText().trim();
        if (!t.matches("\\d+")) throw new IllegalArgumentException("Preencha \"" + nome + "\" com um número.");
        return t;
    }

    static String listaDeNumeros(JTextField f) {
        StringJoiner j = new StringJoiner(",", "[", "]");
        for (String parte : f.getText().split("[,;\\s]+")) {
            if (parte.isBlank()) continue;
            if (!parte.matches("\\d+")) throw new IllegalArgumentException("Jogadores: use ids separados por vírgula (ex.: 2,3,4,5,6).");
            j.add(parte);
        }
        return j.toString();
    }

    static String campo(String nome, String valor) {
        return "\"" + nome + "\":" + Json.aspas(valor.trim());
    }

    static void abrirNoNavegador(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception ignorada) { /* tenta o xdg-open */ }
        try {
            new ProcessBuilder("xdg-open", url).start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Abra no navegador: " + url);
        }
    }

    // ───────────────────────────── JSON mínimo (sem bibliotecas) ─────────────────────────────

    static final class Json {
        private final String s;
        private int i;

        private Json(String s) { this.s = s; }

        static Object ler(String texto) {
            if (texto == null || texto.isBlank()) return null;
            Json j = new Json(texto);
            Object v = j.valor();
            j.espacos();
            if (j.i != j.s.length()) throw new IllegalArgumentException("JSON inválido");
            return v;
        }

        /** Devolve o JSON indentado; se não for JSON, devolve o texto como veio. */
        static String formatar(String texto) {
            if (texto == null || texto.isBlank()) return "(sem corpo)";
            try {
                StringBuilder sb = new StringBuilder();
                escrever(ler(texto), 0, sb);
                return sb.toString();
            } catch (Exception e) {
                return texto;
            }
        }

        static String aspas(String v) {
            StringBuilder sb = new StringBuilder("\"");
            for (char c : v.toCharArray()) {
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> {
                        if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                        else sb.append(c);
                    }
                }
            }
            return sb.append('"').toString();
        }

        private static void escrever(Object v, int nivel, StringBuilder sb) {
            String recuo = "  ".repeat(nivel + 1), fim = "  ".repeat(nivel);
            if (v instanceof Map<?, ?> m) {
                if (m.isEmpty()) { sb.append("{}"); return; }
                sb.append("{\n");
                Iterator<? extends Map.Entry<?, ?>> it = m.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<?, ?> e = it.next();
                    sb.append(recuo).append(aspas(String.valueOf(e.getKey()))).append(": ");
                    escrever(e.getValue(), nivel + 1, sb);
                    sb.append(it.hasNext() ? ",\n" : "\n");
                }
                sb.append(fim).append('}');
            } else if (v instanceof List<?> l) {
                if (l.isEmpty()) { sb.append("[]"); return; }
                sb.append("[\n");
                for (int k = 0; k < l.size(); k++) {
                    sb.append(recuo);
                    escrever(l.get(k), nivel + 1, sb);
                    sb.append(k < l.size() - 1 ? ",\n" : "\n");
                }
                sb.append(fim).append(']');
            } else if (v instanceof String str) {
                sb.append(aspas(str));
            } else {
                sb.append(v);
            }
        }

        private void espacos() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        private Object valor() {
            espacos();
            char c = s.charAt(i);
            switch (c) {
                case '{': return objeto();
                case '[': return lista();
                case '"': return string();
                case 't': i += 4; return Boolean.TRUE;
                case 'f': i += 5; return Boolean.FALSE;
                case 'n': i += 4; return null;
                default: return numero();
            }
        }

        private Map<String, Object> objeto() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            espacos();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                espacos();
                String chave = string();
                espacos();
                i++; // ':'
                m.put(chave, valor());
                espacos();
                if (s.charAt(i++) == '}') return m; // senão era ','
            }
        }

        private List<Object> lista() {
            List<Object> l = new ArrayList<>();
            i++;
            espacos();
            if (s.charAt(i) == ']') { i++; return l; }
            while (true) {
                l.add(valor());
                espacos();
                if (s.charAt(i++) == ']') return l;
            }
        }

        private String string() {
            StringBuilder sb = new StringBuilder();
            i++; // aspas de abertura
            while (s.charAt(i) != '"') {
                char c = s.charAt(i++);
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case 'r' -> sb.append('\r');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                        default -> sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
            i++; // aspas de fechamento
            return sb.toString();
        }

        private Object numero() {
            int inicio = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            String t = s.substring(inicio, i);
            if (t.isEmpty()) throw new IllegalArgumentException("JSON inválido");
            try {
                return Long.parseLong(t);
            } catch (NumberFormatException e) {
                return Double.parseDouble(t);
            }
        }
    }
}
