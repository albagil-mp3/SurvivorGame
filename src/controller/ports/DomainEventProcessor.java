package controller.ports;

import java.util.List;

import model.ports.ActionDTO;
import model.ports.Event;

public interface DomainEventProcessor {

    public void notifyNewProjectileFired(String entityId, String assetId);

    public List<ActionDTO> decideActions(List<Event> events);
}
