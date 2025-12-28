import javax.swing.*;
import javax.swing.Timer;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.util.ArrayList;
import java.util.Random;

public class PinballEvolved extends JPanel implements ActionListener, KeyListener {

    // Window
    private static final int WIDTH = 440;
    private static final int HEIGHT = 540;

    // Paddle
    private int paddleWidth = 160;
    private static final int PADDLE_HEIGHT = 14;
    private int paddleX = 140;
    private final int paddleY = 470;

    // Ball
    private static final int BALL_SIZE = 16;
    private ArrayList<Point.Double> balls = new ArrayList<>();
    private ArrayList<Point.Double> speeds = new ArrayList<>();

    // Game state
    private int score = 0;
    private int level = 1;
    private int lives = 3;
    private int highScore = 0;
    private boolean paused = false;
    private boolean gameOver = false;

    private Timer timer;
    private Random rand = new Random();
    private File scoreFile = new File("highscore.dat");

    public PinballEvolved() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);

        loadHighScore();

        balls.add(new Point.Double(200, 120));
        speeds.add(new Point.Double(3.0, 4.0));

        timer = new Timer(16, this);
        timer.start();
    }

    // 🌈 Gradient background
    private void drawBackground(Graphics2D g) {
        GradientPaint gp = new GradientPaint(
                0, 0, new Color(10, 10, 30),
                0, HEIGHT, new Color(50, 0, 80)
        );
        g.setPaint(gp);
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g2);

        if (gameOver) {
            g2.setColor(Color.PINK);
            g2.setFont(new Font("Consolas", Font.BOLD, 32));
            g2.drawString("GAME OVER", 100, 240);

            g2.setFont(new Font("Consolas", Font.PLAIN, 18));
            g2.drawString("Score: " + score, 165, 280);
            g2.drawString("High Score: " + highScore, 135, 310);
            g2.drawString("Press R to Restart", 120, 350);
            return;
        }

        // Balls
        g2.setColor(new Color(255, 90, 90));
        for (Point.Double b : balls)
            g2.fillOval((int) b.x, (int) b.y, BALL_SIZE, BALL_SIZE);

        // Paddle
        g2.setColor(new Color(0, 220, 255));
        g2.fillRoundRect(paddleX, paddleY, paddleWidth, PADDLE_HEIGHT, 12, 12);

        // HUD
        g2.setFont(new Font("Consolas", Font.BOLD, 14));
        g2.setColor(Color.WHITE);
        g2.drawString("Score: " + score, 12, 22);
        g2.drawString("Level: " + level, 150, 22);
        g2.drawString("Lives: " + lives, 260, 22);
        g2.drawString("High: " + highScore, 340, 22);
        if (paused)
            g2.drawString("PAUSED", 185, 50);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (paused || gameOver) return;

        for (int i = 0; i < balls.size(); i++) {
            Point.Double b = balls.get(i);
            Point.Double s = speeds.get(i);

            b.x += s.x;
            b.y += s.y;

            // Wall collision
            if (b.x <= 0 || b.x >= WIDTH - BALL_SIZE) {
                s.x *= -1;
                Toolkit.getDefaultToolkit().beep();
            }
            if (b.y <= 0) {
                s.y *= -1;
                Toolkit.getDefaultToolkit().beep();
            }

            // Paddle collision
            if (b.y + BALL_SIZE >= paddleY &&
                b.x + BALL_SIZE > paddleX &&
                b.x < paddleX + paddleWidth) {

                s.y *= -1;
                score++;
                Toolkit.getDefaultToolkit().beep();

                if (score % 5 == 0) {
                    level++;
                    s.y += 0.3;
                    s.x += (s.x > 0 ? 0.3 : -0.3);
                    if (paddleWidth > 50)
                        paddleWidth -= 6;

                    // Multi-ball after level 3
                    if (level == 3) {
                        balls.add(new Point.Double(200, 120));
                        speeds.add(new Point.Double(-3.0, 4.0));
                    }
                }
            }

            // Missed ball
            if (b.y > HEIGHT) {
                balls.remove(i);
                speeds.remove(i);
                lives--;
                i--;
            }
        }

        if (balls.isEmpty()) {
            if (lives > 0) {
                balls.add(new Point.Double(200, 120));
                speeds.add(new Point.Double(3.0, 4.0));
            } else {
                gameOver = true;
                saveHighScore();
                timer.stop();
            }
        }

        repaint();
    }

    private void restartGame() {
        score = 0;
        level = 1;
        lives = 3;
        paddleWidth = 160;
        paddleX = 140;
        balls.clear();
        speeds.clear();
        balls.add(new Point.Double(200, 120));
        speeds.add(new Point.Double(3.0, 4.0));
        paused = false;
        gameOver = false;
        timer.start();
    }

    private void saveHighScore() {
        if (score > highScore) {
            highScore = score;
            try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(scoreFile))) {
                o.writeInt(highScore);
            } catch (Exception ignored) {}
        }
    }

    private void loadHighScore() {
        if (!scoreFile.exists()) return;
        try (ObjectInputStream i = new ObjectInputStream(new FileInputStream(scoreFile))) {
            highScore = i.readInt();
        } catch (Exception ignored) {}
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT && paddleX > 0)
            paddleX -= 24;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT && paddleX < WIDTH - paddleWidth)
            paddleX += 24;
        if (e.getKeyCode() == KeyEvent.VK_P)
            paused = !paused;
        if (e.getKeyCode() == KeyEvent.VK_R && gameOver)
            restartGame();
    }

    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Pinball Evolved ✨");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new PinballEvolved());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
