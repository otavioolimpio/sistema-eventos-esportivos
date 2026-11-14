# Roteiro da demonstração — Sistema de Eventos de E-Sports (Grupo 2)
# Serviço de Usuários e Equipes (8081) + Serviço de Notificações (8084) + Mailpit (8025)
#
# Uso, no PowerShell, de dentro da pasta do repositório sistema-eventos-esportivos:
#   powershell -ExecutionPolicy Bypass -File C:\caminho\do\demo.ps1            (containers já rodando)
#   powershell -ExecutionPolicy Bypass -File C:\caminho\do\demo.ps1 -Zerar     (apaga os bancos e sobe tudo do zero)
# Precisa de: Docker Desktop em execução.
#
# A cada passo o script mostra o que vai fazer e espera você apertar Enter.

param([switch]$Zerar)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'   # esconde a barra de progresso das chamadas HTTP

$USUARIOS = 'http://localhost:8081'
$NOTIFICACOES = 'http://localhost:8084'
$MAILPIT = 'http://localhost:8025'
if ($env:DEMO_USUARIOS) { $USUARIOS = $env:DEMO_USUARIOS }
if ($env:DEMO_NOTIFICACOES) { $NOTIFICACOES = $env:DEMO_NOTIFICACOES }
if ($env:DEMO_MAILPIT) { $MAILPIT = $env:DEMO_MAILPIT }

$SUF = Get-Date -Format 'HHmmss'   # deixa e-mails e CPFs únicos, para poder rodar mais de uma vez

if (-not (Test-Path 'docker-compose.yml') -or -not (Test-Path 'servico-notificacoes')) {
    Write-Host 'Rode este script de dentro da pasta do repositório sistema-eventos-esportivos' -ForegroundColor Red
    Write-Host '(a que tem o docker-compose.yml e as pastas servico-*).' -ForegroundColor Red
    exit 1
}

function Titulo($texto) { Write-Host ''; Write-Host "== $texto ==" -ForegroundColor Cyan }
function Fala($texto) { Write-Host $texto -ForegroundColor Yellow }
function Pausa { Write-Host ''; Read-Host '[Enter para continuar]' | Out-Null }

function Compose {
    & docker compose @args
    if ($LASTEXITCODE -ne 0) { throw "docker compose $args falhou (código $LASTEXITCODE)" }
}

function Mostrar($texto) {
    try { $texto | ConvertFrom-Json | ConvertTo-Json -Depth 8 | Write-Host }
    catch { Write-Host $texto }
}

