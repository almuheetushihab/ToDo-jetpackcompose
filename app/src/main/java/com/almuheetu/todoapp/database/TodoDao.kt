package com.almuheetu.todoapp.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    // TodoList operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodoList(todoList: TodoList): Long

    @Delete
    suspend fun deleteTodoList(todoList: TodoList)

    @Query("SELECT * FROM todo_lists WHERE userId = :userId ORDER BY name ASC")
    fun getAllTodoLists(userId: String?): Flow<List<TodoList>>

    @Query("SELECT * FROM todo_lists WHERE listId = :listId")
    suspend fun getTodoListById(listId: Long): TodoList?

    // TodoItem operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodoItem(todoItem: TodoItem)

    @Update
    suspend fun updateTodoItem(todoItem: TodoItem)

    @Delete
    suspend fun deleteTodoItem(todoItem: TodoItem)

    @Query("SELECT * FROM todo_items WHERE listId = :listId ORDER BY createdAt DESC")
    fun getTodoItemsForList(listId: Long): Flow<List<TodoItem>>

    @Query("DELETE FROM todo_items WHERE listId = :listId")
    suspend fun deleteAllItemsInList(listId: Long)
}