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
    private final int cellSize = 30;  // Boyutlar her bir hücre için
    private int playerX, playerY;
    private String levelFolder;
    private int moveCount = 0;  // Counter for the number of moves

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
                    case KeyEvent.VK_S -> movePlayer(0, 1, true);  // S key
                    case KeyEvent.VK_D -> movePlayer(1, 0, true);  // D key
                }
            }
        });
    }

    private void movePlayer(int dx, int dy, boolean continuous) {
        boolean moved = false;
        while (true) {
            int newX = playerX + dx;
            int newY = playerY + dy;

            // Check boundaries and if the new position is walkable
            if (newX >= 0 && newX < matrix[0].length && newY >= 0 && newY < matrix.length && matrix[newY][newX] == 0) {
                // Mark the current position as a wall
                matrix[playerY][playerX] = 1;
                // Update player position
                playerX = newX;
                playerY = newY;
                moved = true;
            } else {
                break;
            }

            if (!continuous) {
                break;
            }
        }

        if (moved) {
            moveCount++; // Increment the move counter when the player moves
            repaint();
            takeScreenshot();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                if (i == playerY && j == playerX) {
                    g.setColor(Color.YELLOW);  // Player position
                } else if (matrix[i][j] == 1) {
                    g.setColor(Color.DARK_GRAY);  // Duvarlar
                } else if (matrix[i][j] == 'Y') {
                    g.setColor(Color.BLACK);  // Bitiş noktasi
                } else {
                    g.setColor(Color.LIGHT_GRAY);  // Boş alanlar
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
            // Create the directory if it doesn't exist
            Path screenshotDir = Paths.get("Screenshots", levelFolder);
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
                String filePath = "ai-project-path-blocker/levels/level06.txt";  // Dosya yolunu burada belirleyin
                
                // filePath'ten levelFolder'ı çıkarma işlemi
                String levelFolder = filePath.substring(filePath.lastIndexOf("level"), filePath.lastIndexOf(".txt"));

                int[][] matrix = readMatrixFromFile(filePath);
                JFrame frame = new JFrame("Game Canvas");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(500, 500);
                DrawGameCanvas canvas = new DrawGameCanvas(matrix, levelFolder);
                frame.add(canvas);
                frame.setVisible(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
