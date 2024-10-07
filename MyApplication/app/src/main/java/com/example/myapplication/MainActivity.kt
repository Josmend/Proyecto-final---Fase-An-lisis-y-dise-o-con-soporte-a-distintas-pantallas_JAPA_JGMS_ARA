package com.example.myapplication

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

data class MultimediaFile(
    val uri: String,
    val description: String
)

data class Note(
    val title: String,
    val description: String,
    val isTask: Boolean = false,
    val dueDate: String? = null, // Solo aplica para tareas
    val reminders: List<String> = listOf(),
    val multimediaFiles: List<MultimediaFile> = listOf()
)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private var currentPhotoPath: String? = null
    private val multimediaFiles = mutableStateListOf<MultimediaFile>()

    private val captureImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri: Uri? = Uri.fromFile(File(currentPhotoPath))
            imageUri?.let {
                multimediaFiles.add(MultimediaFile(it.toString(), "Imagen capturada"))
            }
        }
    }

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            multimediaFiles.add(MultimediaFile(it.toString(), "Archivo de la galería"))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {
                val notes = remember { mutableStateListOf<Note>() }
                var showDialog by remember { mutableStateOf(false) }
                var selectedNote: Note? by remember { mutableStateOf(null) }
                var searchText by remember { mutableStateOf("") }
                var isSortedByTask by remember { mutableStateOf(false) }

                // Filtrar y ordenar las notas
                val filteredNotes = notes
                    .filter { note ->
                        note.title.contains(searchText, ignoreCase = true) || note.description.contains(searchText, ignoreCase = true)
                    }
                    .sortedBy { note ->
                        if (isSortedByTask && note.isTask) note.dueDate else note.title
                    }

                Scaffold(
                    topBar = {
                        Column {
                            SmallTopAppBar(
                                title = { Text("Notas y Tareas") },
                                actions = {
                                    IconButton(onClick = { showDialog = true }) {
                                        Icon(Icons.Default.Add, contentDescription = "Agregar nota")
                                    }
                                }
                            )
                            // Barra de búsqueda
                            TextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                label = { Text("Buscar...") },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                            // Botón de ordenamiento
                            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                Text("Ordenar por: ")
                                Checkbox(
                                    checked = isSortedByTask,
                                    onCheckedChange = { isSortedByTask = it }
                                )
                                Text(if (isSortedByTask) "Fecha de Tarea" else "Título")
                            }
                        }
                    }
                ) { paddingValues ->
                    NoteList(
                        notes = filteredNotes,
                        onItemClick = { note -> selectedNote = note },
                        onDeleteNote = { note -> notes.remove(note) },
                        modifier = Modifier.padding(paddingValues)
                    )
                }

                if (showDialog) {
                    AddNoteDialog(
                        onDismiss = { showDialog = false },
                        onAddNote = { note ->
                            notes.add(note.copy(multimediaFiles = multimediaFiles.toList()))
                            showDialog = false
                        },
                        dispatchTakePictureIntent = { dispatchTakePictureIntent() },
                        openGallery = { openGallery() }
                    )
                }

                selectedNote?.let { note ->
                    EditNoteDialog(
                        note = note,
                        onDismiss = { selectedNote = null },
                        onSaveNote = { updatedNote ->
                            notes[notes.indexOf(note)] = updatedNote
                        },
                        onDeleteNote = {
                            notes.remove(note)
                            selectedNote = null
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun NoteList(
        notes: List<Note>,
        onItemClick: (Note) -> Unit,
        onDeleteNote: (Note) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Column(modifier = modifier) {
            for (note in notes) {
                Row(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                    Text(note.title, modifier = Modifier.weight(1f).clickable { onItemClick(note) })
                    IconButton(onClick = { onDeleteNote(note) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar nota")
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AddNoteDialog(
        onDismiss: () -> Unit,
        onAddNote: (Note) -> Unit,
        dispatchTakePictureIntent: () -> Unit,
        openGallery: () -> Unit
    ) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var isTask by remember { mutableStateOf(false) }
        var dueDate by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Agregar Nota") },
            text = {
                Column {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título") }
                    )
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") }
                    )

                    Row {
                        Text("Es una tarea?")
                        Checkbox(checked = isTask, onCheckedChange = { isTask = it })
                    }

                    if (isTask) {
                        TextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = { Text("Fecha de vencimiento") }
                        )
                    }

                    Button(onClick = { dispatchTakePictureIntent() }) {
                        Text("Capturar Imagen")
                    }
                    Button(onClick = { openGallery() }) {
                        Text("Seleccionar desde Galería")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        onAddNote(
                            Note(
                                title = title,
                                description = description,
                                isTask = isTask,
                                dueDate = dueDate,
                                multimediaFiles = multimediaFiles.toList()
                            )
                        )
                    }
                }) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EditNoteDialog(
        note: Note,
        onDismiss: () -> Unit,
        onSaveNote: (Note) -> Unit,
        onDeleteNote: () -> Unit
    ) {
        var title by remember { mutableStateOf(note.title) }
        var description by remember { mutableStateOf(note.description) }
        var isTask by remember { mutableStateOf(note.isTask) }
        var dueDate by remember { mutableStateOf(note.dueDate ?: "") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Editar Nota") },
            text = {
                Column {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título") }
                    )
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") }
                    )

                    Row {
                        Text("Es una tarea?")
                        Checkbox(checked = isTask, onCheckedChange = { isTask = it })
                    }

                    if (isTask) {
                        TextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = { Text("Fecha de vencimiento") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        onSaveNote(note.copy(title = title, description = description, isTask = isTask, dueDate = dueDate))
                    }
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDeleteNote) {
                        Text("Eliminar")
                    }
                }
            }
        )
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Log.e("MainActivity", "Error creando archivo de imagen", ex)
                null
            }
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                captureImageLauncher.launch(takePictureIntent)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun openGallery() {
        pickMediaLauncher.launch("image/*")
    }
}
