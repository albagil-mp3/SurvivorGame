package engine.controller.mappers;

import engine.model.weapons.ports.WeaponType;
import engine.world.ports.DefWeaponType;

public class WeaponTypeMapper {

    public static WeaponType fromWorldDef(DefWeaponType type) {
        if (type == null) {
            throw new IllegalArgumentException("DefWeaponType cannot be null");
        }

        return switch (type) {
            case BULLET_WEAPON -> WeaponType.PRIMARY_WEAPON;
            case BURST_WEAPON -> WeaponType.SECONDARY_WEAPON;
            case MINE_LAUNCHER -> WeaponType.MINE_LAUNCHER;
            case MISSILE_LAUNCHER -> WeaponType.MISSILE_LAUNCHER;
        };
    }
}
