package model.weapons.ports;

import model.emitter.ports.BodyEmittedDTO;

public interface Weapon {

    public double getAmmoStatus();

    public BodyEmittedDTO getProjectileConfig();

    public WeaponDto getWeaponConfig();

    public String getId();

    public void registerRequest();

    public boolean mustFireNow(double dtSeconds);
}
