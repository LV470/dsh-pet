package com.dsh.pet

import android.content.Context

object Prefs {
    private const val FILE = "dsh_pet"
    private const val KEY_API = "deepseek_api_key"
    private const val KEY_POS = "pos"
    private const val KEY_W = "win_w"
    private const val KEY_H = "win_h"
    private const val KEY_ALPHA = "win_alpha"

    fun getKey(ctx: Context): String =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_API, "") ?: ""

    fun setKey(ctx: Context, v: String) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY_API, v).apply()
    }

    fun savePos(ctx: Context, x: Int, y: Int) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY_POS, "$x,$y").commit()
    }

    fun saveSize(ctx: Context, w: Int, h: Int) {
        // 用 commit() 同步写盘：apply() 是异步的，进程被杀时值会丢
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putInt(KEY_W, w).putInt(KEY_H, h).commit()
    }

    fun loadSize(ctx: Context): Pair<Int, Int> {
        val sp = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return sp.getInt(KEY_W, 240) to sp.getInt(KEY_H, 240)
    }

    fun saveAlpha(ctx: Context, a: Float) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putFloat(KEY_ALPHA, a).commit()
    }

    fun loadAlpha(ctx: Context): Float =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getFloat(KEY_ALPHA, 1f)

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
