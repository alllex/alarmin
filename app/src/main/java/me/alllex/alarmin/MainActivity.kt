package me.alllex.alarmin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import me.alllex.alarmin.ui.theme.AlarminTheme
import java.time.Instant
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlarminTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AlarmPickerScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AlarmPickerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Compute the next 5-minute mark from "now"
    val now = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault())
    val currentHour = now.hour
    val currentMinute = now.minute
    val remainder = currentMinute % 5
    var roundedMinute = if (remainder == 0) currentMinute else currentMinute + (5 - remainder)
    var roundedHour = currentHour
    if (roundedMinute >= 60) {
        roundedMinute -= 60
        roundedHour = (roundedHour + 1) % 24
    }

    // Default selection for hour/min
    var selectedHour by remember { mutableStateOf(roundedHour.toString().padStart(2, '0')) }
    var selectedMinute by remember { mutableStateOf(roundedMinute.toString().padStart(2, '0')) }

    // Lists of possible hour/min choices
    val hours = (0..23).map { it.toString().padStart(2, '0') }
    val minutes = (0..55 step 5).map { it.toString().padStart(2, '0') }

    val squircleShape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Alarmin",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 4.dp),
            color = Color.Gray.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )

        Text(
            text = "$selectedHour:$selectedMinute",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .weight(1f)
        ) {
            HourSelector(
                hours = hours,
                selectedHour = selectedHour,
                onHourSelected = { selectedHour = it },
                squircleShape = squircleShape,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            MinuteAndActionColumn(
                minutes = minutes,
                selectedMinute = selectedMinute,
                onMinuteSelected = { selectedMinute = it },
                onSetAlarm = { note ->
                    scheduleUserAlarm(context, selectedHour, selectedMinute, note)
                },
                squircleShape = squircleShape,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Actually schedule the alarm with the chosen hour/minute and note.
 */
fun scheduleUserAlarm(context: Context, hour: String, minute: String, note: String) {
    // Trim length and add ellipsis if > 50
    var finalNote = note.trim()
    if (finalNote.length > 50) {
        finalNote = finalNote.take(47) + "..."
    }

    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
        putExtra(AlarmClock.EXTRA_HOUR, hour.toInt())
        putExtra(AlarmClock.EXTRA_MINUTES, minute.toInt())
        putExtra(AlarmClock.EXTRA_MESSAGE, finalNote)
        putExtra(AlarmClock.EXTRA_SKIP_UI, true)
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}

@Composable
fun HourSelector(
    hours: List<String>,
    selectedHour: String,
    onHourSelected: (String) -> Unit,
    squircleShape: RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(modifier = modifier.verticalScroll(scrollState)) {
        for (i in hours.indices step 2) {
            Row {
                listOf(hours[i], hours.getOrNull(i + 1)).forEach { hour ->
                    if (hour != null) {
                        SelectableButton(
                            label = hour,
                            isSelected = (hour == selectedHour),
                            onClick = { onHourSelected(hour) },
                            squircleShape = squircleShape,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun MinuteAndActionColumn(
    minutes: List<String>,
    selectedMinute: String,
    onMinuteSelected: (String) -> Unit,
    onSetAlarm: (String) -> Unit,
    squircleShape: RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val localFocusManager = LocalFocusManager.current
    val textFieldFocusRequester = remember { FocusRequester() }

    // 1) Access your SharedPreferences
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("alarmin", Context.MODE_PRIVATE)
    }

    // 2) Read the stored note once, the first time. If none is stored, default to ""
    // We'll hold the entire state in a TextFieldValue to keep selection logic consistent
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = prefs.getString("alarmNote", "") ?: ""
            )
        )
    }

    // 3) We track whether the field was focused before for auto-select logic
    var wasFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (i in minutes.indices step 2) {
            Row {
                listOf(minutes[i], minutes.getOrNull(i + 1)).forEach { minute ->
                    if (minute != null) {
                        SelectableButton(
                            label = minute,
                            isSelected = (minute == selectedMinute),
                            onClick = { onMinuteSelected(minute) },
                            squircleShape = squircleShape,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // OutlinedTextField for the alarm note
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue

                // 4) Save the updated note text to SharedPreferences
                prefs.edit {
                    putString("alarmNote", newValue.text)
                }
            },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            ),
            placeholder = {
                Text(
                    text = "note",
                    modifier = Modifier.fillMaxWidth(),
                    style = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    localFocusManager.clearFocus()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .focusRequester(textFieldFocusRequester)
                .onFocusChanged { focusState ->
                    // Auto-select text on first focus if there's text
                    if (focusState.isFocused && !wasFocused && textFieldValue.text.isNotEmpty()) {
                        val length = textFieldValue.text.length
                        textFieldValue = textFieldValue.copy(
                            selection = TextRange(0, length)
                        )
                    }
                    wasFocused = focusState.isFocused
                }
        )

        Button(
            onClick = {
                // Remove focus from the text field
                textFieldFocusRequester.freeFocus()
                // Then schedule the alarm
                onSetAlarm(textFieldValue.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(60.dp),
            shape = squircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Set alarm",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SelectableButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    squircleShape: RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val backgroundAlpha = if (isSelected) 0.2f else 0.0f

    OutlinedButton(
        onClick = onClick,
        shape = squircleShape,
        border = BorderStroke(
            width = borderWidth,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = backgroundAlpha),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(2f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}
