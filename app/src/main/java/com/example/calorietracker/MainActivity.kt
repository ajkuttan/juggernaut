package com.example.calorietracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS = "calorie_tracker_prefs"
private const val GOAL_KEY = "daily_goal"
private const val MEALS_KEY = "meals"

data class Meal(
    val name: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val store = TrackerStore(this)

        setContent {
            MaterialTheme {
                CalorieTrackerScreen(store)
            }
        }
    }
}

class TrackerStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadGoal(): Int = prefs.getInt(GOAL_KEY, 2000)

    fun saveGoal(goal: Int) {
        prefs.edit().putInt(GOAL_KEY, goal).apply()
    }

    fun loadMeals(): List<Meal> {
        val raw = prefs.getString(MEALS_KEY, "[]") ?: "[]"
        val json = JSONArray(raw)
        val meals = mutableListOf<Meal>()

        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            meals.add(
                Meal(
                    name = obj.optString("name"),
                    calories = obj.optInt("calories"),
                    protein = obj.optDouble("protein"),
                    carbs = obj.optDouble("carbs"),
                    fat = obj.optDouble("fat")
                )
            )
        }

        return meals
    }

    fun saveMeals(meals: List<Meal>) {
        val array = JSONArray()
        meals.forEach { meal ->
            array.put(
                JSONObject().apply {
                    put("name", meal.name)
                    put("calories", meal.calories)
                    put("protein", meal.protein)
                    put("carbs", meal.carbs)
                    put("fat", meal.fat)
                }
            )
        }
        prefs.edit().putString(MEALS_KEY, array.toString()).apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalorieTrackerScreen(store: TrackerStore) {
    var dailyGoalText by remember { mutableStateOf(store.loadGoal().toString()) }
    val meals = remember { mutableStateListOf<Meal>().apply { addAll(store.loadMeals()) } }

    var mealName by remember { mutableStateOf("") }
    var mealCalories by remember { mutableStateOf("") }
    var mealProtein by remember { mutableStateOf("0") }
    var mealCarbs by remember { mutableStateOf("0") }
    var mealFat by remember { mutableStateOf("0") }

    val goal = dailyGoalText.toIntOrNull()?.takeIf { it > 0 } ?: 2000
    val totalCalories = meals.sumOf { it.calories }
    val totalProtein = meals.sumOf { it.protein }
    val totalCarbs = meals.sumOf { it.carbs }
    val totalFat = meals.sumOf { it.fat }
    val progress = (totalCalories.toFloat() / goal.toFloat()).coerceIn(0f, 1f)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Calorie Tracker") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SummaryCard(
                    goal = goal,
                    calories = totalCalories,
                    protein = totalProtein,
                    carbs = totalCarbs,
                    fat = totalFat,
                    progress = progress
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Daily Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = dailyGoalText,
                            onValueChange = { dailyGoalText = it.filter(Char::isDigit) },
                            label = { Text("Calories per day") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = {
                            val parsed = dailyGoalText.toIntOrNull()
                            if (parsed != null && parsed > 0) {
                                store.saveGoal(parsed)
                            }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Save Goal")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Add Meal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = mealName, onValueChange = { mealName = it }, label = { Text("Meal name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = mealCalories, onValueChange = { mealCalories = it.filter(Char::isDigit) }, label = { Text("Calories") }, modifier = Modifier.fillMaxWidth())

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(value = mealProtein, onValueChange = { mealProtein = sanitizeDecimal(it) }, label = { Text("Protein") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = mealCarbs, onValueChange = { mealCarbs = sanitizeDecimal(it) }, label = { Text("Carbs") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = mealFat, onValueChange = { mealFat = sanitizeDecimal(it) }, label = { Text("Fat") }, modifier = Modifier.weight(1f))
                        }

                        Button(onClick = {
                            val calories = mealCalories.toIntOrNull()
                            if (mealName.isNotBlank() && calories != null && calories >= 0) {
                                meals.add(0, Meal(
                                    name = mealName.trim(),
                                    calories = calories,
                                    protein = mealProtein.toDoubleOrNull() ?: 0.0,
                                    carbs = mealCarbs.toDoubleOrNull() ?: 0.0,
                                    fat = mealFat.toDoubleOrNull() ?: 0.0
                                ))
                                store.saveMeals(meals)
                                mealName = ""
                                mealCalories = ""
                                mealProtein = "0"
                                mealCarbs = "0"
                                mealFat = "0"
                            }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Add Meal")
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Meal Log", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        meals.clear()
                        store.saveMeals(meals)
                    }) { Text("Clear all") }
                }
                Divider(modifier = Modifier.padding(top = 8.dp))
            }

            if (meals.isEmpty()) {
                item { Text("No meals logged yet.") }
            } else {
                itemsIndexed(meals) { index, meal ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(meal.name, fontWeight = FontWeight.Bold)
                                Text("${meal.calories} kcal")
                                Text("P ${meal.protein}g · C ${meal.carbs}g · F ${meal.fat}g")
                            }
                            IconButton(onClick = {
                                meals.removeAt(index)
                                store.saveMeals(meals)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete meal")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    goal: Int,
    calories: Int,
    protein: Double,
    carbs: Double,
    fat: Double,
    progress: Float
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Today's Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Calories: $calories / $goal kcal")
            Text("Remaining: ${(goal - calories).coerceAtLeast(0)} kcal")
            Text("Macros: P %.1fg · C %.1fg · F %.1fg".format(protein, carbs, fat))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp))
        }
    }
}

private fun sanitizeDecimal(input: String): String {
    val filtered = input.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    return if (firstDot == -1) filtered else {
        val withoutExtraDots = filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
        withoutExtraDots
    }
}
