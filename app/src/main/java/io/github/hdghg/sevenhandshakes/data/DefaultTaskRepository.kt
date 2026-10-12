package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTaskRepository
    @Inject
    constructor(
        @param:DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    ) : TaskRepository {
        override suspend fun createTask(title: String, description: String): String {
            return ""
        }

        override suspend fun updateTask(taskId: String, title: String, description: String) {
        }

        override fun getTasksStream(): Flow<List<Task>> = emptyFlow()

        override suspend fun refreshTask(taskId: String) {
            refresh()
        }

        override fun getTaskStream(taskId: String): Flow<Task?> = emptyFlow()

        override suspend fun getTask(taskId: String, forceUpdate: Boolean): Task? {
            return null
        }

        override suspend fun completeTask(taskId: String) {}

        override suspend fun activateTask(taskId: String) {
        }

        override suspend fun clearCompletedTasks() {
        }

        override suspend fun deleteTask(taskId: String) {
        }

        override suspend fun refresh() {
        }
    }
