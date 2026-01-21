package model.weapons.ports;

import events.domain.ports.BodyToEmitDTO;

public interface Weapon {

    public double getAmmoStatus();

    public BodyToEmitDTO getProjectileConfig();

    public WeaponDto getWeaponConfig();

    public String getId();

    public void registerRequest();

    public boolean mustFireNow(double dtSeconds);
}
