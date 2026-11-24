#!/usr/bin/env bash
# Fluxo completo de demonstracao, SO pelo API Gateway
# Sistema de Eventos de E-Sports - Grupo 2 (APSOO/UFRPE)
#
# Para que serve: roda de uma vez, pelo terminal, o mesmo fluxo da demonstracao
# do painel (UC02 -> UC01 -> UC04 -> UC03 -> UC05 -> UC06 + UC07) e confere o
# codigo HTTP de cada passo. Todas as chamadas passam pelo Gateway; o script nao
# conhece as portas dos servicos de negocio (elas sao aleatorias, registradas no Eureka).
#
# Como rodar (Ubuntu; precisa de curl e python3), com o sistema no ar
# (docker compose up -d), de qualquer pasta:
#     bash scripts/fluxo-gateway.sh
# As portas sao lidas do arquivo .env da raiz (GATEWAY_PORT e MAILPIT_WEB_PORT).
# Pode ser executado varias vezes: e-mails, CPFs e o nome do evento recebem um
# sufixo unico; a modalidade FPS e reaproveitada se ja existir.
# Termina com codigo 0 se todos os passos deram o resultado esperado.
set -u
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
[ -f "$RAIZ/.env" ] || { echo "Arquivo .env nao encontrado em $RAIZ"; exit 1; }
set -a; . "$RAIZ/.env"; set +a
GW=http://localhost:$GATEWAY_PORT
MAILPIT=http://localhost:$MAILPIT_WEB_PORT
S=$(date +%s)
T=$(mktemp); trap 'rm -f "$T"' EXIT
ERROS=0

# chama METODO CAMINHO CODIGO_ESPERADO [JSON]
chama() {
  local m=$1 p=$2 esperado=$3 d=${4:-}
  if [ -n "$d" ]; then
    code=$(curl -s -o "$T" -w '%{http_code}' -X "$m" "$GW$p" -H 'Content-Type: application/json' -d "$d")
  else
    code=$(curl -s -o "$T" -w '%{http_code}' -X "$m" "$GW$p")
  fi
  local marca=OK
  if [ "$code" != "$esperado" ]; then marca="ESPERAVA $esperado"; ERROS=$((ERROS+1)); fi
  echo ">>> $m $GW$p -> HTTP $code [$marca]"
  echo "    $(head -c 300 "$T")"
}
# campo chave1 [chave2...] -> valor do JSON da ultima resposta
campo() { python3 -c 'import sys,json; d=json.load(open(sys.argv[1]))
for k in sys.argv[2:]: d=d[k]
print(d)' "$T" "$@"; }

echo "Gateway: $GW   Mailpit: $MAILPIT"
echo "Esperando o Gateway enxergar os 4 servicos (Eureka)..."
for p in /api/usuarios /api/eventos /api/inscricoes "/notificacoes?limite=1"; do
  for _ in $(seq 1 60); do
    [ "$(curl -s -o /dev/null -w '%{http_code}' "$GW$p")" = 200 ] && break; sleep 3
  done
done

echo; echo "== 1. UC02 - Organizador, chefe e 5 jogadores (cada um recebe e-mail)"
chama POST /api/usuarios 201 "{\"nome\":\"Organizadora Ana\",\"email\":\"org.$S@demo.com\",\"cpf\":\"1$S\",\"tipoPerfil\":\"ORGANIZADOR\"}"
ORG=$(campo usuario id)
chama POST /api/usuarios 201 "{\"nome\":\"Chefe Bruno\",\"email\":\"chefe.$S@demo.com\",\"cpf\":\"2$S\",\"tipoPerfil\":\"CHEFE_EQUIPE\"}"
CHEFE=$(campo usuario id)
JOG=()
for n in 1 2 3 4 5; do
  chama POST /api/usuarios 201 "{\"nome\":\"Jogador $n\",\"email\":\"jog$n.$S@demo.com\",\"cpf\":\"3$n$S\",\"tipoPerfil\":\"JOGADOR\"}"
  JOG+=("$(campo usuario id)")
done

echo; echo "== 2. UC01 - Modalidade FPS"
code=$(curl -s -o "$T" -w '%{http_code}' "$GW/api/modalidades")
if grep -q '"nome":"FPS"' "$T"; then
  echo "    FPS ja cadastrada (execucao anterior): reaproveitando"
else
  chama POST /api/modalidades 201 "{\"nome\":\"FPS\",\"descricao\":\"Tiro em primeira pessoa\",\"minJogadores\":5,\"maxJogadores\":5,\"organizadorId\":$ORG}"
fi

echo; echo "== 3. UC04 - Evento FPS com 2 vagas"
chama POST /api/eventos 201 "{\"nome\":\"Copa FPS $S\",\"descricao\":\"Demonstracao\",\"modalidade\":\"FPS\",\"organizadorId\":$ORG,\"vagasTotais\":2}"
EV=$(campo id)

echo; echo "== 4. UC03 - Equipe Alpha com 4 jogadores (a StrategyFPS exige 5: 400)"
chama POST /api/equipes 400 "{\"nome\":\"Alpha\",\"modalidade\":\"FPS\",\"chefeId\":$CHEFE,\"jogadoresIds\":[${JOG[0]},${JOG[1]},${JOG[2]},${JOG[3]}]}"
echo "== 4b. UC03 - Equipe Alpha com 5 jogadores (201)"
chama POST /api/equipes 201 "{\"nome\":\"Alpha\",\"modalidade\":\"FPS\",\"chefeId\":$CHEFE,\"jogadoresIds\":[$(IFS=,; echo "${JOG[*]}")]}"
EQ=$(campo id)

echo; echo "== 5. UC05 - Chefe solicita a inscricao (InscricaoFacade: Usuarios -> Eventos -> PENDENTE)"
chama POST /api/inscricoes 201 "{\"equipeId\":$EQ,\"eventoId\":$EV,\"chefeId\":$CHEFE}"
INS=$(campo id)

echo; echo "== 6. UC06 - Organizador aprova (ocupa a vaga, APROVADA, e-mail pelo UC07)"
chama PATCH "/api/inscricoes/$INS/avaliar" 200 "{\"organizadorId\":$ORG,\"aprovada\":true,\"justificativa\":\"Documentacao em ordem\"}"
echo "    status: $(campo inscricao status)   notificacaoEnviada: $(campo notificacaoEnviada)"
chama GET "/api/eventos/$EV" 200
echo "    vagasDisponiveis: $(campo vagasDisponiveis) de $(campo vagasTotais)"

echo; echo "== 7. E-mails desta execucao no Mailpit ($MAILPIT)"
sleep 1
curl -s "$MAILPIT/api/v1/messages?limit=100" | python3 -c "
import sys,json
for m in json.load(sys.stdin)['messages']:
    if '.$S@' in m['To'][0]['Address']: print('   ', m['To'][0]['Address'], '|', m['Subject'])"

echo
if [ "$ERROS" -eq 0 ]; then echo "Fluxo concluido: todos os passos deram o resultado esperado."
else echo "Fluxo concluido com $ERROS passo(s) diferente(s) do esperado."; fi
exit "$ERROS"
