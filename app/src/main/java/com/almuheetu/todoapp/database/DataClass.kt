package com.almuheetu.todoapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_lists")
data class TodoList(
    @PrimaryKey(autoGenerate = true) val listId: Long = 0L,
    val name: String,
    val userId: String? = null
)

@Entity(tableName = "todo_items")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val listId: Long,
    val task: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)