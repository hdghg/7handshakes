package io.github.hdghg.sevenhandshakes.data

import kotlinx.coroutines.flow.Flow

/**
 * Interface to the data layer.
 */
interface TaskRepository {
    fun getTasksStream(): Flow<List<Task>>

    suspend fun refresh()

    fun getTaskStream(taskId: String): Flow<Task?>

    suspend fun getTask(taskId: String, forceUpdate: Boolean = false): Task?

    suspend fun refreshTask(taskId: String)

    suspend fun createTask(title: String, description: String): String

    suspend fun updateTask(taskId: String, title: String, description: String)

    suspend fun completeTask(taskId: String)

    suspend fun activateTask(taskId: String)

    suspend fun clearCompletedTasks()

    suspend fun deleteTask(taskId: String)
}
