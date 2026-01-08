package model.ports;


import model.bodies.ports.BodyType;


public class EventDTO {

    public final BodyType body1Type;
    public final BodyType body2Type;
    public final EventType eventType;
    
    public EventDTO(BodyType body1Type, BodyType body2Type, EventType eventType){
        this.body1Type = body1Type;
        this.body2Type = body2Type;
        this.eventType = eventType;
    }
}
