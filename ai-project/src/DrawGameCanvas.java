import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DrawGameCanvas extends JPanel {
    private final int[][] matrix;
    private final int cellSize = 30;  // Boyutlar her bir hücre için

    public DrawGameCanvas(int[][] matrix) {
        this.matrix = matrix;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                if (matrix[i][j] == 1) {
                    g.setColor(Color.DARK_GRAY);  // Duvarlar
                } else if (matrix[i][j] == 'X') {
                    g.setColor(Color.YELLOW);  // Başlangıç noktası
                } else if (matrix[i][j] == 'Y') {
                    g.setColor(Color.BLACK);  // Bitiş noktası
                } else {
                    g.setColor(Color.LIGHT_GRAY);  // Boş alanlar
                }
                g.fillRect(j * cellSize, i * cellSize, cellSize, cellSize);
                g.setColor(Color.BLACK);
                g.drawRect(j * cellSize, i * cellSize, cellSize, cellSize);
            }
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
                String filePath = "level01.txt";  // Dosya yolunu burada belirleyin
                int[][] matrix = readMatrixFromFile(filePath);
                JFrame frame = new JFrame("Game Canvas");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(500, 500);
                frame.add(new DrawGameCanvas(matrix));
                frame.setVisible(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
