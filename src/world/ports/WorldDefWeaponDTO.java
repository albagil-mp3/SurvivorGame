package world.ports;

public class WorldDefWeaponDTO extends WorldDefItemDTO {

    // region Fields
    public final int burstSize; // Number of shots per burst
    public final int burstFireRate; // Fire rate within a burst (shots per second)
    public final int fireRate; // Fire rate (shots per second)
    public final int maxAmmo; // Maximum ammunition capacity
    public final double reloadTime; // Reload time (seconds)
    public final WorldDefWeaponType weaponType; // Weapon type

    public final double projectileAcc; // Projectile acceleration (if applicable)
    public final double projectileAccDuration; // Time during which the acceleration applies
    public final double projectileAngle; // Angle of the projectile
    public final double projectileMass; // Mass of the projectile (kilograms)
    public final double projectileMaxLifetime; // Maximum lifetime of the projectile (seconds)
    public final double projectileSize; // Size of the projectile
    public final double projectileSpeed; // Projectile initial speed
    // endregion

    // *** CONSTRUCTOR ***

    public WorldDefWeaponDTO(String assetId, double projectileSize, double projectileAngle,
            WorldDefWeaponType weaponType,
            double projectileSpeed, double projectileAcc, double projectileAccDuration,
            int burstSize, int burstFireRate, int fireRate, int maxAmmo, double reloadTime,
            double projectileMass, double projectileMaxLifetime) {

        super(assetId);

        this.burstSize = burstSize;
        this.burstFireRate = burstFireRate;
        this.fireRate = fireRate;
        this.maxAmmo = maxAmmo;
        this.reloadTime = reloadTime;
        this.weaponType = weaponType;

        this.projectileAcc = projectileAcc;
        this.projectileAccDuration = projectileAccDuration;
        this.projectileAngle = projectileAngle;
        this.projectileMass = projectileMass;
        this.projectileMaxLifetime = projectileMaxLifetime;
        this.projectileSize = projectileSize;
        this.projectileSpeed = projectileSpeed;
    }
}