# POST com JSON. Mostra o código HTTP e o corpo; devolve um objeto com Codigo e Corpo.
function Post($url, $dados) {
    $json = $dados | ConvertTo-Json -Compress -Depth 5
    Write-Host "POST $url" -ForegroundColor White
    Write-Host $json
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    try {
        $r = Invoke-WebRequest -Uri $url -Method Post -Body $bytes `
            -ContentType 'application/json; charset=utf-8' -UseBasicParsing
        $codigo = [int]$r.StatusCode
        $corpo = $r.Content
    } catch {
        $resp = $_.Exception.Response
        if ($null -eq $resp) { throw }
        $codigo = [int]$resp.StatusCode
        $corpo = $_.ErrorDetails.Message
        if (-not $corpo) {
            $leitor = New-Object System.IO.StreamReader($resp.GetResponseStream())
            $corpo = $leitor.ReadToEnd()
        }
    }
    Write-Host ''
    Write-Host "-> HTTP $codigo" -ForegroundColor White
    Mostrar $corpo
    return [pscustomobject]@{ Codigo = $codigo; Corpo = $corpo }
}

function NovoUsuario($nome, $email, $cpf, $perfil) {
    return [ordered]@{ nome = $nome; email = $email; cpf = $cpf; tipoPerfil = $perfil }
}

function UltimaNotificacao {
    $lista = Invoke-RestMethod "$NOTIFICACOES/notificacoes?limite=1"
    $n = $lista[0]
    foreach ($campo in 'canal', 'destinatario', 'status', 'erro') {
        $valor = $n.$campo
        if ($null -eq $valor) { $valor = 'null' } else { $valor = ("$valor" -split "`r?`n")[0] }
        Write-Host ("  {0}: {1}" -f $campo, $valor)
    }
}

function TotalEmails { return (Invoke-RestMethod "$MAILPIT/api/v1/messages").total }

function EstaNoAr($url) {
    try { Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3 | Out-Null; return $true }
    catch { return $false }
}

# ─────────────────────────────────────────────────────────────
if ($Zerar) {
    Titulo 'Preparação: apagando os bancos e subindo tudo do zero'
    Compose down -v
    Compose up --build -d
    Fala 'Aguardando os serviços subirem...'
    for ($i = 0; $i -lt 60; $i++) {
        if ((EstaNoAr "$USUARIOS/api/usuarios") -and (EstaNoAr "$NOTIFICACOES/notificacoes")) { break }
        Start-Sleep -Seconds 2
    }
}

Titulo '0. Os três contêineres no ar'
Compose ps
Fala "Abra no navegador: $MAILPIT (caixa de e-mail de teste)"
Pausa

# ─────────────────────────────────────────────────────────────
Titulo '1. UC02 + UC07 - Cadastrar usuário envia as credenciais por e-mail'
Fala 'O Organizador cadastra a Ana como Chefe de Equipe. O Serviço de Usuários gera uma senha'
Fala 'provisória e chama o Serviço de Notificações (POST /notificacoes).'
Fala 'Lá dentro: NotificationClientManager.getInstance() (Singleton) escolhe o EmailAdapter (Adapter).'
Pausa
$r = Post "$USUARIOS/api/usuarios" (NovoUsuario 'Ana' "ana.$SUF@exemplo.com" "cpf-ana-$SUF" 'CHEFE_EQUIPE')
$chefeId = ($r.Corpo | ConvertFrom-Json).usuario.id
Fala 'credenciaisEnviadas: true -> atualize o Mailpit: chegou o e-mail "Suas credenciais de acesso".'
Pausa
Fala 'O Serviço de Notificações registrou o envio (GET /notificacoes):'
UltimaNotificacao
Pausa

# ─────────────────────────────────────────────────────────────
Titulo '2. RN03 / FA03 - Falha no e-mail NÃO desfaz o cadastro'
Fala 'Vamos derrubar o servidor de e-mail (Mailpit) e cadastrar outro usuário.'
Pausa
Compose stop mailpit
$r = Post "$USUARIOS/api/usuarios" (NovoUsuario 'Bruno' "bruno.$SUF@exemplo.com" "cpf-bruno-$SUF" 'JOGADOR')
Fala 'HTTP 201 com credenciaisEnviadas: false e um aviso: o usuário foi cadastrado mesmo assim.'
Fala 'No Serviço de Notificações, a tentativa ficou registrada como FALHA (FA01):'
UltimaNotificacao
Fala 'O EmailAdapter transformou o erro do SMTP em FalhaEnvioException, e o Singleton'
Fala 'devolveu ResultadoEnvio.falha() em vez de deixar a exceção escapar.'
Pausa
Compose start mailpit
Fala 'Mailpit de volta no ar. Atenção: ele guarda os e-mails só na memória, então a caixa'
Fala 'recomeça vazia (o e-mail da Ana some do navegador; o registro dela continua no banco).'
Pausa

# ─────────────────────────────────────────────────────────────
Titulo '3. UC03 - Strategy valida o elenco pela modalidade'
Fala 'Cadastrando 5 jogadores para montar uma equipe de FPS'
Fala '(cada um recebe o próprio e-mail de credenciais no Mailpit)...'
$jogadores = @()
foreach ($i in 1..5) {
    $dados = NovoUsuario "Jogador $i" "jogador$i.$SUF@exemplo.com" "cpf-j$i-$SUF" 'JOGADOR'
    $resp = Invoke-RestMethod -Uri "$USUARIOS/api/usuarios" -Method Post -ContentType 'application/json; charset=utf-8' `
        -Body ([System.Text.Encoding]::UTF8.GetBytes(($dados | ConvertTo-Json -Compress)))
    $jogadores += [int]$resp.usuario.id
}
Fala "Jogadores criados com ids: $($jogadores -join ' ')  (chefe: Ana, id $chefeId)"
Pausa

Fala 'Tentativa 1: equipe de FPS com só 4 jogadores. A StrategyFPS exige exatamente 5.'
Pausa
$r = Post "$USUARIOS/api/equipes" ([ordered]@{
        nome = 'Alpha'; modalidade = 'FPS'; chefeId = $chefeId; jogadoresIds = @($jogadores[0..3]) })
Fala 'HTTP 400: o ContextoValidacaoElenco escolheu a StrategyFPS pela modalidade, e ela recusou.'
Pausa

Fala 'Tentativa 2: a mesma equipe com 5 jogadores.'
Pausa
$antes = TotalEmails
$r = Post "$USUARIOS/api/equipes" ([ordered]@{
        nome = 'Alpha'; modalidade = 'FPS'; chefeId = $chefeId; jogadoresIds = @($jogadores) })
Fala "HTTP 201: equipe criada. A Ana recebeu o aviso por e-mail (Mailpit: $antes -> $(TotalEmails) mensagens)."
Pausa

# ─────────────────────────────────────────────────────────────
Titulo 'Resumo'
Fala 'Usuários cadastrados:'
$usuarios = Invoke-RestMethod "$USUARIOS/api/usuarios"
foreach ($u in $usuarios) {
    Write-Host ("  {0,3}  {1,-10} {2}" -f $u.id, $u.nome, $u.tipoPerfil)
}
Fala 'Notificações registradas:'
$notificacoes = Invoke-RestMethod "$NOTIFICACOES/notificacoes"
foreach ($n in $notificacoes) {
    Write-Host ("  {0,-8} {1,-6} {2}" -f $n.status, $n.canal, $n.destinatario)
}
Fala ''
Fala 'Fim. Para desligar tudo: docker compose down'
