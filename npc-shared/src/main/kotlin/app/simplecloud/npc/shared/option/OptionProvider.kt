package app.simplecloud.npc.shared.option

/**
 * @author Niklas Nieberler
 */

@Suppress("UNCHECKED_CAST")
open class OptionProvider(
    private val options: MutableList<Option> = arrayListOf()
) {

    fun <T : Any> hasOption(pair: Pair<String, T>): Boolean {
        return getOptions().firstOrNull { it.key == pair.first } != null
    }

    fun <T : Any> getOption(key: String, defaultValue: T): T {
        return (getOptions().firstOrNull { it.key.equals(key, true) }?.value ?: defaultValue) as T
    }

    fun <T : Any> getOption(pair: Pair<String, T>): T {
        return (getOptions().firstOrNull { it.key.equals(pair.first, true) }?.value ?: pair.second) as T
    }

    fun add(vararg option: Option) {
        this.options.addAll(option)
    }

    open fun getOptions(): List<Option> = this.options

    companion object {
        fun with(vararg option: Option): OptionProvider {
            return OptionProvider(option.toMutableList())
        }
    }

}