package controller.mappers;

import model.weapons.ports.WeaponDto;
import world.ports.WorldDefWeaponDTO;

public class WeaponMapper {

    public static WeaponDto fromWorldDef(WorldDefWeaponDTO weaponDef, int shootingOffset) {
        if (weaponDef == null) {
            return null;
        }
        return new WeaponDto(
            WeaponTypeMapper.fromWorldDef(weaponDef.weaponType),
                weaponDef.assetId,
                weaponDef.projectileSize,
                weaponDef.projectileSpeed,
                weaponDef.projectileAcc,
                weaponDef.projectileAccDuration,
                weaponDef.burstSize,
                weaponDef.burstFireRate,
                weaponDef.fireRate,
                weaponDef.maxAmmo,
                weaponDef.reloadTime,
                weaponDef.projectileMass,
                weaponDef.maxLifetimeInSeconds,
                shootingOffset
        );   
    }
} 