package com.almuheetu.todoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.compose.material.AlertDialog
import androidx.compose.material.TextButton
import androidx.compose.ui.graphics.Color
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.DrawerValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalDrawer
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.rememberDrawerState
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.almuheetu.todoapp.database.AppDatabase
import com.almuheetu.todoapp.database.TodoItem
import com.almuheetu.todoapp.ui.screen.TodoViewModel
import com.almuheetu.todoapp.ui.screen.TodoViewModelFactory
import com.almuheetu.todoapp.ui.theme.ToDoAppTheme
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        createNotificationChannel() // For notifications

        setContent {
            ToDoAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val database = AppDatabase.getDatabase(applicationContext)
                    val viewModel: TodoViewModel = viewModel(
                        factory = TodoViewModelFactory(database.todoDao())
                    )

                    // Observe current user
                    val currentUser = auth.currentUser
                    LaunchedEffect(currentUser) {
                        viewModel.setUserId(currentUser?.uid)
                    }

                    // Check authentication status and navigate
                    val authState = remember { mutableStateOf(auth.currentUser != null) }

                    if (authState.value) {
                        // Main App Content
                        ToDoAppContent(viewModel = viewModel) {
                            auth.signOut()
                            authState.value = false
                        }
                    } else {
                        // Authentication UI
                        AuthScreen(auth = auth) { success ->
                            if (success) {
                                authState.value = true
                                viewModel.setUserId(auth.currentUser?.uid)
                            } else {
                                // Show error
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Notification Channel Creation ---
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

const val CHANNEL_ID = "todo_notifications"

// --- Authentication UI ---
@Composable
fun AuthScreen(auth: FirebaseAuth, onAuthResult: (Boolean) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) } // For register/login toggle

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to My To-Do App", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isRegistering) {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            onAuthResult(task.isSuccessful)
                        }
                } else {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            onAuthResult(task.isSuccessful)
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isRegistering) "Register" else "Login")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { isRegistering = !isRegistering }) {
            Text(if (isRegistering) "Already have an account? Login" else "Don't have an account? Register")
        }
    }
}

