package com.keynova.keyboard.theme

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.keynova.keyboard.R

data class KeyboardTheme(
    val id: String,
    val name: String,
    @ColorInt val background: Int,
    @ColorInt val keyBg: Int,
    @ColorInt val keyBgPressed: Int,
    @ColorInt val keySpecialBg: Int,
    @ColorInt val keySpecialPressed: Int,
    @ColorInt val accent: Int,
    @ColorInt val accentPressed: Int,
    @ColorInt val text: Int,
    @ColorInt val textSecondary: Int,
    @DrawableRes val keyDrawable: Int = R.drawable.bg_key,
    @DrawableRes val keySpecialDrawable: Int = R.drawable.bg_key_special,
    @DrawableRes val keyAccentDrawable: Int = R.drawable.bg_key_accent,
    val keyRadiusDp: Int = 10,
    val keyStroke: Boolean = false
)

object ThemeManager {
    fun all(): List<KeyboardTheme> = themes
    fun byId(id: String): KeyboardTheme = themes.find { it.id == id } ?: themes.first()

    private fun c(
        id: String, name: String,
        bg: Long, k: Long, kp: Long, s: Long, sp: Long, a: Long, ap: Long, t: Long, ts: Long,
        kd: Int = R.drawable.bg_key, sd: Int = R.drawable.bg_key_special, ad: Int = R.drawable.bg_key_accent,
        r: Int = 10, st: Boolean = false
    ) = KeyboardTheme(id, name, bg.i(), k.i(), kp.i(), s.i(), sp.i(), a.i(), ap.i(), t.i(), ts.i(), kd, sd, ad, r, st)

    private val themes = listOf(
        c("dark_gold", "طلایی تیره", 0xFF10131A, 0xFF202634, 0xFF30384A, 0xFF171C28, 0xFF252C3A, 0xFFD4A72C, 0xFFB98A18, 0xFFF3F5F7, 0xFFBFC7D5),
        c("midnight", "آبی نیمه‌شب", 0xFF0C1020, 0xFF1A2238, 0xFF26314D, 0xFF141B2E, 0xFF1F2942, 0xFF5B8CFF, 0xFF3E6FE0, 0xFFEAF0FF, 0xFFAAB9D6),
        c("emerald", "زمردی", 0xFF0F1714, 0xFF1D2A24, 0xFF294038, 0xFF16211C, 0xFF22332C, 0xFF3CCB8F, 0xFF27A873, 0xFFEAF7F1, 0xFFB4CCC0),
        c("rose", "رز", 0xFF181015, 0xFF2C1E26, 0xFF3D2834, 0xFF231720, 0xFF34212D, 0xFFFF6FA1, 0xFFE05286, 0xFFFFF0F5, 0xFFD6B8C6),
        c("light", "روشن", 0xFFECEFF4, 0xFFFFFFFF, 0xFFDDE3EC, 0xFFDCE3ED, 0xFFCBD5E1, 0xFF2563EB, 0xFF1D4ED8, 0xFF1A2233, 0xFF5A6B82, st = true),
        c("graphite", "گرافیت", 0xFF141414, 0xFF262626, 0xFF363636, 0xFF1D1D1D, 0xFF2D2D2D, 0xFFE0E0E0, 0xFFC0C0C0, 0xFFF5F5F5, 0xFFAAAAAA),
        c("ocean", "فیروزه‌ای", 0xFF081821, 0xFF102A38, 0xFF183C50, 0xFF0C2230, 0xFF143447, 0xFF2DD4BF, 0xFF14B8A6, 0xFFE6FFFB, 0xFFA6D6D0),
        c("violet", "بنفش", 0xFF140F1F, 0xFF241B38, 0xFF33274F, 0xFF1C1530, 0xFF2A2045, 0xFFA78BFA, 0xFF8B5CF6, 0xFFF5F3FF, 0xFFC4B5FD),
        c("sunset", "غروب", 0xFF1A120D, 0xFF2E2118, 0xFF3D2D20, 0xFF241A13, 0xFF35261B, 0xFFFF8A5B, 0xFFE06A3A, 0xFFFFF4EE, 0xFFD8BBA8),
        c("crimson", "زرشکی", 0xFF170D10, 0xFF2B181D, 0xFF3B2229, 0xFF221318, 0xFF331C23, 0xFFFF4D6D, 0xFFE03557, 0xFFFFF1F3, 0xFFD8B0BA),
        c("sky", "آسمانی", 0xFF0B1620, 0xFF16293A, 0xFF203A52, 0xFF122230, 0xFF1B3347, 0xFF38BDF8, 0xFF0EA5E9, 0xFFF0FAFF, 0xFFB4D4E6),
        c("lime", "لیمویی", 0xFF12160C, 0xFF222A16, 0xFF303C1F, 0xFF1B2212, 0xFF28331A, 0xFFA3E635, 0xFF84CC16, 0xFFF7FEE7, 0xFFCFE0A8),
        c("sand", "شنی روشن", 0xFFF3EEE4, 0xFFFFFBF5, 0xFFE7DDCE, 0xFFE9DFD0, 0xFFDCCFBC, 0xFFB45309, 0xFF92400E, 0xFF2A2118, 0xFF7A6854, st = true),
        c("lavender", "یاسی روشن", 0xFFF1EDF8, 0xFFFFFCFF, 0xFFE3DCEF0, 0xFFE4DBF0, 0xFFD6CAE6, 0xFF7C3AED, 0xFF6D28D9, 0xFF241B33, 0xFF72658A, st = true),
        c("mint", "نعنایی روشن", 0xFFEDF7F2, 0xFFFFFFFF, 0xFFDCEEE4, 0xFFDCECE2, 0xFFCCE1D5, 0xFF059669, 0xFF047857, 0xFF18261F, 0xFF60786C, st = true),
        c("coffee", "قهوه‌ای", 0xFF17120F, 0xFF2A211B, 0xFF392D25, 0xFF211A15, 0xFF30261F, 0xFFD6A25E, 0xFFBC8742, 0xFFF8F1E8, 0xFFD2C0AE),
        c("navy_coral", "سرمه‌ای مرجانی", 0xFF0E1422, 0xFF1C2740, 0xFF283757, 0xFF162033, 0xFF22304A, 0xFFFF7A90, 0xFFEF5D76, 0xFFF4F7FF, 0xFFB7C3DA),
        c("slate_teal", "اسلیت سبزآبی", 0xFF11181B, 0xFF1E2C31, 0xFF2A3C42, 0xFF182429, 0xFF24343A, 0xFF5EEAD4, 0xFF2DD4BF, 0xFFEDFDFC, 0xFFAAC7C3),
        c("plum_gold", "بادمجانی طلایی", 0xFF180F19, 0xFF2B1C2E, 0xFF3A2740, 0xFF221524, 0xFF321F35, 0xFFF0C75E, 0xFFD9AD3D, 0xFFFFF8EE, 0xFFD8C8A8),
        c("ice_blue", "یخی آبی", 0xFFEAF2FA, 0xFFFFFFFF, 0xFFD8E6F3, 0xFFDCE8F3, 0xFFCADAE8, 0xFF0284C7, 0xFF0369A1, 0xFF172534, 0xFF627A90, st = true),
    )

    private fun Long.i() = this.toInt()
}
