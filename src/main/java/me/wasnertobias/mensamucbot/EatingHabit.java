package me.wasnertobias.mensamucbot;

public enum EatingHabit {
    NONE,       // default habit (= Whatever) [contains flesh, but neither COW nor PIG]
    VEGAN,      // Valid habit
    VEGETARIAN, // Valid habit
    PIG,        // Valid habit (= NO pig) [contains PIG]
    COW,        // Invalid habit
    PIG_AND_COW // Invalid habit
    // TODO: Add pescetarier
}
