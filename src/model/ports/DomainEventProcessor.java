package model.ports;

import java.util.List;

import events.domain.ports.eventtype.DomainEvent;

public interface DomainEventProcessor {

    public void decideActions(List<DomainEvent> domainEvents, List<ActionDTO> actions);

    public void notifyNewDynamic(String entityId, String assetId);

    public void notifyNewStatic(String entityId, String assetId);

    public void notiyDynamicIsDead(String entityId);

    public void notifyPlayerIsDead(String entityId);

    public void notiyStaticIsDead(String entityId);

}
