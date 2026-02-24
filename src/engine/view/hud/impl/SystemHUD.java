package engine.view.hud.impl;

import java.awt.Color;

import engine.view.hud.core.DataHUD;

public class SystemHUD extends DataHUD {
    public SystemHUD() {
        super(
                new Color(255, 140, 0, 255 ), // Title color
                Color.GRAY, // Highlight color
                new Color(255, 255, 255, 150), // Label color
                new Color(255, 255, 255, 255), // Data color
                50, 200, 35);

        this.addItems();
    }

    private void addItems() {
        this.addSkipValue();             // FPS (hidden)
        this.addSkipValue();              // Draw Scene
        this.addSkipValue();             // Draw HUD
        this.addSkipValue();             // Draw Total
        this.addSkipValue();
        this.addSkipValue();      
        this.addSkipValue();
        this.prepareHud();
    }
}