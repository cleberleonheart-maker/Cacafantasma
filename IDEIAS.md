# Ideias — Caça Fantasma

Anotado em 29/09/2026, logo após a publicação da v1.3. Referências de linha
valem para o commit `127e4e1` e devem ser conferidas antes de usar.

## Estado atual

- 9 cenários (8 + chefe), 7 equipamentos de 3 níveis, 3 consumíveis,
  8 conquistas, 6 postos.
- `GameData.kt` (238 linhas) guarda todo o conteúdo como listas em um
  único `object`. Adicionar conteúdo é barato porque é 100% data-driven.
- `MainActivity.kt` tem 1173 linhas e constrói todas as telas por código,
  sem XML de layout nem Compose. Telas são strings em `showScreen()`.
- Progresso vive em SharedPreferences com chaves planas e sem namespace:
  `cap_$index`, `lv_$id`, `itm_$id`, `ach_$id`, `money`, `max_unlocked`.

## Ideia 1 — Investigations roteirizadas (maior ganho de design)

Hoje cada `Round(spot, ideal, hit)` fixa qual equipamento é o certo. Na
segunda vez que se joga o mesmo cenário a resposta é óbvia, e o único
aleatório é `Random.nextFloat() < hitChance` (`MainActivity.kt:974`).

**Proposta:** embaralhar a ordem das vistorias e sortear o equipamento
ideal de cada uma, mantendo o texto do resultado genérico. O `spot` e o
`hit` já descrevem a pista, então não exige reescrever conteúdo.

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
courage estiver abaixo de X, se o confronto for o primeiro round, se o
fantasma for do tipo tal). Reaproveita o `catchChance` como ponto único
de extensão.

## Ideia 5 — Modo Caça Cega

O equipamento do confronto final não é revelado antes da escolha.
Custa uma tela (esconder os nomes em `renderConfront`) e muda
completamente a decisão tática.

## Ideia 6 — Corrigir o custo de pular vistoria

`skipRound` (`MainActivity.kt:948`) custa só 6 de coragem contra 12 de
errar, então pular nunca é a pior opção e a escolha fica óbvia.
Variar o custo por cenário ou pelo round deixaria a decisão real.

## Ideia 7 — Mudo / controle de volume

`SoundManager.kt` (48 linhas) não tem nenhum controle de volume e o jogo
toca 7 sons. Botão de mudo é a mudança mais barata da lista.

---

## Mundos novos: a decisão de momento

O que trava uma campanha paralela hoje:

1. **`maxUnlocked` é um inteiro global** (`Player.kt:38`) e o desbloqueio
   é sequencial (`index + 1`). Mundo novo com cadeia própria precisa de
   um contador por mundo.
2. **Chaves de SharedPreferences sem namespace.** Dois mundos com ids
   repetidos sobrescrevem um ao outro — e `uv` se repetiria em qualquer
   mundo novo. Precisa virar `cap_<mundo>_<index>`.
3. **A UI pressupõe uma campanha só.** `bossCaptured()`
   (`Player.kt:74`), `allScenariosCaptured()` (`Player.kt:71`) e a
   conquista "Colecionador" pressupõem um único chefe e uma conclusão
   única.

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
  não usadas.
