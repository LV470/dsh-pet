package com.dsh.pet

import android.content.Context

object Prefs {
    private const val FILE = "dsh_pet"
    private const val KEY_API = "deepseek_api_key"
    private const val KEY_POS = "pos"

    fun getKey(ctx: Context): String =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_API, "") ?: ""

    fun setKey(ctx: Context, v: String) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY_API, v).apply()
    }

    fun savePos(ctx: Context, x: Int, y: Int) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY_POS, "$x,$y").apply()
    }

    fun loadPos(ctx: Context): Pair<Int, Int> {
        val s = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_POS, "") ?: ""
        val p = s.split(",")
        if (p.size == 2) {
            val x = p[0].toIntOrNull(); val y = p[1].toIntOrNull()
            if (x != null && y != null) return x to y
        }
        return 60 to 400
    }
}
