package at.jodlidev.esmira.views.inputViews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.jodlidev.esmira.sharedCode.data_structure.Input
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CreativityView(input: Input, get: () -> String, save: (String, Map<String, String>?) -> Unit) {
	val context = LocalContext.current
	val prefs = context.getSharedPreferences("esmira_creativity", android.content.Context.MODE_PRIVATE)

	val duration = input.creativityDuration
	val wordPool = remember {
		input.creativityWords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
			.ifEmpty {
				listOf("Toothpaste","Foam Toothbrush","Wine bottle","Ping pong ball","Headband",
					"Mirror","Sock","Rope","Suitcase","Umbrella","Paintbrush","Band-aid",
					"Trash can","Spoon","Ruler","Lamp","Tie","Toilet paper","Coat hook",
					"Car tire","Bathtub","Banana peel","Flower vase","Iron","Paper clip",
					"Lighter","Hairdryer","Frisbee","Fork","Watering can","Bell","Belt",
					"Horseshoe","Hat")
			}
	}

	val word = remember {
		val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
		val key = "used_words_$today"
		val usedRaw = prefs.getString(key, "") ?: ""
		val used = if (usedRaw.isEmpty()) emptyList() else usedRaw.split("|")
		val available = wordPool.filter { it !in used }
		val pool = if (available.isEmpty()) wordPool else available
		val picked = pool.random()
		val updated = if (available.isEmpty()) listOf(picked) else used + picked
		prefs.edit().putString(key, updated.joinToString("|")).apply()
		picked
	}

	val started = remember { mutableStateOf(false) }
	val remaining = remember { mutableStateOf(duration) }
	val ended = remember { mutableStateOf(false) }
	val textValue = remember { mutableStateOf("") }
	val focusRequester = remember { FocusRequester() }

	if (get().isNotEmpty()) {
		Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
			Text("Creativity test complete.", color = MaterialTheme.colorScheme.primary)
		}
		return
	}

	if (!started.value) {
		Column(
			modifier = Modifier.fillMaxWidth().padding(16.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text(
				text = input.creativityInstructions,
				fontSize = 13.sp,
				color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
				modifier = Modifier.padding(bottom = 12.dp)
			)
			Text("You will have $duration seconds once you start.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
			Spacer(modifier = Modifier.height(12.dp))
			Button(onClick = { started.value = true }) { Text("Start") }
		}
		return
	}

	LaunchedEffect(Unit) {
		focusRequester.requestFocus()
		while (remaining.value > 0 && !ended.value) {
			delay(1000)
			remaining.value--
		}
		if (!ended.value) {
			ended.value = true
			save(textValue.value, mapOf("word" to word))
		}
	}

	val timerColor = if (remaining.value <= 3) MaterialTheme.colorScheme.error
	else MaterialTheme.colorScheme.primary
	val progress = remaining.value.toFloat() / duration.toFloat()

	Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
		LinearProgressIndicator(
			progress = { progress.coerceIn(0f, 1f) },
			modifier = Modifier.fillMaxWidth(),
			color = timerColor,
		)
		Spacer(modifier = Modifier.height(12.dp))
		Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
			Text(
				text = input.creativityInstructions,
				fontSize = 13.sp,
				color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
			)
			Text(
				text = "${remaining.value}s",
				fontSize = 18.sp,
				fontWeight = FontWeight.Bold,
				color = timerColor
			)
		}
		Spacer(modifier = Modifier.height(12.dp))
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
					.padding(vertical = 20.dp, horizontal = 24.dp),
			contentAlignment = Alignment.Center
		) {
			Text(word, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
		}
		Spacer(modifier = Modifier.height(16.dp))
		OutlinedTextField(
			value = textValue.value,
			onValueChange = { textValue.value = it },
			modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp).focusRequester(focusRequester),
			placeholder = { Text("Write as many creative uses as you can...") },
			maxLines = Int.MAX_VALUE,
			enabled = !ended.value
		)
	}
}
