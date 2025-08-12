# Meal Plan Overwrite Protection Implementation

## Overview

Added a confirmation dialog to prevent users from accidentally overwriting their existing incomplete meal plans when creating new ones.

## Logic Implementation

### 1. Meal Plan Creation Logic

**Before creating a new meal plan, the system checks:**

- **No existing meal plan**: Create directly (no dialog)
- **Existing INCOMPLETE meal plan**: Show warning dialog
- **Existing COMPLETED meal plan**: Create directly (no dialog)

### 2. User Flow Scenarios

#### Scenario A: New User (No Meal Plan)

1. User sets meal calories and taps "Create Meal Plan"
2. ✅ **Direct creation** - No dialog shown
3. New meal plan created and user navigates to home

#### Scenario B: User with Incomplete Meal Plan

1. User has existing meal plan (not marked complete)
2. User tries to create new meal plan
3. ⚠️ **Warning dialog appears**: "Replace Meal Plan?"
4. Message: "Are you sure you want to create this meal plan? If you do this, your previous meal plan data will be lost!"
5. Options:
   - **Cancel**: Dismiss dialog, keep existing plan
   - **Replace**: Confirm and create new plan (overwrites old one)

#### Scenario C: User with Completed Meal Plan

1. User has existing meal plan (marked as complete)
2. User tries to create new meal plan
3. ✅ **Direct creation** - No dialog shown (user can create new plans after completing)

## Code Changes

### MealPlanScreen.kt

#### Added State Management

```kotlin
var showOverwriteDialog by remember { mutableStateOf(false) }
```

#### Updated Create Button Logic

```kotlin
Button(onClick = {
    // Check if user has an incomplete meal plan
    val mealPlan = currentMealPlan
    if (mealPlan != null && !mealPlan.isCompleted) {
        // Show confirmation dialog for overwriting incomplete plan
        showOverwriteDialog = true
    } else {
        // No meal plan or completed meal plan - create directly
        // [Direct creation code]
    }
})
```

#### Added Overwrite Confirmation Dialog

```kotlin
if (showOverwriteDialog) {
    Dialog(onDismissRequest = { showOverwriteDialog = false }) {
        Card {
            Column {
                Icon(Icons.Default.Warning) // Warning icon
                Text("Replace Meal Plan?")
                Text("Are you sure you want to create this meal plan? If you do this, your previous meal plan data will be lost!")

                Row {
                    OutlinedButton("Cancel") // Dismisses dialog
                    Button("Replace") // Confirms and creates new plan
                }
            }
        }
    }
}
```

## User Experience Benefits

### 1. Data Protection

- **Prevents accidental loss** of incomplete meal plan data
- **Clear warning message** about data loss consequences
- **User control** with explicit confirmation required

### 2. Smart Behavior

- **No interruption** for new users creating their first plan
- **No interruption** for users who completed their plans (can create new ones)
- **Protection only** when there's risk of losing incomplete work

### 3. Clear Communication

- **Warning icon** (⚠️) to grab attention
- **Clear title**: "Replace Meal Plan?"
- **Explicit message** about data loss
- **Action-oriented buttons**: "Cancel" vs "Replace"

## Dialog Design

### Visual Elements

- **Warning Icon**: Red warning triangle to indicate caution
- **Error Color Scheme**: Replace button uses error color (red)
- **Clear Typography**: Bold title, readable body text
- **Balanced Layout**: Two equally-sized buttons

### Button Behavior

- **Cancel Button**:
  - Outlined style (less prominent)
  - Dismisses dialog without action
  - Preserves existing meal plan
- **Replace Button**:
  - Filled style with error color (red)
  - Confirms the destructive action
  - Creates new meal plan, overwrites old one

## Testing Scenarios

### ✅ Test Case 1: New User

1. Fresh user with no meal plans
2. Create meal plan → Direct creation, no dialog
3. **Expected**: Meal plan created immediately

### ✅ Test Case 2: Incomplete Meal Plan

1. User has existing incomplete meal plan
2. Try to create new meal plan → Warning dialog appears
3. Tap "Cancel" → Dialog dismisses, old plan preserved
4. **Expected**: No new plan created, old plan remains

### ✅ Test Case 3: Confirm Overwrite

1. User has existing incomplete meal plan
2. Try to create new meal plan → Warning dialog appears
3. Tap "Replace" → New plan created, old plan replaced
4. **Expected**: New plan created, old plan lost

### ✅ Test Case 4: Completed Meal Plan

1. User has existing completed meal plan
2. Create new meal plan → Direct creation, no dialog
3. **Expected**: New plan created immediately (user can create multiple completed plans)

## Implementation Status

- ✅ App builds successfully
- ✅ App installs without errors
- ✅ Smart logic prevents accidental overwrites
- ✅ Clear user communication about data loss
- ✅ Maintains smooth flow for appropriate scenarios
- ✅ Proper error handling and state management

The meal plan creation now has intelligent protection against accidental data loss while maintaining a smooth user experience!
