package lumaris.kit

import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * The 6 Battle Box kits. Each one knows how to gear up a player via applyTo().
 * Kits with an active ability (Speedster, Mage) tag their special item with
 * KIT_ABILITY_KEY so KitAbilities.kt (the listener) knows what to do when it's used.
 * Healer's ability reuses the existing SpawnBattleItem "Regen Star" item.
 */
enum class Kit(val displayName: String, val icon: Material, val description: String) {
    TANK("Tank", Material.IRON_CHESTPLATE, "High health, heavy armor. Built to hold the center."),
    ARCHER("Archer", Material.BOW, "Ranged damage, lighter armor, faster shots."),
    SPEEDSTER("Speedster", Material.FEATHER, "Fast mover with a short dash ability."),
    MAGE("Mage", Material.BLAZE_POWDER, "Squishy, but throws a knockback bolt."),
    BRUISER("Bruiser", Material.IRON_SWORD, "Heavy melee damage, average armor."),
    HEALER("Healer", Material.GOLDEN_APPLE, "Supports the team with the Regen Star.");

    companion object {
        val KIT_KEY = NamespacedKey("lumaris", "kit")
        val KIT_ABILITY_KEY = NamespacedKey("lumaris", "kit_ability")

        /** Reads which kit (if any) is stored on a player via persistent data. */
        fun of(player: Player): Kit? {
            val stored = player.persistentDataContainer.get(KIT_KEY, PersistentDataType.STRING) ?: return null
            return entries.find { it.name == stored }
        }
    }

    /** Clears the player's inventory/effects and gives them this kit's loadout. */
    fun applyTo(player: Player) {
        player.inventory.clear()
        player.inventory.armorContents = arrayOfNulls(4)
        for (effect in player.activePotionEffects) {
            player.removePotionEffect(effect.type)
        }
        resetMaxHealth(player)

        player.persistentDataContainer.set(KIT_KEY, PersistentDataType.STRING, this.name)

        when (this) {
            TANK -> {
                player.inventory.helmet = ItemStack(Material.IRON_HELMET)
                player.inventory.chestplate = ItemStack(Material.IRON_CHESTPLATE)
                player.inventory.leggings = ItemStack(Material.IRON_LEGGINGS)
                player.inventory.boots = ItemStack(Material.IRON_BOOTS)
                player.inventory.addItem(ItemStack(Material.STONE_SWORD))
                addMaxHealthBonus(player, 8.0) // +4 hearts
                player.getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.baseValue = 0.4
                player.inventory.addItem(taggedItem(Material.SHIELD, "&bGuardian's Call", TANK))
            }

            ARCHER -> {
                player.inventory.chestplate = ItemStack(Material.LEATHER_CHESTPLATE)
                player.inventory.leggings = ItemStack(Material.LEATHER_LEGGINGS)
                player.inventory.boots = ItemStack(Material.LEATHER_BOOTS)
                player.inventory.addItem(ItemStack(Material.BOW))
                player.inventory.addItem(ItemStack(Material.ARROW, 32))
                player.inventory.addItem(ItemStack(Material.WOODEN_SWORD))
            }

            SPEEDSTER -> {
                player.inventory.boots = ItemStack(Material.LEATHER_BOOTS)
                player.inventory.addItem(ItemStack(Material.WOODEN_SWORD))
                player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, Int.MAX_VALUE, 1, true, false))
                player.inventory.addItem(taggedItem(Material.SUGAR, "&bDash", SPEEDSTER))
            }

            MAGE -> {
                player.inventory.chestplate = ItemStack(Material.LEATHER_CHESTPLATE)
                player.inventory.addItem(ItemStack(Material.WOODEN_SWORD))
                player.inventory.addItem(taggedItem(Material.SNOWBALL, "&bArcane Bolt", MAGE, amount = 16))
            }

            BRUISER -> {
                player.inventory.chestplate = ItemStack(Material.CHAINMAIL_CHESTPLATE)
                player.inventory.leggings = ItemStack(Material.CHAINMAIL_LEGGINGS)
                player.inventory.addItem(ItemStack(Material.IRON_SWORD))
                addDamageBonus(player, 2.0)
            }

            HEALER -> {
                player.inventory.chestplate = ItemStack(Material.LEATHER_CHESTPLATE)
                player.inventory.boots = ItemStack(Material.LEATHER_BOOTS)
                player.inventory.addItem(ItemStack(Material.WOODEN_SWORD))
                player.inventory.addItem(createRegenStarItem())
            }
        }
    }

    /** Builds the same "Regen Star" item as /spawnbattleitem regenstar, so Healers get the real thing. */
    private fun createRegenStarItem(): ItemStack {
        val item = ItemStack(Material.FIREWORK_STAR)
        val meta = item.itemMeta
        meta.setDisplayName("\u00A7a\u00A7lRegen Star")
        meta.persistentDataContainer.set(
            org.bukkit.NamespacedKey(lumaris.PluginContext.instance, "special_item_id"),
            PersistentDataType.STRING,
            "regeneration_circle"
        )
        item.itemMeta = meta
        return item
    }

    private fun taggedItem(material: Material, name: String, kit: Kit, amount: Int = 1): ItemStack {
        val item = ItemStack(material, amount)
        val meta: ItemMeta = item.itemMeta
        meta.setDisplayName(name.replace("&", "\u00A7"))
        meta.persistentDataContainer.set(KIT_ABILITY_KEY, PersistentDataType.STRING, kit.name)
        item.itemMeta = meta
        return item
    }

    private fun resetMaxHealth(player: Player) {
        player.getAttribute(Attribute.MAX_HEALTH)?.let { attr ->
            attr.modifiers.forEach { attr.removeModifier(it) }
        }
        player.getAttribute(Attribute.ATTACK_DAMAGE)?.let { attr ->
            attr.modifiers.forEach { attr.removeModifier(it) }
        }
        player.getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.baseValue = 0.0
        player.health = player.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
    }

    private fun addMaxHealthBonus(player: Player, amount: Double) {
        val attr = player.getAttribute(Attribute.MAX_HEALTH) ?: return
        attr.addModifier(
            AttributeModifier(NamespacedKey("lumaris", "kit_health_bonus"), amount, AttributeModifier.Operation.ADD_NUMBER)
        )
        player.health = attr.value
    }

    private fun addDamageBonus(player: Player, amount: Double) {
        val attr = player.getAttribute(Attribute.ATTACK_DAMAGE) ?: return
        attr.addModifier(
            AttributeModifier(NamespacedKey("lumaris", "kit_damage_bonus"), amount, AttributeModifier.Operation.ADD_NUMBER)
        )
    }
}
