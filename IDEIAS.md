# Ideias — Caça Fantasma

Anotado em 29/09/2026. Atualizado depois do commit `b9b5794` (dificuldade
real + companheiros). Referências de linha valem para esse commit e devem
ser conferidas antes de usar.

## Estado atual

- 9 cenários (8 + chefe), 7 equipamentos de 3 níveis, 3 consumíveis,
  8 conquistas, 6 postos, 2 companheiros.
- `GameData.kt` (287 linhas) guarda todo o conteúdo como listas em um
  único `object`. Adicionar conteúdo é barato porque é 100% data-driven.
- `MainActivity.kt` tem 1225 linhas e constrói todas as telas por código,
  sem XML de layout nem Compose. Telas são strings em `showScreen()`.
- Progresso vive em SharedPreferences com chaves planas e sem namespace:
  `cap_$index`, `lv_$id`, `itm_$id`, `ach_$id`, `money`, `max_unlocked`.

## Resolvido — dificuldade decorativa (era o maior defeito)

O campo `diff` dos cenários só aparecia nas estrelas e no texto
"recomendado: N"; não entrava em nenhum cálculo. A dificuldade real vinha
só de `sc.rounds.size`, o que invertia tudo: o chefe, com 6 rodadas, dava
*mais* chance de montar evidência que a Casa Abandonada, com 4. Com
equipamento bom os dois davam 95%.

**Resolvido em `b9b5794`.** `diff` agora modula a chance base, cada
evidência acima do necessário soma 7 pontos e abaixo desconta, e
`GameData.needEvidence` escala por diff, não por `rounds.size`. Curva
medida: 95% no primeiro cenário até 68% no chefe, monotônica. No chefe,
cada evidência a mais rende ~7 pontos (0 evidência = 26%, 6 = 68%).

**A fórmula inteira está em `GameData.catchChance`** — um só ponto, com os
parâmetros como constantes ao lado (`DIFF_BASE_STEP`, `EVIDENCE_STEP`,
`CHANCE_CAP`...). `MainActivity.catchChance` é só um wrapper que passa
`sc.diff`, a evidência efetiva, o equipamento, o nível e a coragem. É
por ali que se ajusta balanceamento; a Ideia 4 vira somar um `when` nessa
função.

## Ideia 1 — Investigações roteirizadas (agora o maior ganho de design)

Cada `Round(spot, ideal, hit)` fixa qual equipamento é o certo. Na segunda
vez que se joga o mesmo cenário a resposta é óbvia.

**Proposta:** embaralhar a ordem das vistorias e sortear o equipamento
ideal de cada uma, mantendo o texto do resultado genérico. O `spot` e o
`hit` já descrevem a pista, então não exige reescrever conteúdo.

**Ganho extra:** com o `diff` agora valendo, sortear o equipamento ideal
também faz a dificuldade variar entre repetições do mesmo cenário, em vez
de a chance ser fixa.

**Risco:** muda o balanceamento. Exige re-testar os 9 cenários à mão.

## Ideia 2 — Recompensa sem teto (fura a economia)

`Player.addCapture` (`Player.kt:38`) paga `baseReward` inteiro toda vez.
Caçar Casa Abandonada em loop gera ~R$300 por rodada indefinidamente, e
a loja perde sentido depois de um tempo.

**Propostas:**
- lealdade decrescente (recompensa cai ~30% a cada repetição), ou
- teto por cenário, deixando o último como o único de prêmio cheio.

**Impacto:** muda economia e mexe em save de quem já instalou.

## Ideia 3 — Contratos noturnos

Escolher 2 cenários e receber bônus se completar os dois sem derrota.
Dá um motivo para repetir cenário já dominado e cria um objetivo de
curto prazo diferente de "capturar tudo".

## Ideia 4 — Equipamento de runa condicional

Peça extra que só funciona em condição específica (aumenta a chance se a
coragem estiver abaixo de X, se o confronto for o primeiro round, se o
fantasma for do tipo tal). Reaproveita `GameData.catchChance` como ponto
único de extensão — a fórmula inteira já está lá, então é somar um `when`
e pronto.

