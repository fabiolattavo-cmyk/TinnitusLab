package com.example.tinnitusnotch

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.*
import kotlin.random.Random

class TinnitusAudioEngine {
    var freqSx = 8000f; var freqDx = 8000f
    var tipoOndaSx = "Sinusoidale"; var tipoOndaDx = "Sinusoidale"
    var modalitaTerapia = true; var tipoRumore = "Marrone"; var isPulsato = false

    private val sampleRate = 44100; private var audioTrack: AudioTrack? = null; private var isPlaying = false
    private var b0_Sx = 1.0; private var b1_Sx = 0.0; private var b2_Sx = 0.0; private var a1_Sx = 0.0; private var a2_Sx = 0.0
    private var b0_Dx = 1.0; private var b1_Dx = 0.0; private var b2_Dx = 0.0; private var a1_Dx = 0.0; private var a2_Dx = 0.0
    private var x1_Sx = 0.0; private var x2_Sx = 0.0; private var y1_Sx = 0.0; private var y2_Sx = 0.0
    private var x1_Dx = 0.0; private var x2_Dx = 0.0; private var y1_Dx = 0.0; private var y2_Dx = 0.0

    fun calcolaCoefficientiNotch() {
        val omegaSx = 2.0 * PI * freqSx / sampleRate; val alphaSx = tan(omegaSx / 24.0); val a0_Sx = 1.0 + alphaSx
        b0_Sx = 1.0 / a0_Sx; b1_Sx = -2.0 * cos(omegaSx) / a0_Sx; b2_Sx = 1.0 / a0_Sx; a1_Sx = -2.0 * cos(omegaSx) / a0_Sx; a2_Sx = (1.0 - alphaSx) / a0_Sx
        val omegaDx = 2.0 * PI * freqDx / sampleRate; val alphaDx = tan(omegaDx / 24.0); val a0_Dx = 1.0 + alphaDx
        b0_Dx = 1.0 / a0_Dx; b1_Dx = -2.0 * cos(omegaDx) / a0_Dx; b2_Dx = 1.0 / a0_Dx; a1_Dx = -2.0 * cos(omegaDx) / a0_Dx; a2_Dx = (1.0 - alphaDx) / a0_Dx
    }

    private fun generaOnda(fase: Double, tipo: String): Double {
        return when (tipo) {
            "Sinusoidale" -> sin(fase)
            "Quadra" -> if (sin(fase) >= 0) 0.12 else -0.12
            "Triangolare" -> (2.0 / PI) * asin(sin(fase))
            else -> sin(fase)
        }
    }

    fun avvia() {
        if (isPlaying) return; isPlaying = true; calcolaCoefficientiNotch()
        val bufSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        audioTrack = AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufSize, AudioTrack.MODE_STREAM)
        audioTrack?.play()

