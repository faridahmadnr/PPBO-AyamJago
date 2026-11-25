package main;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import model.Tile;
import model.character.Character;
import java.io.File;

public class AyamJagoLabirinGame {
    private static JFrame frame;
    
    public static void main(String[] args) {
        // Set up the assets path before creating any game components
        setupAssetsPaths();
        
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Ayam Jago Labirin - Survival Mode");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            // Updated size for larger maze (25x20) plus UI space
            frame.setSize(25 * 34 + 16, 20 * 34 + 200); // Width: 866px, Height: 880px
            frame.setLocationRelativeTo(null);
            
            GamePanel gamePanel = new GamePanel();
            frame.add(gamePanel);
            
            frame.setVisible(true);
            
            // Ensure the frame is resized to fit the game panel exactly
            frame.pack();
        });
    }
    
    public static void showGameOver(int playerLevel, int enemiesKilled, int survivalTime) {
        String message = "GAME OVER!\n\n" +
                        "Final Stats:\n" +
                        "Player Level: " + playerLevel + "\n" +
                        "Enemies Killed: " + enemiesKilled + "\n" +
                        "Survival Time: " + survivalTime + " turns\n\n" +
                        "Play again?";
        
        int choice = JOptionPane.showConfirmDialog(frame, message, "Game Over", 
                                                  JOptionPane.YES_NO_OPTION);
        
        if (choice == JOptionPane.YES_OPTION) {
            // Restart the game
            frame.getContentPane().removeAll();
            GamePanel newGamePanel = new GamePanel();
            frame.add(newGamePanel);
            frame.revalidate();
            frame.repaint();
            frame.pack();
            newGamePanel.requestFocusInWindow();
        } else {
            // Exit the game
            System.exit(0);
        }
    }
    
    private static void setupAssetsPaths() {
        // Get the current working directory
        String currentDir = System.getProperty("user.dir");
        System.out.println("Current working directory: " + currentDir);
        
        // Use relative assets path from the project working directory
        File assetsDir = new File("assets");
        if (!assetsDir.exists()) {
            assetsDir.mkdirs();
        }
        
        String javaCompatiblePath = "assets/";
        System.out.println("Setting assets path to: " + assetsDir.getAbsolutePath());
        
        try {
            // Ensure the static fields exist in these classes
            Tile.setAssetsPath(javaCompatiblePath);
            Character.IMAGE_BASE_PATH = javaCompatiblePath;
        } catch (Exception e) {
            System.out.println("Error setting asset paths: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Verify that the assets directory contains the required files
        String[] requiredFiles = {
            "wall.png", "floor.jpeg",
            "player-idle1.png", "player-idle2.png", "player-move1.png", "player-move2.png",
            "enemy-idle1.png", "enemy-idle2.png", "enemy-move1.png", "enemy-move2.png",
            "corn.png", "corn2.png"
        };
        System.out.println("Checking for asset files:");
        for (String file : requiredFiles) {
            File assetFile = new File(assetsDir, file);
            System.out.println(file + ": " + (assetFile.exists() ? "Found" : "Missing"));
        }
    }
}