## Ideia 5 — Modo Caça Cega

O equipamento do confronto final não é revelado antes da escolha.
Custa uma tela (esconder os nomes em `renderConfront`) e muda
completamente a decisão tática.

## Ideia 6 — Corrigir o custo de pular vistoria

`skipRound` (`MainActivity.kt:999`) custa só 6 de coragem contra 12 de
errar, então pular nunca é a pior opção e a escolha fica óbvia.
Variar o custo por cenário ou pelo round deixaria a decisão real.

## Ideia 7 — Mudo / controle de volume

`SoundManager.kt` (48 linhas) não tem nenhum controle de volume e o jogo
toca 7 sons. Botão de mudo é a mudança mais barata da lista.

---

## Mundos novos: a decisão de momento

O que trava uma campanha paralela hoje:

1. **`maxUnlocked` é um inteiro global** (`Player.kt:16`) e o desbloqueio
   é sequencial (`Player.kt:43`, `index + 1`). Mundo novo com cadeia própria
   precisa de um contador por mundo.
2. **Chaves de SharedPreferences sem namespace.** Dois mundos com ids
   repetidos sobrescrevem um ao outro — e `uv` se repetiria em qualquer
   mundo novo. Precisa virar `cap_<mundo>_<index>`.
3. **A UI pressupõe uma campanha só.** `bossCaptured()`
   (`Player.kt:74`), `allScenariosCaptured()` (`Player.kt:71`) e a
   conquista "Colecionador" pressupõem um único chefe e uma conclusão
   única.
4. **Os companheiros destravam por `totalCaptures` global**
   (`Player.kt:20`), um contador solto. Num mundo paralelo, um jogador com
   8 capturas no mundo A levaria Baltazar de graça no mundo B. Mesmo
   namespacing da ideia 2.

**Recomendação:** refatorar o que é genérico de mundo *antes* de
adicionar conteúdo — namespacing das chaves, um conceito de
`World`/`Campaign` em `GameData`, e `maxUnlocked` virando mapa. É barato
enquanto o save é pequeno; fica cada vez mais caro depois, porque
mexer em save de usuário instalado exige migração.

Alternativa mais barata: continuar estendendo o mundo atual (mais
cenários, mais equipamentos). Praticamente de graça.

---

## Pendências técnicas conhecidas

- **`local.properties` tem a senha do keystore em texto puro.** O arquivo
  é gitignored (nada foi para o GitHub), mas está em disco. Ideia:
  alterar `build.gradle.kts` para ler `storeFile`/`storePassword`/
  `keyAlias`/`keyPassword` de variáveis de ambiente.
- **Senha da chave é `cacafantasma`**, igual à do store. A primeira
  tentativa com `caca` custou uma build de 50 minutos antes de falhar.
  Para próxima build, testar a senha com `keytool -importkeystore` antes
  de rodar o Gradle.
- **Build é lento neste ambiente** (2 CPUs, 2,7 GB RAM). O daemon do
  Gradle já foi morto por OOM. Usar `--no-daemon --max-workers=1` e
  **nunca** rodar `clean` (leva ~50 min em vez de ~1 min).
- **Save export/import nunca foi testado em aparelho.** A lógica foi
  validada só por simulação em Python. Falta o teste real de
  salvar → zerar → restaurar.
- **4 warnings pré-existentes** em `MainActivity.kt`: parâmetro `sc`
  não usado, `toUpperCase()` depreciado, variáveis `eff` e `prePost`
  não usadas. Nenhum vem das mudanças de dificuldade/companheiros.
- **Curva de dificuldade nunca foi testada jogando.** Os números de
  `b9b5794` vêm de simulação em Python com as mesmas constantes do
  código, não de jogo real. Se algum cenário ficar trivial ou
  impossível na prática, mexer em `DIFF_BASE_STEP` / `EVIDENCE_STEP` em
  `GameData`.
- **O Gradle às vezes reporta duração absurda** (já saiu "6h 44m" numa
  build que terminou em 1 min com a máquina up há 3 h). Se o número não
  fizer sentido, conferir o mtime do APK em
  `app/build/outputs/apk/` em vez de acreditar no console.
