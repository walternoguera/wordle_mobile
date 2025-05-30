package com.example.wordlemovil

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.example.wordlemovil.ui.theme.WordleMovilTheme
import java.util.Locale

//variables globales para guardar nombre, intento actual, palabra objetivo y estado del juego
var campoNombre by mutableStateOf("")
var intentoActual by mutableStateOf(0)
val intentosMaximos = 5
var palabraObjetivo by mutableStateOf("")
val palabras = listOf("raton", "flota", "lento", "pasto", "coche", "papel", "lamas", "tigre", "fuego", "calor")
var juegoActivo by mutableStateOf(false)
var valoresGrid = List(5) { MutableList(5) { mutableStateOf("") } }
var coloresGrid = List(5) { MutableList(5) { mutableStateOf(Color.White) } }

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) {
        //esto hace que por defecto la app arranque en español
        val locale = Locale("es")
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        val context = base.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WordleMovilTheme {
                PantallaWordle()
            }
        }
    }
}

@Composable
fun DropdownMenuIdioma(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    //menu desplegable para cambiar entre español e inglés
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("es", "en")
    val labels = mapOf("es" to "Español", "en" to "English")

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(labels[selectedLanguage] ?: selectedLanguage)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(labels[lang] ?: lang) },
                    onClick = {
                        expanded = false
                        onLanguageSelected(lang)
                    }
                )
            }
        }
    }
}

@Composable
fun PantallaWordle() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val activity = context as Activity

    //guarda idioma y mensaje en caso de rotación
    var selectedLanguage by rememberSaveable { mutableStateOf("es") }
    var mensajeEstado by remember { mutableStateOf("") }

    //esta función reinicia la actividad y cambia el idioma
    fun reiniciarIdioma(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        activity.finish()
        activity.startActivity(Intent(context, activity::class.java))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            //menu para cambiar idioma
            DropdownMenuIdioma(selectedLanguage) { newLang ->
                selectedLanguage = newLang
                reiniciarIdioma(newLang)
            }

            //input para poner el nombre del jugador
            TextField(
                value = campoNombre,
                onValueChange = { campoNombre = it },
                label = { Text(context.getString(R.string.introduce_nombre)) }, //viene de strings.xml
                modifier = Modifier.fillMaxWidth()
            )

            //boton comenzar
            Button(
                onClick = {
                    if (campoNombre.isNotBlank()) {
                        palabraObjetivo = palabras.random()
                        intentoActual = 0
                        juegoActivo = true
                        mensajeEstado = context.getString(R.string.mensaje_vamos_adivina, campoNombre) //de strings.xml
                        for (f in 0..4) for (c in 0..4) {
                            valoresGrid[f][c].value = ""
                            coloresGrid[f][c].value = Color.White
                        }
                    } else {
                        mensajeEstado = context.getString(R.string.mensaje_falta_nombre) //de strings.xml
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(context.getString(R.string.boton_comenzar)) //de strings.xml
            }

            //grid del juego, es de (5x5)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (f in 0..4) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        for (c in 0..4) {
                            TextField(
                                value = valoresGrid[f][c].value,
                                onValueChange = {
                                    if (it.length <= 1 && f == intentoActual && juegoActivo)
                                        valoresGrid[f][c].value = it.uppercase()
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = coloresGrid[f][c].value,
                                    unfocusedContainerColor = coloresGrid[f][c].value,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(60.dp)
                                    .border(
                                        width = 2.dp,
                                        brush = SolidColor(Color.DarkGray),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }
                }
            }

            //botón validar intento
            Button(
                onClick = {
                    focusManager.clearFocus()
                    val intento = valoresGrid[intentoActual].joinToString("") { it.value.lowercase() }

                    if (intento.length != 5 || intento.any { !it.isLetter() }) {
                        mensajeEstado = context.getString(R.string.mensaje_completa_letras) //de strings.xml
                        return@Button
                    }

                    if (!palabras.contains(intento)) {
                        mensajeEstado = context.getString(R.string.mensaje_palabra_no_valida) //de strings.xml
                        return@Button
                    }

                    for (i in 0..4) {
                        val letra = intento[i].toString()
                        coloresGrid[intentoActual][i].value = when {
                            letra == palabraObjetivo[i].toString() -> Color(0xFFAAF683)
                            palabraObjetivo.contains(letra) -> Color(0xFFFFD166)
                            else -> Color(0xFFE0E0E0)
                        }
                    }

                    if (intento == palabraObjetivo) {
                        mensajeEstado = context.getString(R.string.mensaje_gano, campoNombre) //de strings.xml
                        juegoActivo = false
                        return@Button
                    }

                    intentoActual++
                    if (intentoActual == intentosMaximos) {
                        mensajeEstado = context.getString(R.string.mensaje_perdio, palabraObjetivo) //de strings.xml
                        juegoActivo = false
                    } else {
                        mensajeEstado = context.getString(R.string.mensaje_intento, intentoActual + 1, intentosMaximos) //de strings.xml
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = juegoActivo
            ) {
                Text(context.getString(R.string.boton_validar)) //de strings.xml
            }

            //botón reiniciar juego
            Button(
                onClick = {
                    palabraObjetivo = palabras.random()
                    intentoActual = 0
                    juegoActivo = true
                    mensajeEstado = context.getString(R.string.mensaje_reiniciar, campoNombre) //de strings.xml
                    for (f in 0..4) for (c in 0..4) {
                        valoresGrid[f][c].value = ""
                        coloresGrid[f][c].value = Color.White
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = campoNombre.isNotBlank()
            ) {
                Text(context.getString(R.string.boton_reiniciar)) //de strings.xml
            }

            //mensaje de estado: dice lo que esta pasando (gano, perdio, falta nombre, etc)
            Text(mensajeEstado, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