        Thread {
            var fSx = 0.0; var fDx = 0.0; var cp = 0; var on = true; val lp = sampleRate * 0.35
            var p0 = 0.0; var p1 = 0.0; var p2 = 0.0; var stateSea = 0.0
            val buffer = ShortArray(bufSize)

            while (isPlaying) {
                for (i in 0 until buffer.size step 2) {
                    if (isPulsato) { if (++cp >= lp) { on = !on; cp = 0 } } else { on = true }
                    fSx += 2.0 * PI * freqSx / sampleRate; fDx += 2.0 * PI * freqDx / sampleRate
                    var outSx = 0.0; var outDx = 0.0

                    if (!modalitaTerapia) {
                        outSx = if (on) generaOnda(fSx, tipoOndaSx) else 0.0
                        outDx = if (on) generaOnda(fDx, tipoOndaDx) else 0.0
                    } else {
                        val wSx = Random.nextFloat() * 2.0 - 1.0; val wDx = Random.nextFloat() * 2.0 - 1.0
                        when (tipoRumore) {
                            "Marrone" -> { outSx = (x1_Sx + 0.02 * wSx) / 1.02; outDx = (x1_Dx + 0.02 * wDx) / 1.02; x1_Sx = outSx; x1_Dx = outDx; outSx *= 3.5; outDx *= 3.5 }
                            "Rosa", "Pioggia" -> { p0 = 0.99886 * p0 + wSx * 0.0555179; p1 = 0.99332 * p1 + wSx * 0.0750759; p2 = 0.96900 * p2 + wSx * 0.1538520; outSx = (p0 + p1 + p2 + wSx * 0.5362) * 0.25; outDx = outSx }
                            "Bianco" -> { outSx = wSx * 0.2; outDx = wDx * 0.2 }
                            "Viola" -> { outSx = (wSx - x1_Sx) * 0.4; x1_Sx = wSx; outDx = (wDx - x1_Dx) * 0.4; x1_Dx = wDx }
                            "Mare" -> { stateSea += 0.0001; val mod = (sin(stateSea) + 1.0) / 2.0; outSx = (x1_Sx + 0.02 * wSx) / 1.02; x1_Sx = outSx; outSx *= 3.5 * mod; outDx = outSx }
                            "Frattale" -> { val modF = (sin(fSx * 0.01) + cos(fDx * 0.005)) * 0.1; outSx = sin(fSx) * modF; outDx = cos(fDx) * modF }
                        }
                        val fSxOut = b0_Sx * outSx + b1_Sx * x1_Sx + b2_Sx * x2_Sx - a1_Sx * y1_Sx - a2_Sx * y2_Sx; x2_Sx = x1_Sx; x1_Sx = outSx; y2_Sx = y1_Sx; y1_Sx = fSxOut; outSx = fSxOut
                        val fDxOut = b0_Dx * outDx + b1_Dx * x1_Dx + b2_Dx * x2_Dx - a1_Dx * y1_Dx - a2_Dx * y2_Dx; x2_Dx = x1_Dx; x1_Dx = outDx; y2_Dx = y1_Dx; y1_Dx = fDxOut; outDx = fDxOut
                        if (!on) { outSx = 0.0; outDx = 0.0 }
                    }
                    buffer[i] = (outSx * 18000).toInt().coerceIn(-32768, 32767).toShort()
                    buffer[i + 1] = (outDx * 18000).toInt().coerceIn(-32768, 32767).toShort()
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }.start()
    }

    fun ferma() { isPlaying = false; audioTrack?.stop(); audioTrack?.release(); audioTrack = null }
}
class MainActivity : ComponentActivity() {
    private val engine = TinnitusAudioEngine()
    private var isPlayingState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("TinnitusPrefs", Context.MODE_PRIVATE)
        engine.freqSx = prefs.getFloat("freqSx", 8000f); engine.freqDx = prefs.getFloat("freqDx", 8000f)
        engine.tipoOndaSx = prefs.getString("ondaSx", "Sinusoidale") ?: "Sinusoidale"
        engine.tipoOndaDx = prefs.getString("ondaDx", "Sinusoidale") ?: "Sinusoidale"
        engine.tipoRumore = prefs.getString("rumore", "Marrone") ?: "Marrone"
        engine.isPulsato = prefs.getBoolean("pulsato", false)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) { InterfacciaNativa(prefs) }
            }
        }
    }

    @Composable
    fun InterfacciaNativa(prefs: android.content.SharedPreferences) {
        var inputSx by remember { mutableStateOf(engine.freqSx.toInt().toString()) }
        var inputDx by remember { mutableStateOf(engine.freqDx.toInt().toString()) }
        var waveSx by remember { mutableStateOf(engine.tipoOndaSx) }
        var waveDx by remember { mutableStateOf(engine.tipoOndaDx) }
        var noiseType by remember { mutableStateOf(engine.tipoRumore) }
        var pulseActive by remember { mutableStateOf(engine.isPulsato) }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🛡️ Tinnitus Lab Pro", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("ORECCHIO SX", color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(value = inputSx, onValueChange = { inputSx = it; it.toFloatOrNull()?.let { f -> engine.freqSx = f; prefs.edit().putFloat("freqSx", f).apply() } }, label = { Text("Hz") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Row { listOf("Sinusoidale", "Triangolare", "Quadra").forEach { t -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = (waveSx == t), onClick = { waveSx = t; engine.tipoOndaSx = t; prefs.edit().putString("ondaSx", t).apply() }); Text(t, style = MaterialTheme.typography.bodySmall) } } }
                }
            }

            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("ORECCHIO DX", color = MaterialTheme.colorScheme.secondary)
                    OutlinedTextField(value = inputDx, onValueChange = { inputDx = it; it.toFloatOrNull()?.let { f -> engine.freqDx = f; prefs.edit().putFloat("freqDx", f).apply() } }, label = { Text("Hz") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Row { listOf("Sinusoidale", "Triangolare", "Quadra").forEach { t -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = (waveDx == t), onClick = { waveDx = t; engine.tipoOndaDx = t; prefs.edit().putString("ondaDx", t).apply() }); Text(t, style = MaterialTheme.typography.bodySmall) } } }
                }
            }

            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("LIBRERIA SUONI TERAPEUTICI")
                    Row { listOf("Marrone", "Rosa", "Bianco", "Viola").forEach { r -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = (noiseType == r), onClick = { noiseType = r; engine.tipoRumore = r; prefs.edit().putString("rumore", r).apply() }); Text(r, style = MaterialTheme.typography.bodySmall) } } }
                    Row { listOf("Pioggia", "Mare", "Frattale").forEach { r -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = (noiseType == r), onClick = { noiseType = r; engine.tipoRumore = r; prefs.edit().putString("rumore", r).apply() }); Text(r, style = MaterialTheme.typography.bodySmall) } } }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = pulseActive, onCheckedChange = { pulseActive = it; engine.isPulsato = it; prefs.edit().putBoolean("pulsato", it).apply() }); Text("Attiva Suono Pulsato") }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { engine.modalitaTerapia = false; engine.ferma(); engine.avvia(); isPlayingState = true }) { Text("TEST FISCHI") }
                Button(onClick = { engine.modalitaTerapia = true; engine.ferma(); engine.avvia(); isPlayingState = true }) { Text("AVVIA TERAPIA") }
            }
            if (isPlayingState) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { engine.ferma(); isPlayingState = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("SPEGNI") }
            }
        }
    }
}
