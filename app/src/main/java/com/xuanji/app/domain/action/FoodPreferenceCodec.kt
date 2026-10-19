package com.xuanji.app.domain.action

import com.google.gson.Gson
import com.google.gson.JsonElement

/** Validate named enum values before Gson can silently turn an unknown value into null. */
object FoodPreferenceCodec {
    fun decode(value: JsonElement): FoodPreference {
        require(value.isJsonObject)
        val root = value.asJsonObject
        root.get("availableEquipment")?.takeUnless { it.isJsonNull }?.let { field ->
            require(field.isJsonArray && field.asJsonArray.size() <= KitchenEquipment.entries.size)
            val names = field.asJsonArray.map { it.asString }
            require(names.distinct().size == names.size && names.all { name -> KitchenEquipment.entries.any { it.name == name } })
        }
        root.get("maxCookingDifficulty")?.takeUnless { it.isJsonNull }?.let { field ->
            require(CookingDifficulty.entries.any { it.name == field.asString })
        }
        return Gson().fromJson(root, FoodPreference::class.java).also {
            require(it.excludedIngredients.size <= 12 && it.allergens.size <= 12)
        }
    }
}
