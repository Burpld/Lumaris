package lumaris

/**
 * Simple global holder for the plugin instance, set once in Main.onEnable().
 * Lets other classes (like Kit.kt) build items that need a NamespacedKey tied
 * to the plugin, without threading a Main reference through every constructor.
 */
object PluginContext {
    lateinit var instance: Main
}
