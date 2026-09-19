package com.xuanji.app.data.repository

import com.xuanji.app.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ActionRepositoryTest {
    @Test
    fun profile_key_is_stable_and_isolated_by_birth_or_location() {
        val base = UserProfile(1990, 1, 2, 3, 4, "上海 / 上海 / 黄浦", "310101")
        assertEquals(ActionRepository.profileKey(base), ActionRepository.profileKey(base))
        assertNotEquals(
            ActionRepository.profileKey(base),
            ActionRepository.profileKey(base.copy(locationCode = "110101"))
        )
        assertNotEquals(
            ActionRepository.profileKey(base),
            ActionRepository.profileKey(base.copy(birthDay = 3))
        )
    }
}
