package engine.view.core;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import engine.view.renderables.ports.PlayerRenderDTO;
import killergame.HighscoreStore;

/**
 * Semi-transparent pause overlay with controls and score display.
 */
public class PauseOverlay extends JPanel {

    private final View view;
    private final JPanel centerPanel;
    private final JLabel lblScore;
    private final JLabel lblHighscore;
    private Runnable onReset;
    private Runnable onExit;

    public PauseOverlay(View view) {
        this.view = view;
        this.setLayout(new BorderLayout());
        this.setOpaque(false);

        centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setPreferredSize(new Dimension(360, 220));
        centerPanel.setBackground(new Color(0, 0, 0, 200));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 12, 8, 12);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;

        JLabel title = new JLabel("PAUSE");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        centerPanel.add(title, c);

        c.gridy++;
        lblScore = new JLabel("Score: 0");
        lblScore.setFont(new Font("SansSerif", Font.PLAIN, 18));
        lblScore.setForeground(Color.YELLOW);
        centerPanel.add(lblScore, c);

        c.gridy++;
        lblHighscore = new JLabel("Highscore: 0");
        lblHighscore.setFont(new Font("SansSerif", Font.PLAIN, 18));
        lblHighscore.setForeground(Color.ORANGE);
        centerPanel.add(lblHighscore, c);

        c.gridy++;
        c.gridwidth = 2;
        JButton btnContinue = createMenuButton("Continuar", Color.GREEN);
        btnContinue.addActionListener(e -> {
            // Use View helper so engineResume() is performed there
            if (this.view != null) this.view.hidePauseOverlay();
        });
        centerPanel.add(btnContinue, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        JButton btnExit = createMenuButton("Salir al menú", Color.RED);
        btnExit.addActionListener(e -> {
            if (onExit != null) onExit.run(); else System.exit(0);
        });
        centerPanel.add(btnExit, c);

        this.add(centerPanel, BorderLayout.CENTER);
    }

    private JButton createMenuButton(String text, java.awt.Color color) {
        JButton button = new JButton(text);
        button.setFont(button.getFont().deriveFont(18f));
        button.setForeground(color);
        button.setBackground(java.awt.Color.DARK_GRAY);
        button.setBorderPainted(true);
        button.setAlignmentX(CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(200, 50));
        button.setPreferredSize(new Dimension(200, 50));
        button.setFocusPainted(false);
        return button;
    }

    public void showOverlay() {
        SwingUtilities.invokeLater(() -> {
            PlayerRenderDTO p = this.view.getLocalPlayerRenderData();
            int score = p == null ? 0 : p.score;
            lblScore.setText("Score: " + score);
            int hs = HighscoreStore.getHighscore();
            if (score > hs) {
                HighscoreStore.saveHighscore(score);
                hs = score;
            }
            lblHighscore.setText("Highscore: " + hs);

            this.setVisible(true);
            this.repaint();
        });
    }

    public void hideOverlay() {
        SwingUtilities.invokeLater(() -> {
            this.setVisible(false);
            this.view.requestFocusInWindow();
        });
    }

    public void setOnReset(Runnable r) { this.onReset = r; }
    public void setOnExit(Runnable r) { this.onExit = r; }
}
