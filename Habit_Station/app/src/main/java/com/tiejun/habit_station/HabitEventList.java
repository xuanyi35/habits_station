/*
 * Copyright (c) 2017 Team 24,CMPUT301, University of Alberta - All Rights Reserved.
 * You mayuse,distribute, or modify thid code under terms and condition of the Code of Student Behavior at University of Alberta.
 * You can find a copy of the license in this project. Otherwise please contact xuanyi@ualberta.ca.
 *
 */

package com.tiejun.habit_station;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by XuanyiWu on 2017-10-22.
 */

public class HabitEventList {
    private ArrayList<HabitEvent> events = new ArrayList<HabitEvent>();

    public HabitEventList() {}

    public void add(HabitEvent event) {
        events.add(event);
    }

    public boolean hasHabit(HabitEvent event) {
        return events.contains(event);
    }

    public void delete(HabitEvent event) {
        events.remove(event);
    }

    public int getCount() {
        return events.size();
    }

    public HabitEvent getEvent(int index) {
        return events.get(index);
    }

    class EventDateCompare implements Comparator<HabitEvent> {
        public int compare(HabitEvent event, HabitEvent e1) {
            if (event.geteTime().before(e1.geteTime()))
                return 1;
            if (event.geteTime().before(e1.geteTime()))
                return -1;
            return 0;
        }
    }

    public ArrayList<HabitEvent> sortEvents() {
        EventDateCompare compare = new EventDateCompare();
        Collections.sort(events, compare);
        return events;
    }


}