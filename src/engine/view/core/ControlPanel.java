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

        this.playAgainButton = new javax.swing.JButton("PLAY AGAIN");
        this.playAgainButton.setFont(this.playAgainButton.getFont().deriveFont(18f));
        this.playAgainButton.setForeground(java.awt.Color.GREEN);
        this.playAgainButton.setBackground(java.awt.Color.DARK_GRAY);
        this.playAgainButton.setBorderPainted(true);
        this.playAgainButton.setMaximumSize(new java.awt.Dimension(200, 50));
        this.playAgainButton.setPreferredSize(new java.awt.Dimension(200, 50));
        this.playAgainButton.setFocusPainted(false);
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
