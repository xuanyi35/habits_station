/*
 * Copyright (c) 2017 TeamX, CMPUT301, University of Alberta - All Rights Reserved.
 * You may use, distribute, or modify this code under terms and conditions of the Code of Student Behaviour at University of Alberta.
 * You can find a copy of lisense in this project. Otherwise please contact contact@abc.ca.
 */

package com.tiejun.habit_station;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by tiejun on 2017-10-13.
 */

public class HabitList{
    // list of all habits created
    private ArrayList<Habit> habits = new ArrayList<Habit>();

    public HabitList() {}

    public void add(Habit habit) {
        if (this.hasHabit(habit)) {
            throw new IllegalArgumentException("Duplicate habits.");
        }
        habits.add(habit);
    }

    public boolean hasHabit(Habit habit) {
        return habits.contains(habit);
    }

    public void delete(Habit habit) {
        habits.remove(habit);
    }

    public int getCount() {
        return habits.size();
    }

    public Habit getHabit(int index) {
        return habits.get(index);
    }

    class DateCompare implements Comparator<Habit> {
        public int compare(Habit habit, Habit t1) {
            if (habit.getStartDate().before(t1.getStartDate()))
                return 1;
            if (habit.getStartDate().after(t1.getStartDate()))
                return -1;
            return 0;

        }
    }

    public ArrayList<Habit> getHabits() {
        DateCompare compare = new DateCompare();
        Collections.sort(habits, compare);
        return habits;
    }

}