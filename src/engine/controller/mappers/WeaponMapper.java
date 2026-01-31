package engine.controller.mappers;

import engine.model.weapons.ports.WeaponDto;
import engine.world.ports.DefWeaponDTO;

public class WeaponMapper {

    public static WeaponDto fromWorldDef(DefWeaponDTO weaponDef, int shootingOffset) {

        if (weaponDef == null) {
            return null;
        }

        return new WeaponDto(
                WeaponTypeMapper.fromWorldDef(weaponDef.weaponType),
                weaponDef.assetId,

                weaponDef.projectileSize,
                weaponDef.projectileSpeed,
                weaponDef.projectileThrust,
                weaponDef.projectileThrustDuration,

                weaponDef.burstSize,
                weaponDef.burstFireRate,
                weaponDef.fireRate,

                weaponDef.maxAmmo,
                weaponDef.reloadTime,

                weaponDef.projectileMass,
                weaponDef.projectileMaxLifetime,

                shootingOffset
        );
    }
}
