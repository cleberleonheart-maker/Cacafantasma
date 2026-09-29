package com.caca.fantasma.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.caca.fantasma.BuildConfig
import com.caca.fantasma.R
import com.caca.fantasma.game.Achievement
import com.caca.fantasma.game.Auth
import com.caca.fantasma.game.Consumable
import com.caca.fantasma.game.DownloadResult
import com.caca.fantasma.game.Equip
import com.caca.fantasma.game.GameData
import com.caca.fantasma.game.Player
import com.caca.fantasma.game.Scenario
import com.caca.fantasma.game.SoundManager
import com.caca.fantasma.game.UpdateChecker
import com.caca.fantasma.game.UpdateInfo
import com.caca.fantasma.game.UpdateResult
import org.json.JSONObject
import java.io.File
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var player: Player
    private lateinit var auth: Auth
    private lateinit var column: LinearLayout
    private lateinit var snd: SoundManager

    private var screen = "menu"

    private var huntScenario: Scenario? = null
    private var huntEquipped = mutableListOf<String>()
    private var huntActiveItems = mutableSetOf<String>()
    private var huntEvidence = 0
    private var huntAura = 0
    private var huntIntuition = false
    private var huntShield = false
    private var huntHp = 100
    private var huntTips = 0
    private var huntRound = 0
    private var huntPhase = "prep"
    private var huntResult = ""
    private var beatBossJustNow = false

    private var combatEquip: Equip? = null
    private var combatTiming = ""

    private val exportSave = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(::writeSaveTo)
    }

    private val importSave = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(::readSaveFrom)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = Player(this)
        auth = Auth(this)
        snd = SoundManager(this)
        column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(40))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.bg))
            isFillViewport = true
            addView(column, LinearLayout.LayoutParams(MATCH, WRAP))
        }
        setContentView(scroll)
        showScreen(if (auth.hasAccount()) "login" else "register")
    }

    override fun onDestroy() {
        snd.release()
        super.onDestroy()
    }

    private fun showScreen(s: String) {
        screen = s
        column.removeAllViews()
        when (s) {
            "menu" -> renderMenu()
            "scenarios" -> renderScenarios()
            "shop" -> renderShop()
            "help" -> renderHelp()
            "hunt" -> renderHunt()
            "trophies" -> renderTrophies()
            "achievements" -> renderAchievements()
            "settings" -> renderSettings()
            "ending" -> renderEnding()
            "login" -> renderLogin()
            "register" -> renderRegister()
            else -> renderMenu()
        }
        column.alpha = 0f
        column.translationY = dp(14).toFloat()
        column.animate().alpha(1f).translationY(0f).setDuration(300).start()
    }

    // ---------- helpers ----------

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun fontDisplay(): Typeface = resources.getFont(R.font.space_grotesk_bold)

    private fun fontBody(): Typeface = resources.getFont(R.font.outfit_regular)

    private fun fontMedium(): Typeface = resources.getFont(R.font.outfit_semibold)

    private fun color(id: Int): Int = ContextCompat.getColor(this, id)

    private fun starterKitDesc(): String {
        val equip = GameData.equipById(GameData.STARTER_EQUIP)?.name ?: "um equipamento"
        return "R\$${GameData.STARTER_MONEY} e uma $equip"
    }

    // ---------- settings / save backup ----------

    private fun renderSettings() {
        header(back = true)
        spacer(4)
        column.addView(Label("Configurações", 20, R.color.text_primary, fontDisplay()))
        spacer(4)
        pill("Caça Fantasma versão ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", R.color.accent)
        spacer(12)

        column.addView(Label("Salvar progresso", 18, R.color.accent, fontDisplay()))
        column.addView(Label("Gera um arquivo .json com todo o seu progresso: dinheiro, equipamento, consumíveis, capturas e conquistas. Guarde o arquivo no Google Drive ou em qualquer outro lugar — ele serve para restaurar em outro celular.", 13, R.color.text_secondary, fontBody()))
        spacer(2)
        addButton(ghostButton("Salvar em arquivo") { exportSave.launch(defaultSaveName()) }, 8)

        spacer(16)
        column.addView(Label("Restaurar progresso", 18, R.color.accent, fontDisplay()))
        column.addView(Label("Substitui o progresso atual pelo conteúdo de um arquivo .json salvo antes. A conta e a senha não são alteradas.", 13, R.color.text_secondary, fontBody()))
        spacer(2)
        addButton(ghostButton("Restaurar de arquivo") {
            if (player.totalCaptures == 0 && player.money <= GameData.STARTER_MONEY) {
                importSave.launch(arrayOf("application/json", "text/plain", "*/*"))
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Restaurar progresso?")
                    .setMessage("Todo o progresso atual (${player.totalCaptures} fantasmas, R\$${player.money}, equipamento e conquistas) será substituído pelo do arquivo. Não dá para desfazer.")
                    .setPositiveButton("Escolher arquivo") { _, _ -> importSave.launch(arrayOf("application/json", "text/plain", "*/*")) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }, 8)

        spacer(20)
        column.addView(Label("Sobre", 18, R.color.accent, fontDisplay()))
        spacer(2)
        addButton(ghostButton("Como jogar") { showScreen("help") }, 8)
        addButton(ghostButton("Verificar atualizações") { checkForUpdates() }, 8)

        spacer(20)
        addButton(ghostButton("← Voltar") { showScreen("menu") })
    }

    private fun defaultSaveName(): String {
        val who = auth.username().ifBlank { "cacador" }.replace(Regex("[^A-Za-z0-9-]"), "-")
        return "caca-fantasma-$who-v${BuildConfig.VERSION_NAME}.json"
    }

    private fun writeSaveTo(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(player.backupJson().toString(2).toByteArray())
            } ?: return Toast.makeText(this, "Não foi possível abrir o arquivo", Toast.LENGTH_LONG).show()
            snd.unlock()
            Toast.makeText(this, "Progresso salvo em $uri", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Falha ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun readSaveFrom(uri: Uri) {
        val raw = try {
            contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (e: Exception) {
            null
        } ?: return Toast.makeText(this, "Não foi possível ler o arquivo", Toast.LENGTH_LONG).show()

        val error = try {
            player.restoreJson(JSONObject(raw))
        } catch (e: Exception) {
            "o arquivo não parece um save válido do Caça Fantasma"
        }
        if (error != null) {
            AlertDialog.Builder(this)
                .setTitle("Arquivo inválido")
                .setMessage("Não foi possível restaurar: $error.")
                .setPositiveButton("Fechar", null)
                .show()
            return
        }
        snd.unlock()
        Toast.makeText(this, "Progresso restaurado!", Toast.LENGTH_LONG).show()
        showScreen("menu")
    }

    // ---------- updates ----------

    private fun checkForUpdates() {
        val progress = ProgressBar(this).apply {
            isIndeterminate = true
            indeterminateTintList = ColorStateList.valueOf(color(R.color.glow))
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Verificando atualizações")
            .setView(progress)
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.show()

        Thread {
            val result = UpdateChecker.check(BuildConfig.VERSION_NAME)
            runOnUiThread {
                if (dialog.isShowing) dialog.dismiss()
                when (result) {
                    is UpdateResult.Available -> showUpdateDialog(result.update)
                    is UpdateResult.UpToDate -> Toast.makeText(
                        this,
                        "Você já está na versão ${BuildConfig.VERSION_NAME}",
                        Toast.LENGTH_SHORT
                    ).show()
                    is UpdateResult.Failed -> AlertDialog.Builder(this)
                        .setTitle("Não foi possível verificar")
                        .setMessage("${result.reason}.\n\nVerifique a internet e tente de novo.")
                        .setPositiveButton("Fechar", null)
                        .setNeutralButton("Tentar de novo") { _, _ -> checkForUpdates() }
                        .show()
                }
            }
        }.start()
    }

    private fun showUpdateDialog(update: UpdateInfo) {
        val size = if (update.apkSize > 0) "  ·  ${update.apkSize / 1024 / 1024} MB" else ""
        val message = buildString {
            append("Versão ${update.versionName} disponível")
            append(" (você tem ${BuildConfig.VERSION_NAME}$size).")
            if (update.notes.isNotBlank()) append("\n\n${update.notes}")
            append("\n\nO app será baixado e aberto para instalar.")
        }
        AlertDialog.Builder(this)
            .setTitle("Nova versão disponível")
            .setMessage(message)
            .setPositiveButton("Atualizar") { _, _ -> downloadUpdate(update) }
            .setNegativeButton("Agora não", null)
            .show()
    }

    private fun downloadUpdate(update: UpdateInfo) {
        val status = Label("Baixando… 0%", 15, R.color.glow, fontMedium(), center = true)
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            progressTintList = ColorStateList.valueOf(color(R.color.glow))
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(18), dp(24), dp(6))
            addView(status, LinearLayout.LayoutParams(MATCH, WRAP))
            addView(bar, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) })
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Baixando ${update.versionName}")
            .setView(body)
            .setCancelable(false)
            .create()
        dialog.show()

        Thread {
            val result = UpdateChecker.download(update, File(cacheDir, "updates")) { percent ->
                runOnUiThread {
                    bar.progress = percent
                    status.text = "Baixando… $percent%"
                }
            }
            runOnUiThread {
                dialog.dismiss()
                when (result) {
                    is DownloadResult.Done -> promptInstall(result.file)
                    is DownloadResult.Failed -> AlertDialog.Builder(this)
                        .setTitle("Download falhou")
                        .setMessage("${result.reason}.")
                        .setPositiveButton("Tentar de novo") { _, _ -> downloadUpdate(update) }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
        }.start()
    }

    private fun promptInstall(file: File) {
        val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(install)
        } catch (e: Exception) {
            Toast.makeText(this, "Abra as configurações e permita instalar apps desconhecidos", Toast.LENGTH_LONG).show()
        }
    }

    private fun TextView.style(n: Int, c: Int, f: Typeface) {
        textSize = n.toFloat()
        setTextColor(c)
        typeface = f
        includeFontPadding = false
        setPadding(dp(2), dp(2), dp(2), dp(2))
    }

    private fun spacer(h: Int) {
        column.addView(View(this), LinearLayout.LayoutParams(MATCH, dp(h)))
    }

    private fun field(hint: String, password: Boolean = false, prefill: String = "", minLines: Int = 1): EditText {
        val et = EditText(this)
        et.hint = hint
        et.setTextColor(color(R.color.text_primary))
        et.setHintTextColor(color(R.color.text_muted))
        et.setTextSize(16f)
        et.typeface = fontBody()
        et.setBackgroundResource(R.drawable.bg_panel)
        et.setPadding(dp(14), dp(12), dp(14), dp(12))
        et.minLines = minLines
        et.textSize = 16f
        if (password) et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        if (prefill.isNotEmpty()) et.setText(prefill)
        column.addView(et, LinearLayout.LayoutParams(MATCH, dp(52)).apply { topMargin = dp(10) })
        return et
    }

    private fun Label(txt: String, n: Int = 16, c: Int = R.color.text_primary, f: Typeface? = null, center: Boolean = false): TextView {
        val tv = TextView(this)
        tv.style(n, color(c), f ?: fontBody())
        tv.text = txt
        if (center) tv.gravity = Gravity.CENTER
        return tv
    }

    private fun header(back: Boolean) {
        val h = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val title = Label("CAÇA FANTASMA", 24, R.color.glow, fontDisplay())
        val money = Label("R\$${player.money}", 15, R.color.gold, fontMedium())
        h.addView(title, LinearLayout.LayoutParams(0, WRAP, 1f))
        money.gravity = Gravity.CENTER_VERTICAL
        h.addView(money)
        column.addView(h)
        if (back) spacer(4)
    }

    private fun pill(txt: String, textColor: Int = R.color.text_secondary): TextView {
        val tv = Label(txt, 14, textColor, fontMedium(), center = true)
        tv.setBackgroundResource(R.drawable.bg_pill)
        tv.setPadding(dp(14), dp(8), dp(14), dp(8))
        column.addView(tv, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(6) })
        return tv
    }

    private fun panel(): LinearLayout {
        val p = LinearLayout(this)
        p.orientation = LinearLayout.VERTICAL
        p.setBackgroundResource(R.drawable.bg_panel)
        p.setPadding(dp(16), dp(14), dp(16), dp(14))
        return p
    }

    private fun addPanel(p: LinearLayout, w: Int = MATCH, h: Int = WRAP, marginTop: Int = 0) {
        val lp = LinearLayout.LayoutParams(w, h).apply { topMargin = dp(marginTop) }
        column.addView(p, lp)
    }

    private fun panelLine(p: LinearLayout, txt: String, n: Int = 14, c: Int = R.color.text_secondary, mt: Int = 4) {
        p.addView(Label(txt, n, c, fontBody()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(mt) })
    }

    private fun glowButton(txt: String, handler: (View) -> Unit): Button {
        val b = Button(this)
        b.text = txt
        b.textSize = 16f
        b.setTypeface(fontMedium())
        b.setTextColor(color(R.color.text_primary))
        b.setBackgroundResource(R.drawable.bg_btn_accent)
        b.setOnClickListener { snd.click(); handler(it) }
        return b
    }

    private fun ghostButton(txt: String, handler: (View) -> Unit): Button {
        val b = Button(this)
        b.text = txt
        b.textSize = 15f
        b.setTypeface(fontMedium())
        b.setTextColor(color(R.color.text_primary))
        b.setBackgroundResource(R.drawable.bg_btn)
        b.setOnClickListener { snd.click(); handler(it) }
        return b
    }

    private fun addButton(b: Button, marginTop: Int = 0) {
        column.addView(b, LinearLayout.LayoutParams(MATCH, dp(54)).apply { topMargin = dp(marginTop) })
    }

    private fun navRow(vararg buttons: Button) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        buttons.forEach { bt ->
            row.addView(bt, LinearLayout.LayoutParams(0, dp(54), 1f).apply { marginStart = if (bt === buttons[0]) 0 else dp(8) })
        }
        column.addView(row, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(16) })
    }

    private fun detailRow(k: String, v: String) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.addView(Label(k, 14, R.color.text_secondary, fontBody()), LinearLayout.LayoutParams(0, WRAP, 1f))
        row.addView(Label(v, 15, R.color.text_primary, fontMedium()))
        column.addView(row, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) })
    }

    private fun hpBar(value: Int, max: Int = 100, colorRes: Int = R.color.success): ProgressBar {
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        bar.max = max
        bar.progress = value.coerceIn(0, max)
        bar.progressTintList = ColorStateList.valueOf(color(colorRes))
        bar.progressBackgroundTintList = ColorStateList.valueOf(color(R.color.panel_border))
        return bar
    }

    private fun addHpBar(label: String, value: Int, max: Int = 100, colorRes: Int = R.color.success): ProgressBar {
        column.addView(Label(label, 13, R.color.text_secondary, fontMedium()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })
        val bar = hpBar(value, max, colorRes)
        column.addView(bar, LinearLayout.LayoutParams(MATCH, dp(14)).apply { topMargin = dp(4) })
        return bar
    }

    // ---------- login / register ----------

    private fun renderRegister() {
        spacer(24)
        val art = Label("▓▒░ CAÇA FANTASMA ░▒▓", 30, R.color.glow, fontDisplay(), center = true)
        column.addView(art)
        spacer(6)
        column.addView(Label("Crie seu caçador para guardar o progresso neste aparelho.", 14, R.color.text_secondary, fontBody(), center = true))
        spacer(18)
        column.addView(Label("Nome do caçador", 14, R.color.text_secondary, fontMedium()))
        val user = field("ex.: João Caçador")
        column.addView(Label("Senha (mínimo 4 caracteres)", 14, R.color.text_secondary, fontMedium()))
        val pass = field("senha secreta", password = true)
        val conf = field("repita a senha", password = true)
        spacer(10)
        addButton(glowButton("Criar conta e jogar") {
            val u = user.text.toString().trim()
            val p = pass.text.toString()
            val c = conf.text.toString()
            when {
                u.isEmpty() -> Toast.makeText(this, "Escolha um nome para o caçador", Toast.LENGTH_SHORT).show()
                u.length < 3 -> Toast.makeText(this, "O nome precisa de ao menos 3 letras", Toast.LENGTH_SHORT).show()
                p.length < 4 -> Toast.makeText(this, "A senha precisa de ao menos 4 caracteres", Toast.LENGTH_SHORT).show()
                p != c -> Toast.makeText(this, "As senhas não conferem", Toast.LENGTH_SHORT).show()
                else -> {
                    auth.register(u, p)
                    snd.unlock()
                    Toast.makeText(this, "Conta criada. Boa caçada, ${u}!", Toast.LENGTH_LONG).show()
                    showScreen("menu")
                }
            }
        })
    }

    private fun renderLogin() {
        spacer(24)
        val art = Label("▓▒░ CAÇA FANTASMA ░▒▓", 30, R.color.glow, fontDisplay(), center = true)
        column.addView(art)
        spacer(6)
        column.addView(Label("Entre para continuar sua caçada.", 14, R.color.text_secondary, fontBody(), center = true))
        spacer(18)
        column.addView(Label("Nome do caçador", 14, R.color.text_secondary, fontMedium()))
        val user = field("seu nome", prefill = auth.username())
        column.addView(Label("Senha", 14, R.color.text_secondary, fontMedium()))
        val pass = field("sua senha", password = true)
        spacer(10)
        addButton(glowButton("Entrar") {
            val u = user.text.toString().trim()
            val p = pass.text.toString()
            if (auth.login(u, p)) {
                snd.unlock()
                showScreen("menu")
            } else {
                snd.defeat()
                Toast.makeText(this, "Usuário ou senha incorretos", Toast.LENGTH_SHORT).show()
            }
        })
        spacer(16)
        val forgot = Label("Apagar conta e começar de novo", 13, R.color.text_muted, fontBody(), center = true)
        column.addView(forgot, LinearLayout.LayoutParams(MATCH, WRAP))
        forgot.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Apagar tudo?")
                .setMessage("Sua conta e todo o progresso serão perdidos. A nova conta começa com ${starterKitDesc()}.")
                .setPositiveButton("Apagar tudo") { _, _ ->
                    auth.wipe()
                    player.reset()
                    showScreen("register")
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    // ---------- menu ----------

    private fun renderMenu() {
        player.ensureStarterKit()
        header(back = false)
        spacer(10)
        val art = Label("▓▒░ CAÇA FANTASMA ░▒▓", 34, R.color.glow, fontDisplay(), center = true)
        column.addView(art)
        spacer(2)
        val sub = Label("Caçador de espíritos. Equipamento, cenários e glória.", 15, R.color.text_secondary, fontBody(), center = true)
        column.addView(sub)
        spacer(12)
        pill("Posto: ${player.rank()}  ·  ${player.totalCaptures} fantasmas presos   ·  R\$${player.money}")
        pill("Bônus de posto: +${(player.rankData().rewardBonus * 100).toInt()}% prêmio  ·  -${player.rankData().shopDiscount}% na loja", R.color.accent)
        if (player.bossCaptured()) {
            pill("✦ A caçada foi concluída — o castelo é seu", R.color.gold)
        }
        spacer(18)
        addButton(glowButton("Cenários de Caçada") { showScreen("scenarios") })
        addButton(ghostButton("Loja de Equipamento") { showScreen("shop") }, 10)
        addButton(ghostButton("Sala de Troféus") { showScreen("trophies") }, 10)
        addButton(ghostButton("Conquistas") { showScreen("achievements") }, 10)
        if (player.bossCaptured()) {
            addButton(ghostButton("✦ Final da caçada") { showScreen("ending") }, 10)
        }
        addButton(ghostButton("Configurações") { showScreen("settings") }, 10)
        addButton(ghostButton("Sair da conta") { showScreen("login") }, 10)
        addButton(ghostButton("Zerar progresso") {
            AlertDialog.Builder(this)
                .setTitle("Reiniciar caçada?")
                .setMessage("Todo o progresso, dinheiro e equipamento serão perdidos. A caçada recomeça com ${starterKitDesc()}.")
                .setPositiveButton("Sim, zerar") { _, _ -> player.reset(); showScreen("menu") }
                .setNegativeButton("Cancelar", null)
                .show()
        }, 24)
    }

    // ---------- scenarios ----------

    private fun renderScenarios() {
        header(back = true)
        spacer(4)
        column.addView(Label("Cenários de Caçada", 20, R.color.text_primary, fontDisplay()))
        column.addView(Label("Cada local guarda um fantasma diferente.", 14, R.color.text_secondary, fontBody()))
        spacer(4)
        GameData.SCENARIOS.forEach { sc ->
            val unlocked = sc.index <= player.maxUnlocked
            val isBoss = sc.isBoss
            val p = panel()
            p.addView(Label((if (isBoss) "CHEFE — " else "") + sc.name, if (isBoss) 19 else 18, if (isBoss) R.color.gold else R.color.accent, fontDisplay()))
            val ghost = Label("∞ ${sc.ghost}   ·   dificuldade ${"★".repeat(sc.diff)}", 14, if (isBoss) R.color.danger else R.color.glow, fontMedium())
            p.addView(ghost, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(4) })
            p.addView(Label(sc.local, 13, R.color.text_muted, fontBody()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(2) })
            p.addView(Label("Prêmio R\$${sc.baseReward}  ·  ${player.capturesOf(sc.index)} preso(s)", 15, R.color.gold, fontMedium()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })
            if (isBoss) p.addView(Label("Domina todos os espíritos dos cenários anteriores.", 13, R.color.danger, fontBody()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(2) })
            addPanel(p, marginTop = 12)
            if (unlocked) {
                addButton(glowButton("Caçar — ${sc.name}") { startHunt(sc.index) }, 8)
            } else {
                pill(if (isBoss) "Bloqueado — capture todos os cenários para enfrentar o chefe" else "Bloqueado — capture o cenário anterior para liberar", R.color.text_muted)
            }
        }
        spacer(8)
        addButton(ghostButton("← Voltar") { showScreen("menu") })
    }

    // ---------- shop ----------

    private fun renderShop() {
        header(back = true)
        spacer(4)
        val discount = player.rankData().shopDiscount
        column.addView(Label("Loja de Equipamento", 20, R.color.text_primary, fontDisplay()))
        column.addView(
            Label(if (discount > 0) "Desconto do posto aplicado: -$discount%." else "Compre e melhore seu arsenal.", 14, R.color.text_secondary, fontBody())
        )
        spacer(4)
        GameData.EQUIP.forEach { e ->
            val level = player.levelOf(e.id)
            val unlocked = player.equipUnlocked(e)
            val p = panel()
            p.addView(Label(e.name, 17, R.color.text_primary, fontMedium()))
            p.addView(Label(e.desc, 13, R.color.text_secondary, fontBody()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(2) })
            val status = if (level == 0) "Não possui" else "Nível $level de 3"
            val cost = if (level == 0) e.price else e.upgradePrice * level
            val priceOut = player.discountedPrice(cost)
            p.addView(Label("$status  ·  R\$$priceOut", 15, R.color.gold, fontMedium()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })
            addPanel(p, marginTop = 12)
            if (!unlocked) {
                val req = GameData.SCENARIOS.getOrNull(e.unlockAfter)
                pill("Libera após capturar: ${req?.name ?: "???"}", R.color.text_muted)
            } else if (level >= 3) {
                pill("Nível máximo", R.color.success)
            } else {
                val label = if (level == 0) "Comprar  R\$$priceOut" else "Melhorar para Nv ${level + 1}  R\$$priceOut"
                val canBuy = player.money >= priceOut
                val b = ghostButton(label) {
                    if (player.money >= priceOut) {
                        player.money = player.money - priceOut
                        player.totalSpent = player.totalSpent + priceOut
                        player.setLevel(e.id, level + 1)
                        snd.coin()
                        Toast.makeText(this, "${e.name} ${if (level == 0) "adquirido" else "melhorado"}!", Toast.LENGTH_SHORT).show()
                        checkAchievements(null, false)
                        renderShop()
                    } else {
                        Toast.makeText(this, "Dinheiro insuficiente", Toast.LENGTH_SHORT).show()
                    }
                }
                if (!canBuy) {
                    b.isEnabled = false
                    b.setAlpha(0.45f)
                }
                addButton(b, 8)
            }
        }

        spacer(14)
        column.addView(Label("Consumíveis", 18, R.color.accent, fontDisplay()))
        column.addView(Label("Compre itens de uso único e ative-os na preparação.", 13, R.color.text_secondary, fontBody()))
        spacer(2)
        GameData.CONSUMABLES.forEach { c ->
            val count = player.itemCount(c.id)
            val priceOut = player.discountedPrice(c.price)
            val p = panel()
            p.addView(Label(c.name, 16, R.color.text_primary, fontMedium()))
            p.addView(Label(c.desc, 13, R.color.text_secondary, fontBody()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(2) })
            p.addView(Label("Em estoque: $count  ·  R\$$priceOut cada", 14, R.color.gold, fontMedium()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(6) })
            addPanel(p, marginTop = 10)
            val canBuy = player.money >= priceOut
            val b = ghostButton("Comprar +1  R\$$priceOut") {
                if (player.money >= priceOut) {
                    player.money = player.money - priceOut
                    player.totalSpent = player.totalSpent + priceOut
                    player.addItem(c.id, 1)
                    snd.coin()
                    checkAchievements(null, false)
                    renderShop()
                } else {
                    Toast.makeText(this, "Dinheiro insuficiente", Toast.LENGTH_SHORT).show()
                }
            }
            if (!canBuy) {
                b.isEnabled = false
                b.setAlpha(0.45f)
            }
            addButton(b, 6)
        }
        spacer(8)
        addButton(ghostButton("← Voltar") { showScreen("menu") })
    }

    // ---------- help ----------

    private fun renderHelp() {
        header(back = true)
        spacer(4)
        column.addView(Label("Como jogar", 20, R.color.text_primary, fontDisplay()))
        spacer(4)
        val help = panel()
        panelLine(help, "1. Escolha um cenário desbloqueado. O chefe final só abre após capturar todos.", 15, R.color.text_primary, 0)
        panelLine(help, "2. Leve até 3 equipamentos e ative consumíveis — cada local tem pistas que só um aparelho revela.")
        panelLine(help, "3. Investigue as áreas: mesmo com o aparelho certo, a chance de achar a pista é ~85%. Aparelhos de nível mais alto acham mais fácil.")
        panelLine(help, "4. Errar uma vistoria gasta sua coragem (HP). HP baixo enfraquece a caçada no confronto.")
        panelLine(help, "5. No confronto, escolha o equipamento E o momento: aguardar é seguro, atacar de frente dá +15% de chance mas sem proteção, e o ritual dá prêmio ×1,5 com -10% de chance.")
        panelLine(help, "6. A Armadilha captura melhor, o Sal enfraquece, o Crucifixo te protege se falhar.")
        panelLine(help, "7. Capture o chefe para coletar todos os troféus e a conquista de Colecionador.")
        panelLine(help, "8. Postos mais altos pagam mais prêmio e dão desconto na loja.")
        panelLine(help, "9. Conquistas premiadas com dinheiro ficam na Sala de Troféus.", 15, R.color.glow)
        addPanel(help)
        spacer(8)
        addButton(glowButton("Entendi") { showScreen("menu") })
    }

    // ---------- trophies ----------

    private fun renderTrophies() {
        header(back = true)
        spacer(4)
        val owned = GameData.SCENARIOS.count { player.capturesOf(it.index) > 0 }
        column.addView(Label("Sala de Troféus", 20, R.color.text_primary, fontDisplay()))
        column.addView(Label("$owned de ${GameData.SCENARIOS.size} espíritos contidos.", 14, R.color.text_secondary, fontBody()))
        spacer(4)
        GameData.SCENARIOS.forEach { sc ->
            val captures = player.capturesOf(sc.index)
            val got = captures > 0
            val p = panel()
            if (got) {
                p.addView(Label((if (sc.isBoss) "⚔ " else "") + sc.ghost, 17, if (sc.isBoss) R.color.gold else R.color.accent, fontDisplay()))
                p.addView(Label(sc.name + " — preso $captures vez(es)", 13, R.color.text_secondary, fontBody()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(2) })
            } else {
                p.addView(Label("???", 17, R.color.text_muted, fontDisplay()))
                p.addView(Label("Espírito ainda à solta.", 13, R.color.text_muted, fontBody()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(2) })
            }
            if (sc.isBoss) p.addView(Label(if (got) "Espírito supremo capturado." else "Só os melhores o encaram.", 12, R.color.text_muted, fontBody()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(4) })
            addPanel(p, marginTop = 10)
        }
        spacer(10)
        column.addView(Label("Conquistas", 18, R.color.accent, fontDisplay()))
        spacer(2)
        GameData.ACHIEVEMENTS.forEach { a ->
            val ok = player.achUnlocked(a.id)
            val p = panel()
            p.addView(Label((if (ok) "✓ " else "· ") + a.name, 16, if (ok) R.color.gold else R.color.text_muted, fontMedium()))
            p.addView(Label(a.desc, 13, R.color.text_secondary, fontBody()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(2) })
            p.addView(Label(if (ok) "Concluída (+R\$${a.reward})" else "Recompensa: R\$${a.reward}", 13, if (ok) R.color.success else R.color.text_muted, fontMedium()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(6) })
            addPanel(p, marginTop = 8)
        }
        spacer(8)
        addButton(ghostButton("← Voltar") { showScreen("menu") })
    }

    // ---------- achievements ----------

    private fun renderAchievements() {
        header(back = true)
        spacer(4)
        val done = GameData.ACHIEVEMENTS.count { player.achUnlocked(it.id) }
        column.addView(Label("Conquistas", 20, R.color.text_primary, fontDisplay()))
        column.addView(Label("$done de ${GameData.ACHIEVEMENTS.size} conquistadas.", 14, R.color.text_secondary, fontBody()))
        spacer(4)
        GameData.ACHIEVEMENTS.forEach { a ->
            val ok = player.achUnlocked(a.id)
            val p = panel()
            p.addView(Label((if (ok) "✓ " else "· ") + a.name, 17, if (ok) R.color.gold else R.color.text_primary, fontMedium()))
            p.addView(Label(a.desc, 14, R.color.text_secondary, fontBody()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(2) })
            p.addView(Label(if (ok) "Concluída (+R\$${a.reward})" else "Recompensa: R\$${a.reward}", 14, if (ok) R.color.success else R.color.text_muted, fontMedium()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(6) })
            addPanel(p, marginTop = 10)
        }
        spacer(8)
        addButton(ghostButton("← Voltar") { showScreen("menu") })
    }

    // ---------- hunt ----------

    private fun startHunt(index: Int) {
        huntScenario = GameData.SCENARIOS[index]
        huntEquipped.clear()
        huntActiveItems.clear()
        huntEvidence = 0
        huntAura = 0
        huntIntuition = false
        huntShield = false
        huntHp = 100
        huntTips = 0
        huntRound = 0
        huntPhase = "prep"
        combatEquip = null
        combatTiming = ""
        beatBossJustNow = false
        showScreen("hunt")
    }

    private fun renderHunt() {
        header(back = false)
        spacer(4)
        val sc = huntScenario ?: return showScreen("scenarios")
        when (huntPhase) {
            "prep" -> renderPrep(sc)
            "round" -> renderRound(sc)
            "confront" -> renderConfront(sc)
            "timing" -> renderTiming(sc)
            "result" -> renderResult(sc)
        }
    }

    private fun effectiveEvidence(): Int = huntEvidence + huntAura

    private fun itemActive(c: Consumable): Boolean = huntActiveItems.contains(c.id)

    private fun activateItem(c: Consumable) {
        if (!player.useItem(c.id)) return
        huntActiveItems.add(c.id)
        snd.coin()
        when (c.effect) {
            "intuition" -> { huntIntuition = true; Toast.makeText(this, "Pilha ativada: próxima pista é certa!", Toast.LENGTH_SHORT).show() }
            "shield" -> { huntShield = true; Toast.makeText(this, "Amuleto ativado: proteção no confronto.", Toast.LENGTH_SHORT).show() }
            "aura" -> { huntAura++; Toast.makeText(this, "Incenso queimando: +1 evidência no confronto.", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun renderPrep(sc: Scenario) {
        column.addView(Label(if (sc.isBoss) "A GRANDE CAÇADA" else "Preparação", 20, R.color.text_primary, fontDisplay()))
        column.addView(Label(sc.name, 16, R.color.accent, fontDisplay()))
        column.addView(Label(sc.local, 13, R.color.text_muted, fontBody()))
        spacer(4)
        val p = panel()
        p.addView(Label(sc.intro, 15, R.color.text_secondary, fontBody()))
        addPanel(p)
        spacer(4)
        column.addView(Label("Leve até 3 equipamentos:", 15, R.color.text_primary, fontMedium()))
        spacer(2)
        GameData.EQUIP.filter { player.owns(it.id) }.forEach { e ->
            val selected = huntEquipped.contains(e.id)
            val b = ghostButton("${if (selected) "✓ " else ""}${e.name}  (Nv ${player.levelOf(e.id)})") {
                if (selected) huntEquipped.remove(e.id)
                else {
                    if (huntEquipped.size >= 3) {
                        Toast.makeText(this, "Máximo de 3 equipamentos", Toast.LENGTH_SHORT).show()
                        return@ghostButton
                    }
                    huntEquipped.add(e.id)
                }
                renderHunt()
            }
            if (selected) {
                b.setBackgroundResource(R.drawable.bg_btn_accent)
                b.setAlpha(1f)
            }
            addButton(b, 6)
        }
        if (huntEquipped.isEmpty()) {
            pill(
                if (GameData.EQUIP.none { player.owns(it.id) })
                    "Sem equipamento — compre na loja para poder caçar"
                else
                    "Escolha pelo menos um equipamento",
                R.color.text_muted
            )
        }

        spacer(8)
        column.addView(Label("Consumíveis:", 15, R.color.text_primary, fontMedium()))
        spacer(2)
        var hadItem = false
        GameData.CONSUMABLES.forEach { c ->
            val count = player.itemCount(c.id)
            if (count > 0) {
                hadItem = true
                val active = itemActive(c)
                val b = ghostButton("%s%s  (%d restante%s)  [%s]".format(
                    c.name, if (active) " ✓" else "", count, if (count == 1) "" else "s",
                    when (c.effect) {
                        "intuition" -> if (huntIntuition) "próxima pista certa" else "ativa"
                        "shield" -> if (huntShield) "protegido" else "ativa"
                        else -> "aura +1"
                    }
                )) {
                    if (!active) activateItem(c) else Toast.makeText(this, "Já ativo nesta caçada", Toast.LENGTH_SHORT).show()
                    renderHunt()
                }
                if (active) {
                    b.setBackgroundResource(R.drawable.bg_btn_accent)
                    b.setAlpha(1f)
                }
                addButton(b, 6)
            }
        }
        if (!hadItem) {
            pill("Sem consumíveis — compre na loja", R.color.text_muted)
        }

        addButton(glowButton("Começar a investigar") {
            if (huntEquipped.isEmpty()) {
                Toast.makeText(this, "Escolha pelo menos um equipamento", Toast.LENGTH_SHORT).show()
                return@glowButton
            }
            huntPhase = "round"
            huntRound = 0
            renderHunt()
        }, 14)
        addButton(ghostButton("Voltar") { showScreen("scenarios") })
    }

    private fun hitChance(e: Equip): Float = (0.80f + 0.07f * (player.levelOf(e.id) - 1)).coerceIn(0f, 0.97f)

    private fun renderRound(sc: Scenario) {
        val round = sc.rounds.getOrNull(huntRound) ?: run {
            huntPhase = "confront"
            renderHunt()
            return
        }
        column.addView(Label("Investigação  ${huntRound + 1} de ${sc.rounds.size}", 20, R.color.text_primary, fontDisplay()))
        column.addView(Label("Evidências: ${effectiveEvidence()}", 15, R.color.glow, fontMedium()))
        addHpBar("Coragem: $huntHp/100", huntHp, 100, if (huntHp > 40) R.color.success else R.color.danger)
        spacer(2)
        if (huntIntuition) pill("Pilha de Ecto-Lítio: a próxima vistoria é pista certa", R.color.accent)
        spacer(2)
        val p = panel()
        p.addView(Label("Você está em: ${round.spot}", 16, R.color.text_primary, fontMedium()))
        p.addView(Label("Use um equipamento para varrer o local.", 14, R.color.text_secondary, fontBody()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(4) })
        addPanel(p)
        spacer(4)
        huntEquipped.forEach { id ->
            val e = GameData.equipById(id) ?: return@forEach
            addButton(ghostButton("${e.name}  (Nv ${player.levelOf(e.id)})") { doRound(sc, e) }, 6)
        }
        addButton(ghostButton("Pular a vistoria") { skipRound(sc) }, 8)
    }

    private fun doRound(sc: Scenario, e: Equip) {
        val round = sc.rounds.getOrNull(huntRound) ?: return nextRound()
        var found = huntIntuition || (round.ideal == e.id && Random.nextFloat() < hitChance(e))
        if (found) {
            huntEvidence++
            huntTips += sc.clueTip
            player.money = player.money + sc.clueTip
            huntResult = if (huntIntuition && round.ideal != e.id)
                "PILHA REVELADORA — você sentiu a pista exata!  (+R\$${sc.clueTip})"
            else
                "PISTA ENCONTRADA — " + round.hit + "  (+R\$${sc.clueTip} de achado)"
            snd.clue()
        } else {
            huntHp = (huntHp - 12).coerceAtLeast(10)
            huntResult = if (round.ideal == e.id)
                "Quase... o sinal sumiu antes de você confirmar. -12 de coragem."
            else
                "Nenhuma atividade... você varreu e não achou nada. -12 de coragem."
            snd.hurt()
        }
        huntIntuition = false
        nextRound()
    }

    private fun skipRound(sc: Scenario) {
        huntHp = (huntHp - 6).coerceAtLeast(10)
        huntResult = "Você pulou a vistoria. -6 de coragem."
        snd.hurt()
        nextRound()
    }

    private fun nextRound() {
        if (huntRound + 1 < huntScenario!!.rounds.size) {
            huntRound++
            renderHunt()
        } else {
            combatEquip = null
            combatTiming = ""
            huntPhase = "confront"
            renderHunt()
        }
    }

    private fun needEvidence(sc: Scenario): Int = when (sc.diff) {
        1, 2 -> 1
        3, 4 -> 2
        5, 6 -> 3
        else -> 4
    }

    private fun catchChance(sc: Scenario, e: Equip, timing: String): Float {
        val lv = player.levelOf(e.id)
        var c = 0.28f + 0.09f * effectiveEvidence().coerceAtMost(sc.rounds.size)
        when (e.id) {
            "trap" -> c += 0.22f
            "salt" -> c += 0.10f
            "cross" -> c += 0.05f
        }
        c += 0.05f * (lv - 1)
        if (huntHp < 40) c -= 0.10f
        when (timing) {
            "sinal" -> c += 0.05f
            "frente" -> c += 0.15f
            "ritual" -> c -= 0.10f
        }
        return c.coerceIn(0.05f, 0.95f)
    }

    private fun renderConfront(sc: Scenario) {
        val eff = effectiveEvidence()
        column.addView(Label(if (sc.isBoss) "O CONFRONTO FINAL" else "CONFRONTO", 22, R.color.danger, fontDisplay()))
        column.addView(Label("O fantasma se materializou. ".toUpperCase() + sc.ghost + " está na sua frente.", 15, R.color.text_primary, fontBody()))
        spacer(2)
        addHpBar("Coragem: $huntHp/100", huntHp, 100, if (huntHp > 40) R.color.success else R.color.danger)
        spacer(2)
        val p = panel()
        p.addView(Label("Evidências reunidas: $eff de ${sc.rounds.size}  (recomendado: ${needEvidence(sc)})", 16, R.color.glow, fontMedium()))
        if (huntAura > 0) p.addView(Label("Incluindo $huntAura do Incenso de Aura.", 13, R.color.text_muted, fontBody()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(2) })
        p.addView(Label("Escolha o equipamento do confronto final.", 14, R.color.text_secondary, fontBody()), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(4) })
        addPanel(p)
        spacer(4)
        huntEquipped.forEach { id ->
            val e = GameData.equipById(id) ?: return@forEach
            addButton(glowButton("Usar ${e.name}  (Nv ${player.levelOf(e.id)})") {
                combatEquip = e
                huntPhase = "timing"
                renderHunt()
            }, 8)
        }
        if (huntEquipped.isEmpty()) {
            pill("Você não levou equipamento. Fuja!", R.color.danger)
        }
    }

    private fun renderTiming(sc: Scenario) {
        val e = combatEquip ?: return run { huntPhase = "confront"; renderHunt() }
        column.addView(Label("O MOMENTO DO GOLPE", 20, R.color.danger, fontDisplay()))
        column.addView(Label("${e.name} na mão. Escolha como golpear.", 15, R.color.text_primary, fontBody()))
        spacer(2)
        val p = panel()
        p.addView(Label("Chance estimada: ${(catchChance(sc, e, "sinal") * 100).toInt()}% (aguardando) / ${(catchChance(sc, e, "frente") * 100).toInt()}% (frente)", 15, R.color.glow, fontMedium()))
        addPanel(p)
        spacer(4)
        val options = listOf(
            Triple("sinal", "Aguardar o sinal", "Chance estável, +5% e proteções funcionam."),
            Triple("frente", "Atacar de frente", "+15% de chance, mas sem proteção se falhar."),
            Triple("ritual", "Usar o ritual", "-10% de chance, mas prêmio ×1,5 se vencer.")
        )
        options.forEach { (key, name, desc) ->
            addButton(ghostButton(name) { doConfront(sc, e, key) }, 8)
            val d = Label(desc, 13, R.color.text_muted, fontBody(), center = true)
            column.addView(d)
        }
        addButton(ghostButton("Recuar") { huntPhase = "confront"; combatEquip = null; renderHunt() }, 12)
    }

    private fun doConfront(sc: Scenario, e: Equip, timing: String) {
        val chance = catchChance(sc, e, timing)
        val eff = effectiveEvidence()
        val won = Random.nextFloat() < chance

        if (won) {
            var reward = sc.baseReward
            val evBonus = (sc.baseReward * 0.3f * huntEvidence).toInt()
            reward += evBonus
            val prePost = reward
            if (timing == "ritual") reward = (reward * 1.5f).toInt()
            val finalReward = player.rewardMultiplier(reward)

            val wasLast = (sc.index == player.maxUnlocked)
            player.addCapture(sc.index, finalReward)
            if (wasLast && !sc.isBoss) snd.unlock() else snd.victory()

            var msg = "VITÓRIA — ${sc.victory}\n\n"
            msg += "Prêmio R\$$finalReward (base R\$${sc.baseReward}"
            if (evBonus > 0) msg += " + R\$$evBonus de evidência"
            if (timing == "ritual") msg += " + ritual ×1,5"
            if (player.rankData().rewardBonus > 0) msg += " + R\$${finalReward - reward} do posto ${player.rank()}"
            msg += ")."
            if (wasLast && !sc.isBoss) msg += "\n\nNOVO CENÁRIO DESBLOQUEADO!"
            if (sc.isBoss) msg += "\n\n⚔ CHEFE DERROTADO — O castelo é seu."
            val newEquip = GameData.EQUIP.filter { it.unlockAfter == sc.index && !player.owns(it.id) }
            if (newEquip.isNotEmpty()) msg += "\n\nEquipamento liberado na loja: ${newEquip.joinToString(", ") { it.name }}."
            huntResult = msg
            beatBossJustNow = sc.isBoss
            checkAchievements(sc, huntEvidence == sc.rounds.size)
        } else {
            val protected = timing != "frente" && (huntShield || e.id == "cross")
            beatBossJustNow = false
            if (protected) {
                huntResult = "ESCAPOU — ${sc.defeat}\n\nO Crucifixo/Amuleto te protegeu: você ficou com os achados (+R\$$huntTips)."
                snd.hurt()
            } else {
                player.money = player.money - huntTips
                huntResult = "DERROTA — ${sc.defeat}\n\nO fantasma te aterrorizou e você perdeu os achados (-R\$$huntTips)."
                if (timing == "frente") huntResult += "\nArriscar demais anulou todas as proteções."
                snd.defeat()
            }
        }
        huntPhase = "result"
        renderHunt()
    }

    private fun renderResult(sc: Scenario) {
        column.addView(Label("Fim da caçada", 20, R.color.text_primary, fontDisplay()))
        spacer(2)
        val p = panel()
        p.addView(Label(huntResult, 15, R.color.text_primary, fontBody()))
        addPanel(p)
        spacer(2)
        pill("Bolsos atuais: R\$${player.money}   ·   Posto: ${player.rank()}", R.color.gold)
        spacer(4)
        navRow(
            glowButton("Loja") { showScreen("shop") },
            ghostButton("Cenários") { showScreen("scenarios") }
        )
        val ok = huntResult.startsWith("VITÓRIA")
        if (!ok) {
            addButton(ghostButton("Tentar de novo") { startHunt(sc.index) })
        } else if (beatBossJustNow) {
            addButton(glowButton("Ver o final da caçada") { showScreen("ending") })
        }
    }

    // ---------- ending ----------

    private fun renderEnding() {
        header(back = false)
        spacer(18)
        column.addView(Label("▓▒░ ✦ ░▒▓", 26, R.color.gold, fontDisplay(), center = true))
        spacer(2)
        column.addView(Label(GameData.ENDING_TITLE, 28, R.color.glow, fontDisplay(), center = true))
        spacer(14)

        val story = panel()
        story.addView(Label(GameData.ENDING_TEXT, 15, R.color.text_primary, fontBody()))
        addPanel(story)

        spacer(16)
        column.addView(Label("SEU LEGADO", 18, R.color.accent, fontDisplay()))
        spacer(2)

        val stats = panel()
        panelLine(stats, "Espíritos contidos:  ${GameData.SCENARIOS.size} de ${GameData.SCENARIOS.size}", 15, R.color.gold, mt = 0)
        panelLine(stats, "Fantasmas presos:  ${player.totalCaptures}")
        panelLine(stats, "Posto alcançado:  ${player.rank()}")
        panelLine(stats, "Conquistas:  ${player.achievementsUnlocked()} de ${GameData.ACHIEVEMENTS.size}")
        panelLine(stats, "Equipamentos:  ${GameData.EQUIP.count { player.owns(it.id) }} de ${GameData.EQUIP.size}")
        panelLine(stats, "Bolsos:  R\$${player.money}")
        addPanel(stats, marginTop = 8)

        val pending = GameData.ACHIEVEMENTS.count { !player.achUnlocked(it.id) }
        if (pending > 0) {
            pill("Faltam $pending conquistas — continue caçando para completar tudo", R.color.text_muted)
        } else {
            pill("Todas as conquistas desbloqueadas. Nada mais resta nesta cidade.", R.color.success)
        }

        spacer(14)
        navRow(
            glowButton("Sala de Troféus") { showScreen("trophies") },
            ghostButton("Menu") { showScreen("menu") }
        )
    }

    // ---------- achievements engine ----------

    private fun grantAch(a: Achievement) {
        if (player.achUnlocked(a.id)) return
        player.unlockAch(a.id)
        player.money = player.money + a.reward
        snd.unlock()
        Toast.makeText(this, "Conquista: ${a.name}  (+R\$${a.reward})", Toast.LENGTH_LONG).show()
    }

    private fun checkAchievements(sc: Scenario?, perfect: Boolean) {
        if (sc?.isBoss == true) grantAch(GameData.achievementById("boss")!!)
        if (perfect) grantAch(GameData.achievementById("perfect")!!)
        if (player.totalCaptures >= 1) grantAch(GameData.achievementById("first")!!)
        if (player.totalCaptures >= 5) grantAch(GameData.achievementById("five")!!)
        if (player.totalCaptures >= 15) grantAch(GameData.achievementById("master")!!)
        if (player.money >= 3000) grantAch(GameData.achievementById("rich")!!)
        if (player.allScenariosCaptured()) grantAch(GameData.achievementById("collector")!!)
        if (player.totalSpent >= 2000) grantAch(GameData.achievementById("spender")!!)
    }

    companion object {
        private const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
        private const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
    }
}