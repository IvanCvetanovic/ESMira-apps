package at.jodlidev.esmira.views.inputViews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.jodlidev.esmira.sharedCode.data_structure.Input
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private data class StroopTrial(val word: String, val inkColor: String)

@Composable
fun StroopView(input: Input, get: () -> String, save: (String, Map<String, String>?) -> Unit) {
	val colors = remember {
		input.stroopColors.split(",").map { it.trim() }.filter { it.isNotEmpty() }
			.ifEmpty { listOf("red", "blue", "green", "yellow") }
	}
	val trials = remember {
		buildList {
			repeat(input.stroopTrials) {
				val inkColor = colors.random()
				val word = if (input.stroopShowWord) colors.random() else inkColor
				add(StroopTrial(word, inkColor))
			}
		}
	}

	val started = remember { mutableStateOf(false) }
	val currentTrialIndex = remember { mutableStateOf(0) }
	val trialStartTime = remember { mutableStateOf(System.currentTimeMillis()) }
	val results = remember { mutableStateListOf<JSONObject>() }
	val showingFixation = remember { mutableStateOf(false) }

	if (get().isNotEmpty()) {
		Box(
			modifier = Modifier.fillMaxWidth().padding(16.dp),
			contentAlignment = Alignment.Center
		) {
			Text(
				text = "Stroop test complete.",
				color = MaterialTheme.colorScheme.primary,
				style = MaterialTheme.typography.bodyMedium
			)
		}
		return
	}

	if (!started.value) {
		Column(
			modifier = Modifier.fillMaxWidth().padding(16.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text(
				text = "Stroop Test",
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSurface
			)
			Spacer(modifier = Modifier.height(8.dp))
			Text(
				text = input.stroopInstructions,
				fontSize = 13.sp,
				color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
			)
			Spacer(modifier = Modifier.height(12.dp))
			Button(onClick = {
				trialStartTime.value = System.currentTimeMillis()
				started.value = true
			}) {
				Text("Start")
			}
		}
		return
	}

	if (currentTrialIndex.value >= trials.size) return

	if (showingFixation.value) {
		LaunchedEffect(currentTrialIndex.value) {
			delay(500)
			trialStartTime.value = System.currentTimeMillis()
			showingFixation.value = false
		}
		Box(
			modifier = Modifier.fillMaxWidth().height(160.dp),
			contentAlignment = Alignment.Center
		) {
			Text("+", fontSize = 32.sp, color = MaterialTheme.colorScheme.onSurface)
		}
		return
	}

	val trial = trials[currentTrialIndex.value]

	Column(
		modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(
			text = "${currentTrialIndex.value + 1} / ${trials.size}",
			style = MaterialTheme.typography.labelMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Spacer(modifier = Modifier.height(24.dp))
		Box(modifier = Modifier.height(80.dp), contentAlignment = Alignment.Center) {
			if (input.stroopShowWord) {
				Text(
					text = trial.word.uppercase(),
					fontSize = 40.sp,
					fontWeight = FontWeight.Bold,
					color = stroopColor(trial.inkColor)
				)
			} else {
				Box(
					modifier = Modifier
						.size(120.dp, 50.dp)
						.background(stroopColor(trial.inkColor), RoundedCornerShape(8.dp))
				)
			}
		}
		Spacer(modifier = Modifier.height(32.dp))
		Row(
			modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
			horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
		) {
			for (color in colors) {
				Button(
					onClick = {
						val responseTime = System.currentTimeMillis() - trialStartTime.value
						val obj = JSONObject()
						obj.put("trial", currentTrialIndex.value + 1)
						obj.put("word", trial.word)
						obj.put("inkColor", trial.inkColor)
						obj.put("response", color)
						obj.put("responseTime", responseTime)
						obj.put("correct", color == trial.inkColor)
						results.add(obj)

						if (currentTrialIndex.value + 1 >= trials.size) {
							val correctCount = results.count { it.optBoolean("correct") }
							val accuracy = correctCount * 100 / results.size
							val meanRT = results.map { it.optLong("responseTime") }.average().toLong()
							val jsonArray = JSONArray()
							results.forEach { jsonArray.put(it) }
							save(
								jsonArray.toString(),
								mapOf("accuracy" to accuracy.toString(), "meanRT" to meanRT.toString())
							)
						} else {
							currentTrialIndex.value++
							showingFixation.value = true
						}
					},
					colors = ButtonDefaults.buttonColors(containerColor = stroopColor(color)),
					contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
					modifier = Modifier.weight(1f)
				) {
					Text(
						text = color.replaceFirstChar { it.uppercase() },
						color = Color.White,
						style = MaterialTheme.typography.labelMedium
					)
				}
			}
		}
	}
}

private fun stroopColor(name: String): Color = when (name.lowercase()) {
	"red" -> Color(0xFFE53935)
	"blue" -> Color(0xFF1E88E5)
	"green" -> Color(0xFF43A047)
	"yellow" -> Color(0xFFFDD835)
	"purple" -> Color(0xFF8E24AA)
	"orange" -> Color(0xFFFB8C00)
	"pink" -> Color(0xFFE91E63)
	"cyan" -> Color(0xFF00ACC1)
	else -> Color.Gray
}
