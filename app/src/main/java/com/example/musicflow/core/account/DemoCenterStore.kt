package com.example.musicflow.core.account

import android.content.Context

/** A UI-only bookmark: it cannot write Auth, roles, catalog, cloud or playback data. */
class DemoCenterStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "musicflow_demo_center_v1_ui_only", Context.MODE_PRIVATE
    )

    fun selected(): DemoPersona = runCatching {
        DemoPersona.valueOf(prefs.getString("actor", DemoPersona.GUEST.name).orEmpty())
    }.getOrDefault(DemoPersona.GUEST)

    fun select(actor: DemoPersona) {
        check(prefs.edit().putString("actor", actor.name).commit())
    }

    fun progress(actor: DemoPersona): Int =
        DemoCenterPlan.boundedProgress(actor, prefs.getInt("step_${actor.name}", 0))

    fun progressTo(actor: DemoPersona, stepsDone: Int) {
        check(prefs.edit().putInt("step_${actor.name}",
            DemoCenterPlan.boundedProgress(actor, stepsDone)).commit())
    }

    fun reset(actor: DemoPersona) = progressTo(actor, 0)
}
