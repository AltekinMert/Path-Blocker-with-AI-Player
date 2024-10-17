import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DrawGameCanvas extends JPanel {
    private int[][] matrix;
    private final int cellSize = 30; // Boyutlar her bir hücre için
    private int playerX, playerY;
    private String levelFolder;
    private int currentLevel = 1; // Başlangıç seviyesi
    private String currentLevelFolder = "level01";
    private int moveCount = 0; // Counter for the number of moves
    private List<List<int[]>> moveHistory = new ArrayList<>(); // Hareket geçmişi (geri alma için)

    public DrawGameCanvas(int[][] matrix, String levelFolder) {
        this.matrix = matrix;
        this.levelFolder = levelFolder;
        // Find the starting position 'X'
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                if (matrix[i][j] == 'X') {
                    playerX = j;
                    playerY = i;
                    matrix[i][j] = 0; // Make the starting point walkable
                }
            }
        }
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> movePlayer(0, -1, true); // W key
                    case KeyEvent.VK_A -> movePlayer(-1, 0, true); // A key
                    case KeyEvent.VK_S -> movePlayer(0, 1, true); // S key
                    case KeyEvent.VK_D -> movePlayer(1, 0, true); // D key
                    case KeyEvent.VK_R -> restartLevel(); // R key for restart
                    case KeyEvent.VK_Z -> undoLastMove(); // Z key for undo
                }
            }
        });
    }

    private void movePlayer(int dx, int dy, boolean continuous) {
        boolean moved = false;
        List<int[]> currentMove = new ArrayList<>();
        while (true) {
            int newX = playerX + dx;
            int newY = playerY + dy;

            // Check boundaries and if the new position is walkable or is the goal 'Y'
            if (newX >= 0 && newX < matrix[0].length && newY >= 0 && newY < matrix.length
                    && (matrix[newY][newX] == 0 || matrix[newY][newX] == 'Y')) {
                // Mark the current position as a wall
                matrix[playerY][playerX] = 1;
                // Save the current position to the current move for undo
                currentMove.add(new int[] { playerX, playerY });
                // Update player position
                playerX = newX;
                playerY = newY;
                moved = true;

                // If the player reaches 'Y', replace 'Y' with 'X'
                if (matrix[playerY][playerX] == 'Y') {
                    matrix[playerY][playerX] = 0;
                }
            } else {
                break;
            }

            if (!continuous) {
                break;
            }
        }

        if (moved) {
            // Save the entire movement as one action in move history
            moveHistory.add(currentMove);
            moveCount++; // Increment the move counter when the player moves
            repaint();
            takeScreenshot();
            checkForNextLevel(); // Check if the player has captured all 'Y' on the map
        }
    }

    private void restartLevel() {
        // Clear all screenshots of the current level
        try {
            Path screenshotDir = Paths.get("Screenshots", currentLevelFolder);
            if (Files.exists(screenshotDir)) {
                Files.walk(screenshotDir)
                        .filter(Files::isRegularFile)
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            String filePath = "levels/level" + String.format("%02d", currentLevel) + ".txt";
            int[][] newMatrix = readMatrixFromFile(filePath);
            this.matrix = newMatrix;
            // Reset player position
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    if (matrix[i][j] == 'X') {
                        playerX = j;
                        playerY = i;
                        matrix[i][j] = 0; // Make the starting point walkable
                    }
                }
            }
            moveHistory.clear(); // Clear move history
            moveCount = 0;
            repaint();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void undoLastMove() {
        if (!moveHistory.isEmpty()) {
            // Remove the latest screenshot of the current level
            try {
                Path screenshotDir = Paths.get("Screenshots", currentLevelFolder);
                File latestScreenshot = new File(screenshotDir.toFile(), "screenshot_move_" + moveCount + ".png");
                if (latestScreenshot.exists()) {
                    latestScreenshot.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!moveHistory.isEmpty()) {
            List<int[]> lastMove = moveHistory.remove(moveHistory.size() - 1);
            // Restore all the player's previous positions in the last move
            for (int[] position : lastMove) {
                matrix[position[1]][position[0]] = 0; // Make the position walkable again
            }
            // Set player position back to the last point before the move
            if (!lastMove.isEmpty()) {
                int[] lastPosition = lastMove.get(0);
                playerX = lastPosition[0];
                playerY = lastPosition[1];
            }
            repaint();
        }
    }

    private void nextLevel() {
        currentLevel++;
        currentLevelFolder = "level" + String.format("%02d", currentLevel);
        try {
            String filePath = "levels/level" + String.format("%02d", currentLevel) + ".txt";
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                int[][] newMatrix = readMatrixFromFile(filePath);
                this.matrix = newMatrix;
                // Reset player position
                for (int i = 0; i < matrix.length; i++) {
                    for (int j = 0; j < matrix[i].length; j++) {
                        if (matrix[i][j] == 'X') {
                            playerX = j;
                            playerY = i;
                            matrix[i][j] = 0; // Make the starting point walkable
                        }
                    }
                }
                moveHistory.clear(); // Clear move history
                moveCount = 0;
                repaint();
            } else {
                System.out.println("All levels completed!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkForNextLevel() {
        boolean yExists = false;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                if (matrix[i][j] == 'Y') {
                    yExists = true;
                    break;
                }
            }
            if (yExists) {
                break;
            }
        }
        if (!yExists) {
            nextLevel();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                if (i == playerY && j == playerX) {
                    g.setColor(Color.YELLOW); // Player position
                } else if (matrix[i][j] == 1) {
                    g.setColor(Color.DARK_GRAY); // Duvarlar
                } else if (matrix[i][j] == 'Y') {
                    g.setColor(Color.BLACK); // Bitiş noktasi
                } else {
                    g.setColor(Color.LIGHT_GRAY); // Boş alanlar
                }
                g.fillRect(j * cellSize, i * cellSize, cellSize, cellSize);
                g.setColor(Color.BLACK);
                g.drawRect(j * cellSize, i * cellSize, cellSize, cellSize);
            }
        }
    }

    private void takeScreenshot() {
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        paint(g2d);
        g2d.dispose();
        try {
            // Create the directory for the current level if it doesn't exist
            Path screenshotDir = Paths.get("Screenshots", currentLevelFolder);
            if (!Files.exists(screenshotDir)) {
                Files.createDirectories(screenshotDir);
            }
            // Save the screenshot with the move count
            File outputfile = new File(screenshotDir.toFile(), "screenshot_move_" + moveCount + ".png");
            ImageIO.write(image, "png", outputfile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static int[][] readMatrixFromFile(String filePath) throws IOException {
        List<int[]> matrixList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(" ");
                int[] row = new int[tokens.length];
                for (int i = 0; i < tokens.length; i++) {
                    if (tokens[i].equals("X")) {
                        row[i] = 'X';
                    } else if (tokens[i].equals("Y")) {
                        row[i] = 'Y';
                    } else {
                        row[i] = Integer.parseInt(tokens[i]);
                    }
                }
                matrixList.add(row);
            }
        }
        return matrixList.toArray(new int[0][]);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                String[] options = { "User Player", "AI Player" };
                int choice = JOptionPane.showOptionDialog(null, "Select Player Type:", "Player Selection",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

                if (choice == 0) { // User Player seçildiğinde
                    String filePath = "levels/level01.txt"; // Başlangıç seviyesini belirleyin

                    // filePath'ten levelFolder'ı çıkarma işlemi
                    String levelFolder = "level01";

                    int[][] matrix = readMatrixFromFile(filePath);
                    JFrame frame = new JFrame("Game Canvas");
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setSize(500, 500);
                    DrawGameCanvas canvas = new DrawGameCanvas(matrix, levelFolder);
                    frame.add(canvas);
                    frame.setVisible(true);
                } else if (choice == 1) { // AI Player seçildiğinde
                    JOptionPane.showMessageDialog(null, "AI Player implementation will be added later.", "Info",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
