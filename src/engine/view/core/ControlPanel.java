package engine.view.core;

import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;


/**
 * Simple control panel used for auxiliary UI (buttons, sliders).
 */
public class ControlPanel extends JPanel {
    View view;
    private final JButton playAgainButton;

    public ControlPanel(View view) {
        this.view = view;
        this.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        this.playAgainButton = new javax.swing.JButton("Play again");
        this.playAgainButton.setVisible(false);
    }

    public void setPlayAgainAction(ActionListener action) {
        // Remove existing listeners
        for (ActionListener a : this.playAgainButton.getActionListeners()) {
            this.playAgainButton.removeActionListener(a);
        }
        if (action != null) this.playAgainButton.addActionListener(action);
    }

    public void showPlayAgain(boolean visible) {
        this.playAgainButton.setVisible(visible);
        this.revalidate();
        this.repaint();
    }

    public JButton getPlayAgainButton() {
        return this.playAgainButton;
    }
}
