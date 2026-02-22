package engine.view.hud.impl;

import java.awt.Color;

import engine.view.hud.core.DataHUD;

public class PlayerHUD extends DataHUD {
    public PlayerHUD() {
        super(
                new Color(255, 140, 0, 255), // Title color
                Color.GRAY, // Highlight color
                new Color(255, 255, 255, 150), // Label color
                new Color(255, 255, 255, 255), // Data color
                50, 12, 35);

        this.addItems();
    }

    private void addItems() {
        this.addSkipValue();              // entityId
        this.addSkipValue();              // playerName
        this.addSkipValue();              // damage
        this.addSkipValue();              // energy
        this.addSkipValue();              // shield
        this.addSkipValue();              // temperature
        this.addSkipValue();              // activeWeapon
        this.addSkipValue();              // guns
        this.addSkipValue();              // burst
        this.addSkipValue();              // mines
        this.addSkipValue();              // missiles
        this.addTitle("Progress");
        this.addTextItem("Level");
        this.addTextItem("Score");

        this.prepareHud();
    }
}
