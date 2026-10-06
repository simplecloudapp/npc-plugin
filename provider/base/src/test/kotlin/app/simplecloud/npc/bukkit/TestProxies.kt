package app.simplecloud.npc.bukkit

import java.lang.reflect.Proxy

inline fun <reified T> proxy(): T = Proxy.newProxyInstance(
    T::class.java.classLoader,
    arrayOf(T::class.java),
) { _, method, _ -> error("Unexpected call to ${T::class.simpleName}.${method.name}") } as T
