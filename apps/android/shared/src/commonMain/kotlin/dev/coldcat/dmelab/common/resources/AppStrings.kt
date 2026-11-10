package dev.coldcat.dmelab.common.resources

import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.allStringResources
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getSystemResourceEnvironment

/** Immutable, preloaded strings keep state mapping synchronous and free of platform IO. */
public class AppStrings private constructor(private val values: Map<StringResource, String>) {
    public fun get(resource: StringResource, vararg args: Any): String {
        val template = values.getValue(key = resource)
        if (args.isEmpty()) return template
        var nextArgument = 0
        return FORMAT.replace(input = template) { match ->
            if (match.value == "%%") "%"
            else {
                val index = match.groups[1]?.value?.toInt()?.minus(1) ?: nextArgument++
                require(index in args.indices) { "Missing string argument ${index + 1}" }
                args[index].toString()
            }
        }
    }

    public companion object {
        private val FORMAT = Regex(pattern = "%(?:([0-9]+)\\$)?[sd]|%%")

        @OptIn(ExperimentalResourceApi::class)
        public suspend fun load(): AppStrings {
            val environment = getSystemResourceEnvironment()
            val values = buildMap {
                for (resource in Res.allStringResources.values) {
                    put(key = resource, value = getString(environment = environment, resource = resource))
                }
            }
            return AppStrings(values = values)
        }
    }
}
