package main;

import model.Tile;
import model.PlayerRooster;
import model.EnemyRooster;
import model.CornItem;
import model.character.Character;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;

public class GamePanel extends JPanel implements ActionListener {
    private static final int TILE_SIZE = 34; // 34px sprites
    private static final int MAZE_WIDTH = 25; // Increased from 15 to 25
    private static final int MAZE_HEIGHT = 20; // Increased from 15 to 20
    private static final int FPS = 60;
    
    private Tile[][] maze;
    private PlayerRooster player;
    private EnemyRooster enemy;
    private CornItem corn;
    private Timer gameTimer;
    private boolean playerTurn;
    private Random random = new Random();
    private int turnCounter = 0;
    
    // Level system variables
    private int playerLevel = 1;
    private int playerKillCount = 0;
    private int enemyDeathCount = 0;
    private int enemyLevel = 1;
    private static final int KILLS_PER_PLAYER_LEVEL = 5;
    private static final int DEATHS_PER_ENEMY_LEVEL = 5;
    private static final int BASE_ENEMY_HEALTH = 2;
    private int survivalTime = 0; // Track how long player survived
    private boolean gameOver = false;
    
    public GamePanel() {
        // Increase panel size to accommodate larger maze and UI area
        setPreferredSize(new Dimension(MAZE_WIDTH * TILE_SIZE, MAZE_HEIGHT * TILE_SIZE + 150));
        setBackground(Color.BLACK);
        setFocusable(true);
        
        initializeGame();
        
        // Set up game timer for smooth updates
        gameTimer = new Timer(1000 / FPS, this);
        gameTimer.start();
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (playerTurn && !player.isMoving()) {
                    handleKeyPress(e.getKeyCode());
                }
            }
        });
    }
    
    /**
     * Set the assets path for all game resources (tiles and characters)
     * @param path The directory path where assets are stored (should end with /)
     */
    public static void setAssetsPath(String path) {
        Tile.setAssetsPath(path);
        Character.IMAGE_BASE_PATH = path;
    }
    
    private void initializeGame() {
        // Ensure assets path is set relative to working directory
        setAssetsPath("assets/");
        
        // Initialize maze
        maze = new Tile[MAZE_HEIGHT][MAZE_WIDTH];
        createMaze();
        
        // Create player at position (1,1) - use the correct PlayerRooster from model package
        player = new PlayerRooster(1, 1, TILE_SIZE);
        
        // Add just one enemy at the opposite corner
        enemy = new EnemyRooster(MAZE_HEIGHT - 2, MAZE_WIDTH - 2, TILE_SIZE);
        
        // No corn at the beginning
        corn = null;
        
        playerTurn = true;
        turnCounter = 0;
        
        playerLevel = 1;
        playerKillCount = 0;
        enemyDeathCount = 0;
        enemyLevel = 1;
        survivalTime = 0;
        gameOver = false;
        
        // Set initial enemy health based on enemy level
        enemy.setHealth(BASE_ENEMY_HEALTH + (enemyLevel - 1));
    }
    
    private void createMaze() {
        // Create a simple maze with walls at the borders and some random walls inside
        for (int row = 0; row < MAZE_HEIGHT; row++) {
            for (int col = 0; col < MAZE_WIDTH; col++) {
                boolean isWall = (col == 0 || row == 0 || col == MAZE_WIDTH - 1 || row == MAZE_HEIGHT - 1);
                
                // Add some internal walls
                if (!isWall && (col % 3 == 0 && row % 2 == 0)) {
                    isWall = true;
                }
                
                maze[row][col] = new Tile(isWall);
            }
        }
        
        // Ensure starting position and some paths are clear
        maze[1][1].setWall(false);
        maze[1][2].setWall(false);
        maze[2][1].setWall(false);
        
        // Ensure enemy positions are clear
        maze[MAZE_HEIGHT - 2][MAZE_WIDTH - 2].setWall(false);
    }
    
    private void handleKeyPress(int keyCode) {
        // Don't process if player is moving or it's not the player's turn
        if (player.isMoving() || !playerTurn) {
            return;
        }
        
        int dRow = 0, dCol = 0;
        
        switch (keyCode) {
            case KeyEvent.VK_UP:    dRow = -1; break;
            case KeyEvent.VK_DOWN:  dRow = 1;  break;
            case KeyEvent.VK_LEFT:  dCol = -1; break;
            case KeyEvent.VK_RIGHT: dCol = 1;  break;
            default: return;  // Ignore other keys
        }
        
        // Calculate target position
        int targetRow = player.getRow() + dRow;
        int targetCol = player.getCol() + dCol;
        
        // Check if target position has enemy (collision detection)
        if (targetRow == enemy.getRow() && targetCol == enemy.getCol()) {
            // Collision with enemy - attack without moving
            handlePlayerAttackCollision();
            
            // Count this as a move but don't actually move the player
            player.incrementMoveCount();
            
            // Check if player has used both moves after attack
            if (player.getMoveCount() >= 2) {
                playerTurn = false; // Enemy turn next
                
                // Increase turn counter
                turnCounter++;
                
                // 25% chance to spawn corn every 3 turns if no corn exists
                if (corn == null && turnCounter % 3 == 0 && random.nextInt(100) < 25) {
                    corn = CornItem.createAtRandomPosition(maze, player, enemy, TILE_SIZE);
                }
            }
        } else {
            // No collision - try to move player normally
            if (player.move(dRow, dCol, maze)) {
                // Check if player collected corn after successful move
                checkCornCollection();
                
                // If player has used both moves, end their turn
                if (player.getMoveCount() >= 2) {
                    playerTurn = false; // Enemy turn next
                    
                    // Increase turn counter
                    turnCounter++;
                    
                    // 25% chance to spawn corn every 3 turns if no corn exists
                    if (corn == null && turnCounter % 3 == 0 && random.nextInt(100) < 25) {
                        corn = CornItem.createAtRandomPosition(maze, player, enemy, TILE_SIZE);
                    }
                }
            }
        }
    }
    
    private void handlePlayerAttackCollision() {
        // Player attacks enemy without moving - both take damage
        player.setHealth(player.getHealth() - 1);
        enemy.setHealth(enemy.getHealth() - 1);
        System.out.println("Player attacks (collision)! Player stays in place. Player health: " + player.getHealth() + ", Enemy health: " + enemy.getHealth());
        
        // Check if enemy is defeated
        if (enemy.getHealth() <= 0) {
            System.out.println("Enemy defeated!");
            
            // Increase kill and death counters
            playerKillCount++;
            enemyDeathCount++;
            
            // Check for player level up
            if (playerKillCount >= KILLS_PER_PLAYER_LEVEL) {
                levelUpPlayer();
            }
            
            // Check for enemy level up (every 5 deaths)
            if (enemyDeathCount % DEATHS_PER_ENEMY_LEVEL == 0) {
                levelUpEnemy();
            }
            
            // Respawn enemy with current enemy level health
            respawnEnemy();
        }
        
        // Check if player is defeated - GAME OVER (no respawn)
        if (player.getHealth() <= 0) {
            triggerGameOver();
        }
    }
    
    private void respawnEnemy() {
        // Calculate enemy health based on how many times it has died
        int enemyHealthBonus = (enemyDeathCount / DEATHS_PER_ENEMY_LEVEL);
        int newEnemyHealth = BASE_ENEMY_HEALTH + enemyHealthBonus;
        
        int spawnRow = MAZE_HEIGHT - 2;
        int spawnCol = MAZE_WIDTH - 2;
        
        // Avoid spawning on top of the player
        if (player.getRow() == spawnRow && player.getCol() == spawnCol) {
            spawnRow = 1;
            spawnCol = 1;
            if (player.getRow() == spawnRow && player.getCol() == spawnCol) {
                do {
                    spawnRow = random.nextInt(MAZE_HEIGHT - 2) + 1;
                    spawnCol = random.nextInt(MAZE_WIDTH - 2) + 1;
                } while (maze[spawnRow][spawnCol].isWall() ||
                         (spawnRow == player.getRow() && spawnCol == player.getCol()));
            }
        }
        
        enemy = new EnemyRooster(spawnRow, spawnCol, TILE_SIZE);
        enemy.setHealth(newEnemyHealth);
        
        System.out.println("New enemy spawned with " + newEnemyHealth + " health");
        System.out.println("Enemy respawn count: " + enemyDeathCount + " (Level bonus: +" + enemyHealthBonus + ")");
    }
    
    private void processEnemyTurn() {
        if (!enemy.isMoving()) {
            // Debug output
            System.out.println("Processing enemy turn...");
            
            // Calculate where enemy wants to move
            int playerRow = player.getRow();
            int playerCol = player.getCol();
            int enemyRow = enemy.getRow();
            int enemyCol = enemy.getCol();
            
            // Calculate movement direction - only one direction at a time (no diagonal)
            int dRow = 0, dCol = 0;
            
            // Prioritize the direction with greater distance
            int rowDiff = Math.abs(playerRow - enemyRow);
            int colDiff = Math.abs(playerCol - enemyCol);
            
            if (rowDiff > colDiff) {
                // Move vertically first
                if (playerRow < enemyRow) dRow = -1;
                else if (playerRow > enemyRow) dRow = 1;
            } else if (colDiff > rowDiff) {
                // Move horizontally first
                if (playerCol < enemyCol) dCol = -1;
                else if (playerCol > enemyCol) dCol = 1;
            } else if (rowDiff > 0 || colDiff > 0) {
                // Equal distance, choose randomly between horizontal and vertical
                if (random.nextBoolean()) {
                    // Move vertically
                    if (playerRow < enemyRow) dRow = -1;
                    else if (playerRow > enemyRow) dRow = 1;
                } else {
                    // Move horizontally
                    if (playerCol < enemyCol) dCol = -1;
                    else if (playerCol > enemyCol) dCol = 1;
                }
            }
            
            // Check if enemy would move into player's position (collision)
            int targetRow = enemyRow + dRow;
            int targetCol = enemyCol + dCol;
            
            // Only allow attack if enemy is adjacent (not diagonal) to player
            boolean isAdjacentHorizontally = (enemyRow == playerRow) && (Math.abs(enemyCol - playerCol) == 1);
            boolean isAdjacentVertically = (enemyCol == playerCol) && (Math.abs(enemyRow - playerRow) == 1);
            
            if ((targetRow == playerRow && targetCol == playerCol) && 
                (isAdjacentHorizontally || isAdjacentVertically)) {
                // Enemy would collide with player and is adjacent (not diagonal) - attack without moving
                handleEnemyAttackCollision();
                System.out.println("Enemy attacks without moving! Enemy stays at: " + enemyRow + "," + enemyCol);
            } else {
                // No collision or not adjacent - enemy tries to move towards player
                enemy.moveTowardsPlayer(player, maze);
            }
            
            // Return to player's turn and count 1 completed turn
            survivalTime++;
            player.resetMoveCount();
            playerTurn = true;
        }
    }
    
    private void moveEnemyTowardsPlayer() {
        // This method is no longer needed since we're using enemy.moveTowardsPlayer()
        // But keeping it for backward compatibility if needed
        enemy.moveTowardsPlayer(player, maze);
    }
    
    private void handleEnemyAttackCollision() {
        // Enemy attacks player without moving - only player takes damage
        player.setHealth(player.getHealth() - 1);
        System.out.println("Enemy attacks (collision)! Enemy stays in place. Player health: " + player.getHealth());
        
        // Check if player is defeated - GAME OVER (no respawn)
        if (player.getHealth() <= 0) {
            triggerGameOver();
        }
    }

    private void triggerGameOver() {
        System.out.println("Game Over! Player defeated.");
        gameOver = true;
        if (gameTimer != null) {
            gameTimer.stop();
        }
        repaint();
        AyamJagoLabirinGame.showGameOver(playerLevel, playerKillCount, survivalTime);
    }
    
    private void levelUpPlayer() {
        playerLevel++;
        playerKillCount = 0; // Reset kill count
        
        // Give player health bonus
        int healthBonus = 3;
        player.setHealth(player.getHealth() + healthBonus);
        
        System.out.println("PLAYER LEVEL UP! Level: " + playerLevel);
        System.out.println("Health bonus: +" + healthBonus + ". Current health: " + player.getHealth());
    }
    
    private void levelUpEnemy() {
        enemyLevel++;
        
        System.out.println("ENEMY LEVEL UP! Enemy Level: " + enemyLevel);
        System.out.println("Next enemies will be stronger!");
    }
    
    private void checkCornCollection() {
        if (corn != null && corn.isActive() && 
            player.getRow() == corn.getRow() && player.getCol() == corn.getCol()) {
            
            // Player collects corn - heal 3 health
            player.setHealth(player.getHealth() + 3);
            System.out.println("Player collected corn! Health +3. Current health: " + player.getHealth());
            
            // Deactivate the corn
            corn.setActive(false);
            corn = null;
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            updateGame();
            repaint();
        }
    }
    
    private void updateGame() {
        if (gameOver) return;
        
        player.update();
        enemy.update();
        
        // Update corn animation if it exists
        if (corn != null && corn.isActive()) {
            corn.update();
        }
        
        // If no animations are in progress and it's enemy's turn, process enemy turn
        if (!player.isMoving() && !enemy.isMoving() && !playerTurn) {
            processEnemyTurn();
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (gameOver) {
            // Draw game over screen
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            g.drawString("GAME OVER", getWidth()/2 - 150, getHeight()/2);
            return;
        }
        
        // Draw maze
        for (int row = 0; row < MAZE_HEIGHT; row++) {
            for (int col = 0; col < MAZE_WIDTH; col++) {
                // Gambar tile menggunakan method draw milik Tile
                maze[row][col].draw(g, col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE);
            }
        }
        
        // Draw corn if it exists and is active
        if (corn != null && corn.isActive()) {
            corn.draw(g);
        }
        
        // Draw enemy
        enemy.draw(g);
        
        // Draw player
        player.draw(g);
        
        // Move UI to bottom of the game area to not obstruct gameplay
        int uiStartY = MAZE_HEIGHT * TILE_SIZE + 10; // Start UI below the maze
        
        // Draw turn indicator
        g.setColor(Color.BLACK);
        g.fillRect(5, uiStartY, 200, 25);
        g.setColor(playerTurn ? Color.GREEN : Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        if (playerTurn) {
            g.drawString("Player Turn: " + (2 - player.getMoveCount()) + " moves left", 10, uiStartY + 15);
        } else {
            g.drawString("Enemy Turn", 10, uiStartY + 15);
        }
        
        // Draw health and level information below turn indicator
        g.setColor(Color.BLACK);
        g.fillRect(5, uiStartY + 30, 500, 100);
        g.setColor(Color.WHITE);
        g.drawString("Player Health: " + player.getHealth(), 10, uiStartY + 45);
        g.drawString("Enemy Health: " + enemy.getHealth(), 200, uiStartY + 45);
        
        // Draw level information in organized columns
        g.setColor(Color.YELLOW);
        g.drawString("Player Level: " + playerLevel, 10, uiStartY + 65);
        g.drawString("Kills: " + playerKillCount + "/" + KILLS_PER_PLAYER_LEVEL, 10, uiStartY + 85);
        
        g.setColor(Color.RED);
        g.drawString("Enemy Deaths: " + enemyDeathCount, 200, uiStartY + 65);
        g.drawString("Enemy Health Bonus: +" + (enemyDeathCount / DEATHS_PER_ENEMY_LEVEL), 200, uiStartY + 85);
        
        g.setColor(Color.CYAN);
        g.drawString("Survival Time: " + survivalTime + " turns", 10, uiStartY + 105);
    }
}
