import java.io.*;
import java.util.*;

public class BFSearch {

    static final int N = 16;
    static char[][] grid = new char[N][N];
    static int startX, startY, endX, endY;

    // Directions: up, down, left, right
    static int[] dx = { -1, 1, 0, 0 };
    static int[] dy = { 0, 0, -1, 1 };
    static String[] dirNames = { "up", "down", "left", "right" };

    public static void main(String[] args) throws IOException {
        // Provide the path to your .txt file here
        String filePath = "C:\\Users\\merta\\OneDrive\\Masaüstü\\ai-project\\Path-Blocker-with-AI-Player\\ai-project-path-blocker\\levels\\level07.txt";
        readGrid(filePath);

        List<String> path = findShortestPath();
        if (path != null) {
            System.out.println("Shortest path:");
            for (String move : path) {
                System.out.print(move + " ");
            }
        } else {
            System.out.println("No path found.");
        }
    }

    static void readGrid(String filePath) throws IOException {
        //
        //
        //
        //
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        for (int i = 0; i < N; i++) {
            String line = br.readLine().replaceAll("\\s+", "");
            for (int j = 0; j < N; j++) {
                grid[i][j] = line.charAt(j);
                if (grid[i][j] == 'X') {
                    startX = i;
                    startY = j;
                    grid[i][j] = '0'; // Treat 'X' as free space for movement
                }
                if (grid[i][j] == 'Y') {
                    endX = i;
                    endY = j;
                    grid[i][j] = '0'; // Treat 'Y' as free space for movement
                }
            }
        }
        br.close();
    }

    static List<String> findShortestPath() {
        // State: current position, grid state, path taken
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
                return Objects.hash(x, y, gridState);
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
        Set<State> visited = new HashSet<>();

        BitSet initialGridState = gridToBitSet(grid);
        List<String> initialPath = new ArrayList<>();

        State initialState = new State(startX, startY, initialGridState, initialPath);
        queue.add(initialState);
        visited.add(initialState);

        while (!queue.isEmpty()) {
            State current = queue.poll();

            if (current.x == endX && current.y == endY) {
                return current.path;
            }

            for (int d = 0; d < 4; d++) {
                int nx = current.x;
                int ny = current.y;
                BitSet newGridState = (BitSet) current.gridState.clone();
                List<String> newPath = new ArrayList<>(current.path);
                newPath.add(dirNames[d]);

                // Move in the direction until hitting a wall (1)
                while (true) {
                    int tx = nx + dx[d];
                    int ty = ny + dy[d];
                    if (tx < 0 || tx >= N || ty < 0 || ty >= N)
                        break;
                    if (getGridValue(newGridState, tx, ty) == '1')
                        break;

                    // Leave a trail by turning '0's to '1's
                    setGridValue(newGridState, nx, ny, '1');

                    nx = tx;
                    ny = ty;
                }

                // Check if movement is possible (did we move?)
                if (nx == current.x && ny == current.y)
                    continue;

                // Create new state
                State newState = new State(nx, ny, newGridState, newPath);

                if (visited.contains(newState))
                    continue;
                visited.add(newState);
                queue.add(newState);

                // Undo move (optional, if you implement undo functionality)
            }
        }

        return null; // No path found
    }

    static BitSet gridToBitSet(char[][] grid) {
        BitSet bitSet = new BitSet(N * N);
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (grid[i][j] == '1') {
                    bitSet.set(i * N + j);
                }
            }
        }
        return bitSet;
    }

    static char getGridValue(BitSet bitSet, int x, int y) {
        return bitSet.get(x * N + y) ? '1' : '0';
    }

    static void setGridValue(BitSet bitSet, int x, int y, char value) {
        if (value == '1') {
            bitSet.set(x * N + y);
        } else {
            bitSet.clear(x * N + y);
        }
    }
}
