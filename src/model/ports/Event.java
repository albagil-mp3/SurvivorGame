package model.ports;

import model.bodies.ports.Body;

public class Event {

    public final Body primaryBody;
    public final Body secondaryBody;
    public final EventType eventType;

    public Event(Body primaryBody, Body secondaryBody, EventType eventType) {
        this.primaryBody = primaryBody;
        this.secondaryBody = secondaryBody;
        this.eventType = eventType;
    }
}