// --- Main To-Do App UI ---
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ToDoAppContent(viewModel: TodoViewModel, onLogout: () -> Unit) {
    val todoLists by viewModel.allTodoLists.collectAsState()
    val selectedList by viewModel.selectedList.collectAsState()
    val todoItems by viewModel.todoItemsForSelectedList.collectAsState()

    var showAddListDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    var showListsDrawer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedList?.name ?: "Select a List") },
                navigationIcon = {
                    IconButton(onClick = { showListsDrawer = true }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open drawer")
                    }
                },
                actions = {
                    if (selectedList != null) {
                        IconButton(onClick = {
                            selectedList?.let { viewModel.deleteTodoList(it) }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete current list")
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedList != null) {
                FloatingActionButton(onClick = { /* Add new task dialog/logic */ }) {
                    Icon(Icons.Filled.Add, "Add new task")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Input for new task (only visible if a list is selected)
            selectedList?.let {
                var newTaskText by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTaskText,
                        onValueChange = { newTaskText = it },
                        label = { Text("New Task for ${it.name}") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (newTaskText.isNotBlank()) {
                            viewModel.addTodoItem(newTaskText)
                            newTaskText = ""
                        }
                    }) {
                        Text("Add")
                    }
                }
            } ?: run {
                // Prompt to select or create a list
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Please select or create a To-Do List to get started.",
                        style = MaterialTheme.typography.h6
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showAddListDialog = true }) {
                        Text("Create New List")
                    }
                }
            }


            // List of To-Do items
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(todoItems) { todoItem ->
                    TodoItemCard(
                        todoItem = todoItem,
                        onToggleComplete = { toggledItem ->
                            viewModel.toggleTodoItemCompletion(toggledItem)
                        },
                        onDelete = { deletedItem ->
                            viewModel.deleteTodoItem(deletedItem)
                        },
                        context = LocalContext.current // Pass context for notifications
                    )
                }
            }
        }

        // --- Navigation Drawer for Lists ---
        if (showListsDrawer) {
            ModalDrawer(
                drawerContent = {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            "Your Lists",
                            style = MaterialTheme.typography.h5,
                            modifier = Modifier.padding(16.dp)
                        )
                        Divider()
                        LazyColumn {
                            items(todoLists) { list ->
                                ListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectList(list)
                                            showListsDrawer = false
                                        },
                                    text = { Text(list.name) },
                                    icon = {
                                        if (selectedList?.listId == list.listId) {
                                            Icon(
                                                Icons.Default.List,
                                                contentDescription = "Selected List",
                                                tint = MaterialTheme.colors.primary
                                            )
                                        } else {
                                            Icon(Icons.Default.List, contentDescription = "List")
                                        }
                                    }
                                )
                            }
                            item {
                                Button(
                                    onClick = { showAddListDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text("Create New List")
                                }
                            }
                        }
                    }
                },
                gesturesEnabled = true,
                drawerState = rememberDrawerState(initialValue = DrawerValue.Open), // Open by default
                onDrawerStateChange = { newState ->
                    if (newState == DrawerValue.Closed) {
                        showListsDrawer = false
                    }
                },
                content = { /* Main content remains behind the drawer */ }
            )
        }


        // --- Add New List Dialog ---
        if (showAddListDialog) {
            AlertDialog(
                onDismissRequest = { showAddListDialog = false },
                title = { Text("Create New To-Do List") },
                text = {
                    TextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        label = { Text("List Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newListName.isNotBlank()) {
                            viewModel.createTodoList(newListName)
                            newListName = ""
                            showAddListDialog = false
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddListDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


@Composable
fun TodoItemCard(
    todoItem: TodoItem,
    onToggleComplete: (TodoItem) -> Unit,
    onDelete: (TodoItem) -> Unit,
    context: Context // For notifications
) {
    val showPermissionDialog = remember { mutableStateOf(false) }
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, show notification
            showNotification(context, todoItem.task)
        } else {
            // Permission denied
            Toast.makeText(context, "Notification permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = todoItem.task,
                style = MaterialTheme.typography.h6.copy(
                    textDecoration = if (todoItem.isCompleted) TextDecoration.LineThrough else null,
                    color = if (todoItem.isCompleted) Color.Gray else Color.Unspecified
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onToggleComplete(todoItem) }) {
                Icon(
                    if (todoItem.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Check,
                    "Toggle complete"
                )
            }
            IconButton(onClick = {
                // Request POST_NOTIFICATIONS permission for Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        showNotification(context, todoItem.task)
                    } else {
                        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    showNotification(context, todoItem.task)
                }
            }) {
                Icon(Icons.Filled.Notifications, "Set Reminder")
            }
            IconButton(onClick = { onDelete(todoItem) }) {
                Icon(Icons.Filled.Delete, "Delete task")
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun DefaultPreview() {
//    ToDoAppTheme {
//        // For preview, provide dummy data/state
//        // This preview won't run a full app with database/Firebase
//        ToDoAppContent(viewModel = viewModel(factory = TodoViewModelFactory(object : TodoDao {
//            // Dummy DAO for preview
//            override suspend fun insertTodoList(todoList: TodoList): Long = 0L
//            override suspend fun deleteTodoList(todoList: TodoList) {}
//            override fun getAllTodoLists(userId: String?): Flow<List<TodoList>> = flowOf(listOf(TodoList(name = "Personal", listId = 1)))
//            override suspend fun getTodoListById(listId: Long): TodoList? = null
//            override suspend fun insertTodoItem(todoItem: TodoItem) {}
//            override suspend fun updateTodoItem(todoItem: TodoItem) {}
//            override suspend fun deleteTodoItem(todoItem: TodoItem) {}
//            override fun getTodoItemsForList(listId: Long): Flow<List<TodoItem>> = flowOf(listOf(
//                TodoItem(task = "Sample Task 1", id = 1, listId = 1, isCompleted = false),
//                TodoItem(task = "Completed Task", id = 2, listId = 1, isCompleted = true)
//            ))
//            override suspend fun deleteAllItemsInList(listId: Long) {}
//        })), onLogout = {})
//    }
//}

// Function to show a simple notification
fun showNotification(context: Context, taskName: String) {
    val notificationId = UUID.randomUUID().hashCode() // Unique ID for each notification

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app icon
        .setContentTitle("To-Do Reminder")
        .setContentText("Don't forget: $taskName")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true) // Dismiss notification when tapped

    with(NotificationManagerCompat.from(context)) @androidx.annotation.RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS) {
        // notificationId is a unique int for each notification that you must define
        notify(notificationId, builder.build())
    }
    Toast.makeText(context, "Reminder set for '$taskName'", Toast.LENGTH_SHORT).show()
}