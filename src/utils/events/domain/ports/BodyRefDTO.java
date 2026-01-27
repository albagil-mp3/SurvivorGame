package utils.events.domain.ports;

import model.bodies.ports.BodyType;

public record BodyRefDTO(String id, BodyType type) {
    public BodyRefDTO {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("EntityRef.id is required");
        if (type == null) throw new IllegalArgumentException("EntityRef.type is required");
    }
}