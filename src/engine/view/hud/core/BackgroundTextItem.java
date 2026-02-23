package engine.view.hud.core;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

public class BackgroundTextItem extends Item {

    private final Color backgroundColor;
    private final int paddingX;
    private final int paddingY;

    BackgroundTextItem(String label, Color labelColor, Color dataColor, Color backgroundColor, int paddingX, int paddingY) {
        super(label, labelColor, dataColor, true);
        this.backgroundColor = backgroundColor;
        this.paddingX = paddingX;
        this.paddingY = paddingY;
    }

    @Override
    public void draw(Graphics2D g, FontMetrics fm, int posX, int posY, Object value) {
        final String labelText = getPaddedLabel();
        final String valueText = String.valueOf(value);
        final String fullText = labelText + valueText;

        int textWidth = fm.stringWidth(fullText);
        int textTop = posY - fm.getAscent();
        int textHeight = fm.getHeight();

        g.setColor(backgroundColor);
        g.fillRect(
                posX - paddingX,
                textTop - paddingY,
                textWidth + (2 * paddingX),
                textHeight + (2 * paddingY));

        g.setColor(getLabelColor());
        g.drawString(labelText, posX, posY);

        int valueX = posX + fm.stringWidth(labelText);
        g.setColor(getDataColor());
        g.drawString(valueText, valueX, posY);
    }
}
