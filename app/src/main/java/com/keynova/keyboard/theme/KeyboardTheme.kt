package com.keynova.keyboard.theme

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.keynova.keyboard.R

data class KeyboardTheme(
    val id: String,
    val name: String,
    val category: ThemeCategory,
    @ColorInt val background: Int,
    @ColorInt val keyBg: Int,
    @ColorInt val keyBgPressed: Int,
    @ColorInt val keySpecialBg: Int,
    @ColorInt val keySpecialPressed: Int,
    @ColorInt val accent: Int,
    @ColorInt val accentPressed: Int,
    @ColorInt val text: Int,
    @ColorInt val textSecondary: Int,
    @DrawableRes val keyDrawable: Int,
    @DrawableRes val keySpecialDrawable: Int,
    @DrawableRes val keyAccentDrawable: Int,
    @DrawableRes val wallpaperDrawable: Int? = null,
    val keyRadiusDp: Int = 10,
    val keyStroke: Boolean = false
)

enum class ThemeCategory { COLOR, WALLPAPER }

object ThemeManager {
    fun all(): List<KeyboardTheme> = colorThemes() + wallpaperThemes()

    fun byId(id: String): KeyboardTheme = all().find { it.id == id } ?: all().first()

    fun colorThemes(): List<KeyboardTheme> = listOf(
        t("dark_gold", "طلایی تیره", ThemeCategory.COLOR, 0xFF1E1F22.toInt(), 0xFF2B2D31.toInt(), 0xFF3A3D42.toInt(), 0xFF24262A.toInt(), 0xFF33363B.toInt(), 0xFFD4A017.toInt(), 0xFFB8890F.toInt(), 0xFFFFFFFF.toInt(), 0xFFD0D0D0.toInt(), R.drawable.key_bg, R.drawable.key_bg_special, R.drawable.key_bg_accent),
        t("light_blue", "آبی روشن", ThemeCategory.COLOR, 0xFFF2F5FA.toInt(), 0xFFFFFFFF.toInt(), 0xFFDCE8F7.toInt(), 0xFFE7EDF5.toInt(), 0xFFCFDBEC.toInt(), 0xFF2B7FFF.toInt(), 0xFF1E66D0.toInt(), 0xFF1A2433.toInt(), 0xFF516175.toInt(), R.drawable.key_bg_light, R.drawable.key_bg_light_special, R.drawable.key_bg_light_accent),
        t("pure_black", "مشکی خالص", ThemeCategory.COLOR, 0xFF000000.toInt(), 0xFF1A1A1A.toInt(), 0xFF2A2A2A.toInt(), 0xFF111111.toInt(), 0xFF222222.toInt(), 0xFFFFFFFF.toInt(), 0xFFDDDDDD.toInt(), 0xFFFFFFFF.toInt(), 0xFFBBBBBB.toInt(), R.drawable.key_bg_black, R.drawable.key_bg_black_special, R.drawable.key_bg_black_accent),
        t("blue_black", "آبی مشکی", ThemeCategory.COLOR, 0xFF0B1020.toInt(), 0xFF18233A.toInt(), 0xFF243454.toInt(), 0xFF121B2E.toInt(), 0xFF1E2D48.toInt(), 0xFF4DA3FF.toInt(), 0xFF2F82D8.toInt(), 0xFFEAF2FF.toInt(), 0xFFA9BDD8.toInt(), R.drawable.key_bg_blueblack, R.drawable.key_bg_blueblack_special, R.drawable.key_bg_blueblack_accent),
        t("rose", "رز", ThemeCategory.COLOR, 0xFF1F141A.toInt(), 0xFF35222D.toInt(), 0xFF462E3B.toInt(), 0xFF2B1B24.toInt(), 0xFF3A2631.toInt(), 0xFFFF6FA3.toInt(), 0xFFE05588.toInt(), 0xFFFFF1F6.toInt(), 0xFFE3BFCF.toInt(), R.drawable.key_bg_rose, R.drawable.key_bg_rose_special, R.drawable.key_bg_rose_accent),
        t("mint", "نعنایی", ThemeCategory.COLOR, 0xFFF1FAF7.toInt(), 0xFFFFFFFF.toInt(), 0xFFDDF1EA.toInt(), 0xFFE6F3EF.toInt(), 0xFFD1E8DF.toInt(), 0xFF1FA37A.toInt(), 0xFF178562.toInt(), 0xFF17352D.toInt(), 0xFF4B6A61.toInt(), R.drawable.key_bg_mint, R.drawable.key_bg_mint_special, R.drawable.key_bg_mint_accent),
        t("purple_night", "بنفش شب", ThemeCategory.COLOR, 0xFF171322.toInt(), 0xFF27203A.toInt(), 0xFF352B4E.toInt(), 0xFF201A30.toInt(), 0xFF2D2542.toInt(), 0xFFB388FF.toInt(), 0xFF956CE0.toInt(), 0xFFF3EEFF.toInt(), 0xFFC7BDE0.toInt(), R.drawable.key_bg_purple, R.drawable.key_bg_purple_special, R.drawable.key_bg_purple_accent),
        t("sand", "شنی", ThemeCategory.COLOR, 0xFFF7F1E6.toInt(), 0xFFFFFCF7.toInt(), 0xFFEDE0CC.toInt(), 0xFFF1E7D8.toInt(), 0xFFE5D4BB.toInt(), 0xFFB07A2A.toInt(), 0xFF94631D.toInt(), 0xFF2E2418.toInt(), 0xFF6B5A45.toInt(), R.drawable.key_bg_sand, R.drawable.key_bg_sand_special, R.drawable.key_bg_sand_accent),
    )

