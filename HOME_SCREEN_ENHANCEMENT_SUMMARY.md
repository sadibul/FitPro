# Home Screen UI Enhancement Summary

## 🎯 What Was Implemented

### 1. **Redesigned Activity Stats Cards**

- ✅ **All three cards now have the same height and weight**
- ✅ **Icons and text centered in all cards**
- ✅ **Updated text labels**:
  - "Calories" → "Calories Burn"
  - "BPM" → "Calories Plan"
- ✅ **Enhanced clickable functionality**

### 2. **Calorie Target System**

- ✅ **Added `calorieTarget` field to UserProfile**
- ✅ **Database migration from v13 to v14**
- ✅ **Calories Plan card shows user's daily calorie goal**
- ✅ **Clickable Calories Plan card opens target setting dialog**

### 3. **Plan Page Enhancements**

- ✅ **Added "Daily Calorie Goal" setting card**
- ✅ **Shows current calorie target**
- ✅ **Click to open target setting dialog**
- ✅ **Professional form with input validation**

### 4. **Database Updates**

- ✅ **UserProfile.calorieTarget: Int = 0**
- ✅ **UserDao.updateCalorieTarget() method**
- ✅ **Safe migration preserving existing data**

## 🎨 UI/UX Improvements

### Card Design Consistency

```kotlin
ActivityStatCardEnhanced(
    modifier = Modifier
        .heightIn(min = 100.dp) // Match StepCounterCard height
        .weight(1f),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
)
```

### Centered Layout

- **Icons**: Centered with consistent size (24.dp)
- **Text**: Centered with proper typography
- **Values**: Bold, prominent display
- **Labels**: Clear, descriptive text

### Interactive Elements

- **Steps Card**: Click to set step target
- **Calories Burn Card**: Display only (can be enhanced later)
- **Calories Plan Card**: Click to set calorie target

## 🔧 Technical Implementation

### 1. Database Schema

```sql
-- Migration 13 to 14
ALTER TABLE user_profile ADD COLUMN calorieTarget INTEGER NOT NULL DEFAULT 0
```

### 2. UserDao Enhancement

```kotlin
@Query("UPDATE user_profile SET calorieTarget = :target WHERE email = :email")
suspend fun updateCalorieTarget(email: String, target: Int)
```

### 3. HomeScreen Updates

```kotlin
ActivityStatsSection(
    calorieTarget = userProfile?.calorieTarget ?: 0
)
```

### 4. Plan Page Integration

```kotlin
// Calorie Target Setting Card
Card(onClick = { showCalorieTargetDialog = true }) {
    "Current: ${userProfile?.calorieTarget ?: 0} calories"
}
```

## 📱 User Experience Flow

### Home Screen

1. **User sees three equal-sized cards**
2. **Steps card**: Shows progress with target
3. **Calories Burn card**: Shows daily burned calories
4. **Calories Plan card**: Shows daily calorie goal
5. **Click Calories Plan** → Opens target setting dialog

### Plan Screen

1. **User sees "Daily Calorie Goal" card**
2. **Shows current target value**
3. **Click to edit** → Opens professional dialog
4. **Set new target** → Updates database and home screen

### Dialog Experience

1. **Professional design** with rounded corners
2. **Clear instructions** and placeholders
3. **Number input validation**
4. **Cancel/Set Goal buttons**
5. **Immediate feedback** on home screen

## ✅ Features Delivered

- ✅ **Same card sizes** for all three stats
- ✅ **Centered icons and text**
- ✅ **Updated labels** (Calories Burn, Calories Plan)
- ✅ **Calorie target from Plan page**
- ✅ **Interactive calorie goal setting**
- ✅ **Database integration**
- ✅ **Professional UI/UX**
- ✅ **Data persistence**

## 🎉 Result

The home screen now has a **consistent, professional design** with three equal-sized cards that are **visually balanced** and **functionally complete**. Users can set their daily calorie goal from the Plan page, and it appears in the "Calories Plan" card on the home screen, creating a **seamless user experience**! 🚀
