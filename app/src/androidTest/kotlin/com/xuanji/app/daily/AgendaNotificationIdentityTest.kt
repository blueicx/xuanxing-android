package com.xuanji.app.daily

import androidx.test.platform.app.InstrumentationRegistry
import com.xuanji.app.domain.DailyAgendaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AgendaNotificationIdentityTest {
    @Test fun pending_actions_do_not_cross_profiles_items_actions_or_rescheduled_times() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val item = DailyAgendaItem("same-id", "2026-10-16", "12:00", "合成安排", "custom")
        val complete = AgendaReminderActionReceiver.pendingIntent(context, item, "synthetic-a", AgendaReminderActionReceiver.COMPLETE, 1002)
        val repeated = AgendaReminderActionReceiver.pendingIntent(context, item, "synthetic-a", AgendaReminderActionReceiver.COMPLETE, 1002)
        val otherProfile = AgendaReminderActionReceiver.pendingIntent(context, item, "synthetic-b", AgendaReminderActionReceiver.COMPLETE, 1002)
        val snooze = AgendaReminderActionReceiver.pendingIntent(context, item, "synthetic-a", AgendaReminderActionReceiver.SNOOZE, 1002)
        val moved = AgendaReminderActionReceiver.pendingIntent(context, item.copy(time = "13:00"), "synthetic-a", AgendaReminderActionReceiver.COMPLETE, 1002)
        try {
            assertEquals(complete, repeated)
            assertNotEquals(complete, otherProfile)
            assertNotEquals(complete, snooze)
            assertNotEquals(complete, moved)
            assertNotEquals(AgendaReminderActionReceiver.snoozeWorkName("synthetic-a", item.id),
                AgendaReminderActionReceiver.snoozeWorkName("synthetic-b", item.id))
        } finally {
            // None of these tokens is sent; no user record or notification is created.
            listOf(complete, otherProfile, snooze, moved).forEach { it.cancel() }
        }
    }
}