    fun wallpaperThemes(): List<KeyboardTheme> = listOf(
        t("wp_galaxy", "کهکشان نئونی", ThemeCategory.WALLPAPER, 0xCC120A24.toInt(), 0xCC2A1E45.toInt(), 0xCC3A2A60.toInt(), 0xCC1E1736.toInt(), 0xCC2E2450.toInt(), 0xFFD59CFF.toInt(), 0xFFB87AEF.toInt(), 0xFFF8F3FF.toInt(), 0xFFDCCCFF.toInt(), R.drawable.key_bg_wp, R.drawable.key_bg_wp_special, R.drawable.key_bg_wp_accent, R.drawable.wallpaper_galaxy),
        t("wp_ocean", "اقیانوس", ThemeCategory.WALLPAPER, 0xCC0A2236.toInt(), 0xCC1C3A57.toInt(), 0xCC2A5275.toInt(), 0xCC152D44.toInt(), 0xCC234661.toInt(), 0xFF58C9FF.toInt(), 0xFF3AAFE6.toInt(), 0xFFFFFFFF.toInt(), 0xFFD4E8F5.toInt(), R.drawable.key_bg_wp, R.drawable.key_bg_wp_special, R.drawable.key_bg_wp_accent, R.drawable.wallpaper_ocean),
        t("wp_sunset", "غروب کوهستان", ThemeCategory.WALLPAPER, 0xCC2A1420.toInt(), 0xCC4A2438.toInt(), 0xCC63324C.toInt(), 0xCC3A1C2D.toInt(), 0xCC552A40.toInt(), 0xFFFFB36B.toInt(), 0xFFE89243.toInt(), 0xFFFFF5EE.toInt(), 0xFFF0D2C0.toInt(), R.drawable.key_bg_wp, R.drawable.key_bg_wp_special, R.drawable.key_bg_wp_accent, R.drawable.wallpaper_sunset),
        t("wp_forest", "جنگل مه‌آلود", ThemeCategory.WALLPAPER, 0xCC101F16.toInt(), 0xCC22382A.toInt(), 0xCC30503A.toInt(), 0xCC1A2C21.toInt(), 0xCC294232.toInt(), 0xFF8BE07A.toInt(), 0xFF6EC75D.toInt(), 0xFFF1FFF0.toInt(), 0xFFCFE0CD.toInt(), R.drawable.key_bg_wp, R.drawable.key_bg_wp_special, R.drawable.key_bg_wp_accent, R.drawable.wallpaper_forest),
    )

    private fun t(id: String, name: String, cat: ThemeCategory, bg: Int, k: Int, kp: Int, ks: Int, ksp: Int, a: Int, ap: Int, tx: Int, ts: Int, kd: Int, ksd: Int, kad: Int, wp: Int? = null) =
        KeyboardTheme(id, name, cat, bg, k, kp, ks, ksp, a, ap, tx, ts, kd, ksd, kad, wp)
}