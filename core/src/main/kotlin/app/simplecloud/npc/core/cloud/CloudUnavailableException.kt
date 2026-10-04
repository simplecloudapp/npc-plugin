package app.simplecloud.npc.core.cloud

import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException

private const val MAX_CAUSES = 16

class CloudUnavailableException(cause: Throwable) :
    RuntimeException("The SimpleCloud controller is unreachable: ${cause.summary()}", cause)

private fun Throwable.summary(): String {
    val chain = generateSequence(this) { it.cause?.takeIf { cause -> cause !== it } }.take(MAX_CAUSES).toList()
    val pick = chain.firstOrNull { throwable ->
        val ownMessage = throwable.message?.trim().orEmpty()
        val cause = throwable.cause
        val wrapper = throwable is CompletionException ||
            throwable is ExecutionException ||
            throwable.javaClass == RuntimeException::class.java
        val repeatsCause = wrapper && cause != null && ownMessage.endsWith(cause.message.orEmpty())
        ownMessage.isNotEmpty() && !repeatsCause
    } ?: chain.last()
    val message = pick.message?.lineSequence()?.map(String::trim)?.firstOrNull { it.isNotEmpty() }.orEmpty()

    return message.ifEmpty { pick::class.simpleName ?: "unknown error" }
}
