/*
1) Why you prefer the search algorithm you choose?
   - I preferred to use the Breadth-First Search (BFS) algorithm because it is simple to implement and guarantees finding the shortest path in an unweighted grid environment like ours. BFS explores all nodes at the present depth before moving on to nodes at the next depth level, making it ideal for finding the minimum number of moves needed to reach the goal ('Y') from the start ('X').

2) Can you achieve the optimal result? Why? Why not?
   - Yes, we can achieve the optimal result with BFS in this problem because it is designed to find the shortest path in an unweighted environment. Since each move (up, down, left, right) has the same cost (essentially 1), BFS will find the shortest path to the goal without missing any shorter paths.

3) How you achieved efficiency for keeping the states?
   - Efficiency in keeping states was achieved by using a BitSet to represent the grid state for visited nodes and by using a queue (LinkedList) to store and process states level by level. This reduces memory overhead compared to storing the entire matrix repeatedly and keeps the BFS implementation fast and memory efficient.

4) If you prefer to use DFS (tree version) then do you need to avoid cycles?
   - Yes, if using Depth-First Search (DFS), it is crucial to avoid cycles. In a grid environment, without cycle detection, DFS could enter infinite loops or revisit the same cells multiple times, resulting in inefficient pathfinding or failure to find the goal.

5) What will be the path-cost for this problem?
   - The path-cost for this problem is simply the number of moves taken to reach the goal ('Y') from the starting position ('X'). Each move has an equal cost of 1, and BFS ensures that the minimal path-cost (i.e., the shortest path in terms of moves) is found.
*/

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main extends JPanel {
    private Timer aiTimer;
    // import javax.swing.Timer;
    // import java.util.List;
    private char[][] matrix;
    private final int cellSize = 30; // Size for each cell
    private int playerX, playerY;
    private String levelFolder;
    private int currentLevel = 1; // Starting level
    private String currentLevelFolder = "level01";
    private int moveCount = 0; // Counter for the number of moves
    private List<List<int[]>> moveHistory = new ArrayList<>(); // Move history for undo
    private boolean isAIPlayer = false; // Flag to check if AI is playing

    public Main(char[][] matrix, String levelFolder) {
        this.matrix = matrix;
        this.levelFolder = levelFolder;
        initializePlayerPosition();
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isAIPlayer)
                    return; // Ignore key presses if AI is playing
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

    private void initializePlayerPosition() {
        int[] startPos = findStartingPosition(matrix);
        if (startPos != null) {
            playerX = startPos[0];
            playerY = startPos[1];
        } else {
            // Handle error: No starting position found
            JOptionPane.showMessageDialog(this, "No starting position 'X' found.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int[] findStartingPosition(char[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                if (matrix[i][j] == 'X') {
                    matrix[i][j] = '0'; // Make the starting point walkable
                    return new int[] { j, i };
                }
            }
        }
        return null;
    }

    private void movePlayer(int dx, int dy, boolean continuous) {
        boolean moved = false;
        List<int[]> currentMove = new ArrayList<>();
        while (true) {
            int newX = playerX + dx;
            int newY = playerY + dy;

            // Check boundaries and if the new position is walkable or is the goal 'Y'
            if (newX >= 0 && newX < matrix[0].length && newY >= 0 && newY < matrix.length
                    && (matrix[newY][newX] == '0' || matrix[newY][newX] == 'Y')) {
                // Mark the current position as a wall
                matrix[playerY][playerX] = '1';
                // Save the current position to the current move for undo
                currentMove.add(new int[] { playerX, playerY });
                // Update player position
                playerX = newX;
                playerY = newY;
                moved = true;

                // If the player reaches 'Y', set a flag or handle level completion
                if (matrix[playerY][playerX] == 'Y') {
                    // Level completed
                    matrix[playerY][playerX] = '0'; // Optional: Make the goal walkable
                    break; // Stop moving after reaching the goal
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
            char[][] newMatrix = readMatrixFromFile(filePath);
            this.matrix = newMatrix;
            initializePlayerPosition();
            moveHistory.clear(); // Clear move history
            moveCount = 0;
            repaint();

            // Stop and reset the AI timer before starting a new one
            if (aiTimer != null) {
                aiTimer.stop();
            }

            // Reset AI variables and restart AI movement if necessary
            if (isAIPlayer) {
                aiMoveIndex = 0;
                aiPath = null;
                startAIMovement();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void undoLastMove() {
        if (isAIPlayer)
            return; // Do not allow undo if AI is playing
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
                matrix[position[1]][position[0]] = '0'; // Make the position walkable again
            }
            // Set player position back to the last point before the move
            if (!lastMove.isEmpty()) {
                int[] lastPosition = lastMove.get(0);
                playerX = lastPosition[0];
                playerY = lastPosition[1];
            }
            moveCount--; // Decrement move count
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
                char[][] newMatrix = readMatrixFromFile(filePath);
                this.matrix = newMatrix;
                initializePlayerPosition();
                moveHistory.clear(); // Clear move history
                moveCount = 0;
                repaint();

                // Stop and reset the AI timer before starting a new one
                if (aiTimer != null) {
                    aiTimer.stop();
                }

                // Reset AI variables and restart AI movement if necessary
                if (isAIPlayer) {
                    aiMoveIndex = 0;
                    aiPath = null;
                    startAIMovement();
                }
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
                } else if (matrix[i][j] == '1') {
                    g.setColor(Color.DARK_GRAY); // Walls
                } else if (matrix[i][j] == 'Y') {
                    g.setColor(Color.RED); // Goal point
                } else {
                    g.setColor(Color.LIGHT_GRAY); // Empty spaces
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

    public static char[][] readMatrixFromFile(String filePath) throws IOException {
        List<char[]> matrixList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                String[] tokens = line.split("\\s+");
                char[] row = new char[tokens.length];
                for (int i = 0; i < tokens.length; i++) {
                    row[i] = tokens[i].charAt(0);
                }
                matrixList.add(row);
            }
        }
        return matrixList.toArray(new char[0][]);
    }

    // AI Implementation
    private Queue<String> aiPath; // The path that the AI will follow as a queue
    private int aiMoveIndex = 0; // The current move index in the aiPath

    public void startAIMovement() {
        isAIPlayer = true;

        try {
            String filePath = "levels/level" + String.format("%02d", currentLevel) + ".txt";
            char[][] initialMatrix = readMatrixFromFile(filePath);

            // Deep copy to avoid modifying the original matrix
            char[][] matrixCopy = deepCopyMatrix(initialMatrix);

            // Find the starting position in initialMatrix
            int[] startPos = findStartingPosition(matrixCopy);
            if (startPos == null) {
                JOptionPane.showMessageDialog(this, "No starting position 'X' found.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            int startX = startPos[0];
            int startY = startPos[1];

            // Update playerX and playerY to match the starting position
            playerX = startX;
            playerY = startY;

            // Find the shortest path
            List<String> pathList = findShortestPath(matrixCopy, startX, startY);
            if (pathList == null) {
                JOptionPane.showMessageDialog(this, "No path found by AI.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Print the path to the console for debugging
            System.out.println("AI Path for Level " + currentLevel + ": " + String.join(" -> ", pathList));

            // Initialize the queue for AI moves
            aiPath = new LinkedList<>(pathList);

            // Take the first move immediately before starting the timer
            if (!aiPath.isEmpty()) {
                String firstMove = aiPath.poll();
                performAIMove(firstMove);
            }

            // Stop any existing timer before starting a new one
            if (aiTimer != null) {
                aiTimer.stop();
            }

            // Create and start a new timer for AI movement
            aiTimer = new Timer(0, e -> {
                if (!aiPath.isEmpty()) {
                    String move = aiPath.poll();
                    performAIMove(move);
                } else {
                    ((Timer) e.getSource()).stop();
                }
            });
            aiTimer.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // A helper method to move the AI based on the move direction
    private void performAIMove(String move) {
        switch (move) {
            case "up" -> movePlayer(0, -1, true);
            case "down" -> movePlayer(0, 1, true);
            case "left" -> movePlayer(-1, 0, true);
            case "right" -> movePlayer(1, 0, true);
        }
    }

    private List<String> findShortestPath(char[][] initialMatrix, int startX, int startY) {
        // Start time recording before the BFS starts
        long startTime = System.nanoTime();

        // Directions: left, right, up, down
        int[] dx = { -1, 1, 0, 0 };
        int[] dy = { 0, 0, -1, 1 };
        String[] dirNames = { "left", "right", "up", "down" };

        class State {
            int x, y;
            BitSet gridState;
            List<String> path;

            State(int x, int y, BitSet gridState, List<String> path) {
                this.x = x;
                this.y = y;
                this.gridState = gridState;
                this.path = path;
            }

            @Override
            public int hashCode() {
                int result = Objects.hash(x, y);
                result = 31 * result + gridState.hashCode();
                return result;
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof State))
                    return false;
                State other = (State) obj;
                return this.x == other.x && this.y == other.y && this.gridState.equals(other.gridState);
            }
        }

        Queue<State> queue = new LinkedList<>();
        Map<State, Integer> visited = new HashMap<>();

        // Ensure we're using the initial matrix for pathfinding
        BitSet initialGridState = gridToBitSet(initialMatrix);
        List<String> initialPath = new ArrayList<>();

        State initialState = new State(startX, startY, initialGridState, initialPath);
        queue.add(initialState);
        visited.put(initialState, 0);

        int cols = initialMatrix[0].length; // Number of columns

        while (!queue.isEmpty()) {
            State current = queue.poll();

            if (isGoal(current.x, current.y, initialMatrix)) {
                // End time recording after the BFS completes
                long endTime = System.nanoTime();

                // Calculate duration in milliseconds
                long duration = (endTime - startTime) / 1_000_000;

                // Print the time taken for the BFS search
                System.out.println("Time taken to find path: " + duration + " ms");

                return current.path;
            }

            for (int d = 0; d < 4; d++) {
                int nx = current.x;
                int ny = current.y;
                BitSet newGridState = (BitSet) current.gridState.clone();
                List<String> newPath = new ArrayList<>(current.path);
                newPath.add(dirNames[d]);

                // Move in the direction until hitting a wall ('1')
                boolean moved = false;
                while (true) {
                    int tx = nx + dx[d];
                    int ty = ny + dy[d];
                    if (tx < 0 || tx >= cols || ty < 0 || ty >= initialMatrix.length)
                        break;
                    char cell = getGridValue(newGridState, tx, ty, cols);
                    if (cell == '1')
                        break;

                    // Leave a trail by turning '0's to '1's
                    setGridValue(newGridState, nx, ny, '1', cols);

                    nx = tx;
                    ny = ty;
                    moved = true;

                    // If we reach the goal 'Y', we can stop moving further in this direction
                    if (initialMatrix[ny][nx] == 'Y') {
                        break;
                    }
                }

                // Check if movement is possible (did we move?)
                if (!moved)
                    continue;

                // Create new state
                State newState = new State(nx, ny, newGridState, newPath);

                if (visited.containsKey(newState))
                    continue;
                visited.put(newState, newPath.size());
                queue.add(newState);
            }
        }

        // End time recording if BFS search fails
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000;
        System.out.println("BFS search failed to find a path. Time taken: " + duration + " ms");

        return null; // No path found
    }

    private boolean isGoal(int x, int y, char[][] initialMatrix) {
        return initialMatrix[y][x] == 'Y';
    }

    private BitSet gridToBitSet(char[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;
        BitSet bitSet = new BitSet(rows * cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] == '1') {
                    bitSet.set(i * cols + j);
                }
            }
        }
        return bitSet;
    }

    private char getGridValue(BitSet bitSet, int x, int y, int cols) {
        int index = y * cols + x;
        return bitSet.get(index) ? '1' : '0';
    }

    private void setGridValue(BitSet bitSet, int x, int y, char value, int cols) {
        int index = y * cols + x;
        if (value == '1') {
            bitSet.set(index);
        } else {
            bitSet.clear(index);
        }
    }

    private char[][] deepCopyMatrix(char[][] original) {
        char[][] copy = new char[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = Arrays.copyOf(original[i], original[i].length);
        }
        return copy;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                String[] options = { "User Player", "AI Player" };
                int choice = JOptionPane.showOptionDialog(null, "Select Player Type:", "Player Selection",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

                String filePath = "levels/level01.txt"; // Starting level file
                String levelFolder = "level01"; // Extract level folder from the file path
                char[][] matrix = readMatrixFromFile(filePath);

                JFrame frame = new JFrame("Game Canvas");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(500, 500);
                Main canvas = new Main(matrix, levelFolder);

                frame.add(canvas);
                frame.setVisible(true);

                if (choice == 1) { // AI Player selected
                    // Start the AI movement
                    canvas.startAIMovement();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
