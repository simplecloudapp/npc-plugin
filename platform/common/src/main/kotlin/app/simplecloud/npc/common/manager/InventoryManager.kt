package app.simplecloud.npc.common.manager

import app.simplecloud.npc.core.config.ConfigIds
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.inventory.InventoryRepository

class InventoryManager(
    private val repository: InventoryRepository,
) {

    fun create(id: String, rows: Int): InventoryOperationResult {
        if (!ConfigIds.isValid(id)) return InventoryOperationResult.Failure(InventoryFailure.INVALID_ID, id)
        if (repository.find(id) != null) return InventoryOperationResult.Failure(InventoryFailure.ALREADY_EXISTS, id)
        if (rows !in InventoryConfiguration.ROWS) return InventoryOperationResult.Failure(
            InventoryFailure.INVALID_ROWS,
            rows.toString(),
        )

        val config = InventoryConfiguration(id = id, rows = rows)
        repository.save(config)

        return InventoryOperationResult.Success(config)
    }

    fun delete(id: String): InventoryOperationResult {
        val config = repository.find(id) ?: return InventoryOperationResult.Failure(InventoryFailure.NOT_FOUND, id)
        repository.delete(config.id)

        return InventoryOperationResult.Success(config)
    }
}

sealed interface InventoryOperationResult {
    data class Success(val config: InventoryConfiguration) : InventoryOperationResult
    data class Failure(val failure: InventoryFailure, val detail: String? = null) : InventoryOperationResult
}

enum class InventoryFailure {
    INVALID_ID,
    ALREADY_EXISTS,
    INVALID_ROWS,
    NOT_FOUND,
}
