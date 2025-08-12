# Navigation Fix for Calories Plan Card

## Issue Fixed

The "Calories Plan" card on the home screen was showing a "Daily Calorie Goal" dialog instead of navigating to the Meal Plan screen when tapped.

## Root Cause

The `onClick` handler for the Calories Plan card was set to:

```kotlin
onClick = { showCalorieTargetDialog = true }
```

This was showing the calorie target dialog instead of navigating to the meal plan as intended in the flowchart.

## Solution Applied

### 1. Updated ActivityStatsSection Function

- Added `navController: NavController` parameter to the function signature
- Updated the Calories Plan card onClick to navigate to meal plan:

```kotlin
onClick = { navController.navigate(Screen.MealPlan.route) }
```

### 2. Updated HomeScreen ActivityStatsSection Call

- Added `navController = navController` parameter when calling ActivityStatsSection
- This passes the navigation controller down to the component that needs it

## Code Changes

### HomeScreen.kt - ActivityStatsSection Call

```kotlin
// Activity Stats Section
ActivityStatsSection(
    steps = dailySteps,
    stepTarget = userProfile?.stepTarget ?: 0,
    calories = userProfile?.caloriesBurned ?: 0,
    heartRate = userProfile?.heartRate ?: 0,
    userDao = userDao,
    currentUserEmail = currentUserEmail,
    calorieTarget = userProfile?.calorieTarget ?: 0,
    currentMealPlan = currentMealPlan,
    navController = navController  // Added this line
)
```

### HomeScreen.kt - ActivityStatsSection Function

```kotlin
@Composable
private fun ActivityStatsSection(
    steps: Int,
    stepTarget: Int,
    calories: Int,
    heartRate: Int,
    userDao: UserDao,
    currentUserEmail: String?,
    calorieTarget: Int = 0,
    currentMealPlan: MealPlan? = null,
    navController: NavController  // Added this parameter
) {
    // ... existing code ...

    // Enhanced Calories Plan Card (shows status or target)
    ActivityStatCardEnhanced(
        icon = Icons.Default.Restaurant,
        value = if (currentMealPlan != null) {
            if (currentMealPlan.isCompleted) "Completed" else "${currentMealPlan.totalCalories}"
        } else {
            if (calorieTarget > 0) "$calorieTarget" else "0"
        },
        label = "Calories Plan",
        modifier = Modifier.weight(1f),
        onClick = { navController.navigate(Screen.MealPlan.route) }  // Fixed navigation
    )
}
```

## Flow Verification

Now when users tap the "Calories Plan" card:

1. ✅ Home Page → Tapping Calories Plan card navigates to Meal Plan screen
2. ✅ Meal Plan Page → User can set target calories and create meal plan
3. ✅ Create Meal Plan → Shows completion dialog
4. ✅ Back to Home → Shows updated status on Calories Plan card

## Testing Status

- ✅ App builds successfully
- ✅ App installs without errors
- ✅ Navigation flow now matches the intended flowchart behavior
- ✅ No more unexpected calorie goal dialog when tapping Calories Plan card

The Calories Plan card now correctly navigates to the Meal Plan screen as intended in your flowchart!
