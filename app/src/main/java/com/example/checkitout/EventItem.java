package com.example.checkitout;

public class EventItem {
    private final String date;
    private final String eventName;

    public EventItem(String date, String eventName) {
        this.date = date;
        this.eventName = eventName;
    }

    public String getDate() {
        return date;
    }

    public String getEventName() {
        return eventName;
    }
}
