package com.almuheetu.todoapp.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.almuheetu.todoapp.database.TodoDao
import com.almuheetu.todoapp.database.TodoItem
import com.almuheetu.todoapp.database.TodoList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(private val todoDao: TodoDao) : ViewModel() {

    // For Authentication
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    fun setUserId(userId: String?) {
        _currentUserId.value = userId
    }

    // For multiple lists
    val allTodoLists: StateFlow<List<TodoList>> = currentUserId.flatMapLatest { userId ->
        if (userId != null) {
            todoDao.getAllTodoLists(userId)
        } else {
            flowOf(emptyList()) // Or handle unauthenticated state
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    private val _selectedList = MutableStateFlow<TodoList?>(null)
    val selectedList: StateFlow<TodoList?> = _selectedList.asStateFlow()

    val todoItemsForSelectedList: StateFlow<List<TodoItem>> =
        _selectedList.flatMapLatest { selectedList ->
            selectedList?.let { todoDao.getTodoItemsForList(it.listId) } ?: flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun selectList(list: TodoList) {
        _selectedList.value = list
    }

    fun createTodoList(name: String) {
        viewModelScope.launch {
            val userId = _currentUserId.value // Get current user ID
            if (userId != null) {
                val newTodoList = TodoList(name = name, userId = userId)
                val listId = todoDao.insertTodoList(newTodoList)
                // Optionally, select the new list after creation
                _selectedList.value = newTodoList.copy(listId = listId)
            }
        }
    }

    fun deleteTodoList(todoList: TodoList) {
        viewModelScope.launch {
            // Delete all items in the list first
            todoDao.deleteAllItemsInList(todoList.listId)
            todoDao.deleteTodoList(todoList)
            // If the deleted list was selected, deselect it
            if (_selectedList.value?.listId == todoList.listId) {
                _selectedList.value = null
            }
        }
    }

    fun addTodoItem(task: String) {
        viewModelScope.launch {
            _selectedList.value?.let { currentList ->
                val newItem = TodoItem(task = task, listId = currentList.listId)
                todoDao.insertTodoItem(newItem)
            }
        }
    }

    fun toggleTodoItemCompletion(item: TodoItem) {
        viewModelScope.launch {
            todoDao.updateTodoItem(item.copy(isCompleted = !item.isCompleted))
        }
    }

    fun deleteTodoItem(item: TodoItem) {
        viewModelScope.launch {
            todoDao.deleteTodoItem(item)
        }
    }
}

class TodoViewModelFactory(private val todoDao: TodoDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodoViewModel(todoDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}