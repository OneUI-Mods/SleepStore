package dev.ryuya.sleepstore

import android.app.LocaleManager
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.LocaleList
import android.view.View
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.widget.TextViewCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlin.jvm.JvmStatic
import kotlin.math.round

class MainActivity : AppCompatActivity() {
    private var content: LinearLayout? = null

    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val root = LinearLayout(this)
        root.setOrientation(LinearLayout.VERTICAL)
        val surface =
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorSurface,
                javaClass.getName(),
            )
        root.setBackgroundColor(surface)
        val toolbar = MaterialToolbar(this)
        toolbar.setTitle(R.string.app_name)
        root.addView(toolbar, LayoutParams(-1, dp(64)))
        val scroll = ScrollView(this)
        scroll.setFillViewport(true)
        content = LinearLayout(this)
        content!!.setOrientation(LinearLayout.VERTICAL)
        content!!.setPadding(dp(20), dp(4), dp(20), dp(24))
        scroll.addView(content)
        root.addView(scroll, LayoutParams(-1, 0, 1f))
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(
            root,
            OnApplyWindowInsetsListener { view: View?, insets: WindowInsetsCompat? ->
                val bars = insets!!.getInsets(Type.systemBars() or Type.displayCutout())
                view!!.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets!!
            },
        )
        val protection = card()
        text(protection, getString(R.string.protection_title), 20, true, 8)
        text(protection, getString(R.string.protection_body), 16, false, 0)
        val integration = card()
        text(integration, getString(R.string.setup_title), 20, true, 8)
        text(
            integration,
            getString(
                if (MainActivity.frameworkLoaded()) R.string.framework_active
                else R.string.framework_inactive
            ),
            16,
            true,
            6,
        )
        text(integration, getString(R.string.framework_hint), 14, false, 12)
        text(integration, getString(R.string.setup_body), 16, false, 12)
        for (i in Targets.PACKAGES!!.indices) {
            val installed = Targets.installedPackage(this, i)
            if (installed != null) store(integration, installed, Targets.LABELS[i])
        }
        text(integration, getString(R.string.scope_notice), 14, false, 0)
        val about = card()
        text(about, getString(R.string.about_title), 20, true, 8)
        text(about, getString(R.string.version, BuildConfig.VERSION_NAME), 14, false, 8)
        button(about, getString(R.string.language), Runnable { this.language() })
        button(
            about,
            getString(R.string.source_code),
            Runnable { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MainActivity.SOURCE))) },
        )
        button(about, getString(R.string.licenses), Runnable { this.licenses() })
    }

    private fun card(): LinearLayout {
        val card = MaterialCardView(this)
        card.setRadius(dp(24).toFloat())
        card.setStrokeWidth(0)
        card.setCardElevation(0f)
        card.setCardBackgroundColor(
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorSurfaceContainer,
                javaClass.getName(),
            )
        )
        val p = LayoutParams(-1, -2)
        p.bottomMargin = dp(16)
        content!!.addView(card, p)
        val inner = LinearLayout(this)
        inner.setOrientation(LinearLayout.VERTICAL)
        inner.setPadding(dp(20), dp(20), dp(20), dp(20))
        card.addView(inner)
        return inner
    }

    private fun text(parent: LinearLayout, value: String?, size: Int, title: Boolean, bottom: Int) {
        val text = TextView(this)
        text.setText(value)
        TextViewCompat.setTextAppearance(
            text,
            if (title) com.google.android.material.R.style.TextAppearance_Material3_TitleLarge
            else if (size >= 16)
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
            else com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
        )
        text.setTextColor(
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOnSurface,
                javaClass.getName(),
            )
        )
        val p = LayoutParams(-1, -2)
        p.bottomMargin = dp(bottom)
        parent.addView(text, p)
    }

    private fun button(parent: LinearLayout, label: String?, action: Runnable) {
        val b =
            MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle,
            )
        b.setText(label)
        b.setMinHeight(dp(48))
        b.setOnClickListener(android.view.View.OnClickListener { v: View? -> action.run() })
        val primary =
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorPrimary,
                javaClass.getName(),
            )
        val background =
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorSurfaceContainer,
                javaClass.getName(),
            )
        val foreground =
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOnSurface,
                javaClass.getName(),
            )
        b.setTextColor(
            if (
                ColorUtils.calculateContrast(primary or -0x1000000, background or -0x1000000) >= 4.5
            )
                primary
            else foreground
        )
        val p = LayoutParams(-1, -2)
        p.bottomMargin = dp(8)
        parent.addView(b, p)
    }

    private fun store(parent: LinearLayout, pkg: String?, name: Int) {
        var version: String?
        try {
            version = getPackageManager().getPackageInfo(pkg!!, 0).versionName
        } catch (e: Exception) {
            version = getString(R.string.not_installed)
        }
        text(parent, getString(name) + " · " + version, 14, false, 4)
        button(
            parent,
            getString(R.string.open_store, getString(name)),
            Runnable {
                val launch = getPackageManager().getLaunchIntentForPackage(pkg!!)
                if (launch == null) {
                    Toast.makeText(this, R.string.store_unavailable, Toast.LENGTH_SHORT).show()
                    return@Runnable
                }
                try {
                    startActivity(launch)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, R.string.store_unavailable, Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    private fun language() {
        val tags = arrayOf<String?>("", "ja", "en")
        val current =
            getSystemService<LocaleManager?>(LocaleManager::class.java)
                .getApplicationLocales()
                .toLanguageTags()
        var selected: Int = 0
        for (i in tags.indices) if (tags[i] == current) selected = i
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.language)
            .setSingleChoiceItems(
                arrayOf<String?>(getString(R.string.system_language), "日本語", "English"),
                selected,
                android.content.DialogInterface.OnClickListener {
                    dialog: DialogInterface?,
                    index: Int ->
                    getSystemService<LocaleManager?>(LocaleManager::class.java)
                        .setApplicationLocales(LocaleList.forLanguageTags(tags[index]))
                    dialog!!.dismiss()
                },
            )
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun licenses() {
        var full = getString(R.string.license_text)
        try {
            getAssets().open("LICENSES.txt").use { `in` ->
                full += "\n\n" + String(`in`.readAllBytes(), StandardCharsets.UTF_8)
            }
        } catch (ignored: IOException) {}
        val text = TextView(this)
        text.setText(full)
        text.setTextSize(14f)
        text.setTextIsSelectable(true)
        text.setPadding(dp(24), dp(16), dp(24), dp(16))
        val scroll = ScrollView(this)
        scroll.addView(text)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.licenses)
            .setView(scroll)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun dp(n: Int): Int {
        return Math.round(n * getResources().getDisplayMetrics().density)
    }

    companion object {
        const val SOURCE: String = "https://github.com/ryuya0124/SleepStore"

        @JvmStatic
        fun frameworkLoaded(): Boolean {
            return false
        }
    }
}
