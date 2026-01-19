package model.emitter.ports;

import model.emitter.implementations.BasicEmitter;
import model.physics.ports.PhysicsValuesDTO;

public interface Emitter {

    public BodyEmittedDTO getBodyEmittedConfig();

    public BasicEmitter getEmitter();

    public boolean mustEmitNow(PhysicsValuesDTO newPhyValues);

    public void registerEmmitRequest();

    public void setEmitter(BasicEmitter emitter);

}
