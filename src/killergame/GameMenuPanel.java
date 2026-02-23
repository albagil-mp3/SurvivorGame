package killergame;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Menu panel for Killer Game.
 * Rendered inside the main game window.
 */
public class GameMenuPanel extends JPanel {

    private final Runnable onPlay;

    public GameMenuPanel(Runnable onPlay) {
        this.onPlay = onPlay;
        buildLayout();
    }

    private void buildLayout() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(new Color(0, 0, 0, 80));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(true);
        titlePanel.setBackground(new Color(0, 0, 0, 110));
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("BABILONIAN MAZE", SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(48f));
        titleLabel.setForeground(Color.YELLOW);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Have fun", SwingConstants.CENTER);
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(20f));
        subtitleLabel.setForeground(Color.WHITE);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);

        titlePanel.add(Box.createVerticalGlue());
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(10));
        titlePanel.add(subtitleLabel);
        titlePanel.add(Box.createVerticalGlue());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(true);
        buttonPanel.setBackground(new Color(0, 0, 0, 110));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        JButton playButton = createMenuButton("PLAY", Color.GREEN);
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onPlay != null) {
                    onPlay.run();
                }
            }
        });

        JButton instructionsButton = createMenuButton("INSTRUCTIONS", Color.CYAN);
        instructionsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showInstructions();
            }
        });

        JButton exitButton = createMenuButton("EXIT", Color.RED);
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        buttonPanel.add(Box.createVerticalGlue());
        buttonPanel.add(playButton);
        buttonPanel.add(Box.createVerticalStrut(20));
        buttonPanel.add(instructionsButton);
        buttonPanel.add(Box.createVerticalStrut(20));
        buttonPanel.add(exitButton);
        buttonPanel.add(Box.createVerticalGlue());

        add(titlePanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
    }

    private JButton createMenuButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(button.getFont().deriveFont(18f));
        button.setForeground(color);
        button.setBackground(Color.DARK_GRAY);
        button.setBorderPainted(true);
        button.setAlignmentX(CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(200, 50));
        button.setPreferredSize(new Dimension(200, 50));
        button.setFocusPainted(false);
        return button;
    }

    private void showInstructions() {
        JOptionPane.showMessageDialog(this,
                "BABILONIAN MAZE - Instructions\n\n"
                        + "- Use W, A, S, D keys to move around the maze\n"
                        + "- Move mouse to aim at enemies\n"
                        + "- Use Space to shoot your weapon\n"
                        + "- Enemies will run from you through the maze\n"
                        + "- Kill as many enemies as you can!\n",
                "How to Play",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
