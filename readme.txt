SISTEMA DE GERENCIAMENTO DE EVENTOS DE E-SPORTS - INSTRUÇÕES DE EXECUÇÃO
==========================================================================

APSOO (14108) - UFRPE - Prof. Lucas Albertins - Grupo 2
Arthur Leonardo, Guilherme Guimarães, Júlio Lucena e Otávio Olímpio

O sistema tem 7 contêineres, orquestrados pelo Docker Compose:
  - api-gateway (Spring Cloud Gateway): a única porta de entrada da aplicação;
  - service-discovery (Eureka): o catálogo onde cada serviço se registra pelo nome;
  - servico-usuarios-equipes, servico-eventos, servico-inscricoes e
    servico-notificacoes: os 4 microsserviços de negócio, cada um com o seu
    banco SQLite;
  - mailpit: servidor de e-mail de teste (os e-mails enviados aparecem no navegador).

Os comandos abaixo valem para o Ubuntu (Terminal) e para o Windows (PowerShell),
salvo quando indicado. Todos são executados NA PASTA RAIZ DO REPOSITÓRIO
(a pasta que tem o docker-compose.yml e o arquivo .env).


1. REQUISITOS
-------------

  - Git.
  - Docker com o Docker Compose:
      Ubuntu:  Docker Engine + plugin "docker compose"
               (https://docs.docker.com/engine/install/ubuntu/).
               Se aparecer "permission denied", rode os comandos com sudo ou
               adicione o seu usuário ao grupo docker.
      Windows: Docker Desktop, que precisa estar ABERTO (ícone da baleia
               parado na bandeja) antes de qualquer comando docker.
  - JDK 21: só para abrir o painel gráfico (item 6) e rodar os testes (item 9).
    Confira com:  java -version
  - Maven 3.9: só para rodar os testes (item 9) ou executar fora do Docker (item 10).

  Não é preciso instalar Java nem Maven para subir o sistema: o Docker compila
  tudo dentro dos contêineres.


2. OBTER O CÓDIGO
-----------------

    git clone https://github.com/otavioolimpio/sistema-eventos-esportivos.git
    cd sistema-eventos-esportivos

  (Se você recebeu o projeto em .zip, descompacte-o e entre na pasta
  sistema-eventos-esportivos.)


3. PORTAS: O ARQUIVO .env
-------------------------

  Nenhuma porta está escrita no código. Todas ficam no arquivo .env, na raiz:

    GATEWAY_PORT=8080        API Gateway: a única entrada da aplicação
    EUREKA_PORT=8761         Eureka: painel com os serviços registrados
    MAILPIT_WEB_PORT=8025    Mailpit: caixa de e-mail de teste, no navegador
    MAILPIT_SMTP_PORT=1025   Mailpit: servidor SMTP usado pelo Serviço de
                             Notificações (só dentro da rede do Docker)

  Para mudar uma porta, mude só a linha dela no .env e suba de novo
  (item 4). O docker-compose.yml, os serviços e o painel leem o mesmo arquivo.

  Os 4 serviços de negócio NÃO aparecem no .env: cada um sobe numa porta
  aleatória, escolhida na hora, e informa essa porta ao Eureka. O Gateway e os
  outros serviços descobrem o endereço por lá, pelo nome do serviço. Esses
  serviços também não ficam acessíveis fora do Docker: tudo passa pelo Gateway.


4. SUBIR O SISTEMA
------------------

    docker compose up --build -d

  A primeira vez demora de 8 a 30 minutos (baixa as imagens e as dependências
  do Maven e compila os 6 módulos). Das próximas vezes basta:

    docker compose up -d

  Confira se os 7 contêineres estão "Up":

    docker compose ps

  Devem aparecer: api-gateway, mailpit, service-discovery,
  servico-eventos, servico-inscricoes, servico-notificacoes e
  servico-usuarios-equipes.


5. ESPERAR O REGISTRO NO EUREKA
-------------------------------

  Os serviços levam cerca de 60 segundos para se registrar. Abra o painel do
  Eureka no navegador:

    http://localhost:<EUREKA_PORT>

  (com o .env padrão: http://localhost:8761)

  e espere aparecerem os 5 registros com status UP:
    API-GATEWAY, SERVICO-EVENTOS, SERVICO-INSCRICOES, SERVICO-NOTIFICACOES e
    SERVICO-USUARIOS-EQUIPES.

  Na coluna "Status" aparece "UP (1) - " seguido do identificador da
  instância, no formato nome:valor-aleatório (por exemplo,
  "UP (1) - servico-eventos:3a99dfd9d746a4a8100f3c3bee82ab5d"). A porta
  aleatória NÃO aparece nessa coluna. Ela aparece:
    - ao passar o mouse sobre esse identificador: o link aponta para
      http://<IP do contêiner>:<porta>/actuator/info;
    - em http://localhost:<EUREKA_PORT>/eureka/apps, no campo <port> de cada
      instância (ao lado de <ipAddr>).
  Esse IP e essa porta só existem dentro da rede do Docker.

  Depois que os 5 aparecem, o Gateway leva até mais 30 segundos para atualizar
  a lista dele (veja o item 11).


6. ABRIR O PAINEL GRÁFICO (JAVA SWING)
--------------------------------------

  Ainda na pasta raiz do repositório:

    java painel-demo/PainelEsports.java

  (No Windows, "java painel-demo\PainelEsports.java" também funciona.)

  O painel lê GATEWAY_PORT, EUREKA_PORT e MAILPIT_WEB_PORT do .env da pasta
  atual (ou da pasta pai) e mostra no topo de onde veio cada endereço. Se não
  achar o .env, mostra uma janela de erro: rode-o a partir da raiz do
  repositório. Também é possível informar os endereços à mão:

    java -Dgateway=http://localhost:8080 -Deureka=http://localhost:8761 -Dmailpit=http://localhost:8025 painel-demo/PainelEsports.java

  ou pelas variáveis de ambiente GATEWAY_URL, EUREKA_URL e MAILPIT_URL.

  Fluxo sugerido no painel (uma aba por caso de uso):
    1. Usuários (UC02): cadastre um ORGANIZADOR, um CHEFE_EQUIPE e 5 JOGADORES.
       Cada um recebe a senha provisória por e-mail.
    2. Modalidades e Eventos (UC01, UC04): cadastre a modalidade FPS e um
       evento FPS com 2 vagas.
    3. Equipes (UC03): crie uma equipe FPS com 4 jogadores (recusada: a
       Strategy de FPS exige 5) e depois com os 5.
    4. Inscrições (UC05, UC06): solicite a inscrição da equipe no evento
       (PENDENTE) e aprove como organizador (APROVADA, a vaga é ocupada).
    5. Notificações (UC07): veja os e-mails registrados; abra o Mailpit.

  Sem o painel, o mesmo fluxo roda pelo terminal (Ubuntu; precisa de curl e
  python3). Ele pode ser executado várias vezes:

    bash scripts/fluxo-gateway.sh

  ou, no Windows (PowerShell), o roteiro interativo:

    powershell -ExecutionPolicy Bypass -File .\demo.ps1


7. ENDEREÇOS
------------

  Sempre http://localhost:<porta do .env>. Com o .env padrão:

    API Gateway  http://localhost:<GATEWAY_PORT>      http://localhost:8080
    Eureka       http://localhost:<EUREKA_PORT>       http://localhost:8761
    Mailpit      http://localhost:<MAILPIT_WEB_PORT>  http://localhost:8025

  Rotas do Gateway:
    /api/usuarios/**, /api/equipes/**       -> servico-usuarios-equipes
    /api/modalidades/**, /api/eventos/**    -> servico-eventos
    /api/inscricoes/**                      -> servico-inscricoes
    /notificacoes/**                        -> servico-notificacoes

  Exemplo (Ubuntu; no PowerShell use curl.exe):

    curl http://localhost:8080/api/usuarios


8. PERSISTÊNCIA DOS DADOS
-------------------------

  Cada serviço grava o seu banco SQLite em /data, dentro de um volume do
  Docker (usuarios-data, eventos-data, inscricoes-data e notificacoes-data).

    docker compose down       desliga tudo e MANTÉM os dados
    docker compose down -v    desliga tudo e APAGA os volumes (bancos zerados)
    docker volume ls          lista os volumes

  Para abrir um banco (por exemplo, no DB Browser for SQLite), copie-o do
  contêiner para a pasta atual, com o sistema no ar:

    docker compose cp servico-usuarios-equipes:/data/usuarios_equipes.db .

  Os outros bancos: servico-eventos:/data/eventos.db,
  servico-inscricoes:/data/inscricoes.db e
  servico-notificacoes:/data/notificacoes.db.

  Os e-mails do Mailpit ficam só na memória: somem quando o Mailpit reinicia.


9. TESTES AUTOMATIZADOS
-----------------------

  Precisam do JDK 21 e do Maven, mas não do Docker nem do .env:

    mvn clean test

  Resultado esperado: 19 testes passando e "BUILD SUCCESS".


10. EXECUTAR FORA DO DOCKER (OPCIONAL)
--------------------------------------

  Cada módulo pode rodar direto na máquina com "mvn spring-boot:run", de dentro
  da pasta do módulo. Como não há porta no código, defina antes as variáveis:

    service-discovery           EUREKA_PORT
    api-gateway                 GATEWAY_PORT e EUREKA_PORT (ou EUREKA_URL)
    os 4 serviços de negócio    EUREKA_PORT (ou EUREKA_URL); a porta HTTP
                                deles é sempre aleatória
    servico-notificacoes        também SPRING_MAIL_PORT (porta SMTP); o
                                servidor é localhost (ou MAIL_HOST)

  EUREKA_URL tem a forma http://localhost:8761/eureka/.

  Exemplo no Ubuntu (um terminal por módulo, começando pelo Eureka):

    export EUREKA_PORT=8761 GATEWAY_PORT=8080
    cd service-discovery && mvn spring-boot:run

  Exemplo no Windows (PowerShell):

    $env:EUREKA_PORT="8761"; $env:GATEWAY_PORT="8080"
    cd service-discovery; mvn spring-boot:run

  Fora do Docker, cada banco SQLite é criado na pasta do próprio módulo.


11. PROBLEMAS COMUNS
--------------------

  - Erro 500 ou 503 logo depois de subir: os serviços ainda não se registraram
    no Eureka, ou o Gateway ainda não atualizou a lista dele. Espere os 5
    registros no Eureka (item 5) e mais até 30 segundos, e tente de novo.

  - "port is already allocated" / "address already in use" ao subir: outra
    aplicação já usa uma das portas. Troque o valor no .env (por exemplo,
    GATEWAY_PORT=9080) e rode de novo "docker compose up -d".

  - Windows: "error during connect" ou "cannot find the file specified":
    o Docker Desktop não está aberto. Abra-o, espere ele iniciar e repita.

  - Chamar um serviço de negócio direto, sem o Gateway, não funciona: é o
    esperado. Eles não publicam porta fora do Docker; use o Gateway.

  - O painel mostra a janela "configuração não encontrada": rode o comando
    "java painel-demo/PainelEsports.java" a partir da raiz do repositório.

  - Para ver o log de um serviço:  docker compose logs -f servico-inscricoes
