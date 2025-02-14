import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Iterator;

public class EchoJumpGame extends JPanel implements KeyListener {
    private int playerX = 100, playerY = 100;
    private int velocityY = 0, velocityX = 0;
    private boolean aPressed = false, dPressed = false;
    private boolean jumping = false, onGround = true;

    private final int PLAYER_WIDTH = 50, PLAYER_HEIGHT = 50;
    private final int ECHO_WIDTH = 50, ECHO_HEIGHT = 50;
    private final int COLLISION_STEPS = 4; // Number of intermediate steps to check for collision

    private ArrayList<Echo> echoes = new ArrayList<>();
    private ArrayList<Platform> platforms = new ArrayList<>();
    private Echo lastPlacedEcho = null;

    public EchoJumpGame() {
        JFrame frame = new JFrame("Echo Jump");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.addKeyListener(this);
        frame.setVisible(true);

        // Create new level design
        platforms.add(new Platform(50, 150, 200, 20));
        platforms.add(new Platform(100, 400, 600, 20));

        playerX = 100;
        playerY = 100;

        Timer timer = new Timer(16, e -> gameLoop());
        timer.start();
    }

    private void gameLoop() {
        if (!onGround) {
            velocityY += 1;
        }

        velocityX = 0;
        if (aPressed) velocityX = -10;
        if (dPressed) velocityX = 10;

        // Store initial position
        int initialX = playerX;
        int initialY = playerY;

        // Calculate final position
        int targetX = initialX + velocityX;
        int targetY = initialY + velocityY;

        // Perform continuous collision detection
        boolean landed = false;

        // Break movement into steps
        for (int step = 1; step <= COLLISION_STEPS; step++) {
            float progress = (float) step / COLLISION_STEPS;
            int currentX = initialX + (int)(velocityX * progress);
            int currentY = initialY + (int)(velocityY * progress);

            // Check platform collisions
            for (Platform platform : platforms) {
                // Check top collision
                if (isCollidingWithPlatformTop(currentX, currentY, PLAYER_WIDTH, PLAYER_HEIGHT, platform)) {
                    currentY = platform.y - PLAYER_HEIGHT;
                    velocityY = 0;
                    onGround = true;
                    jumping = false;
                    landed = true;
                    targetY = currentY;
                    break;
                }

                // Check side collision
                if (isCollidingWithPlatformSide(currentX, currentY, PLAYER_WIDTH, PLAYER_HEIGHT, platform)) {
                    if (velocityX > 0) {
                        currentX = platform.x - PLAYER_WIDTH;
                        targetX = currentX;
                    }
                    if (velocityX < 0) {
                        currentX = platform.x + platform.width;
                        targetX = currentX;
                    }
                    velocityX = 0;
                }
            }

            // Update last placed echo collision status
            if (lastPlacedEcho != null) {
                Rectangle playerRect = new Rectangle(currentX, currentY, PLAYER_WIDTH, PLAYER_HEIGHT);
                Rectangle echoRect = new Rectangle(lastPlacedEcho.x, lastPlacedEcho.y, ECHO_WIDTH, ECHO_HEIGHT);

                if (!playerRect.intersects(echoRect)) {
                    lastPlacedEcho.collisionEnabled = true;
                    lastPlacedEcho = null;
                }
            }

            // Check echo collisions
            for (Echo echo : echoes) {
                if (echo.collisionEnabled) {
                    // Check top collision
                    if (isCollidingWithEchoTop(currentX, currentY, PLAYER_WIDTH, PLAYER_HEIGHT, echo)) {
                        currentY = echo.y - PLAYER_HEIGHT;
                        velocityY = 0;
                        onGround = true;
                        jumping = false;
                        landed = true;
                        targetY = currentY;
                        break;
                    }

                    // Check side collision
                    if (isCollidingWithEchoSide(currentX, currentY, PLAYER_WIDTH, PLAYER_HEIGHT, echo)) {
                        if (velocityX > 0) {
                            currentX = echo.x - PLAYER_WIDTH;
                            targetX = currentX;
                        }
                        if (velocityX < 0) {
                            currentX = echo.x + ECHO_WIDTH;
                            targetX = currentX;
                        }
                        velocityX = 0;
                    }
                }
            }

            playerX = currentX;
            playerY = currentY;

            if (landed) break;
        }

        onGround = landed;

        // Update final position
        playerX = targetX;
        playerY = targetY;

        // Fall through the gap? Respawn at the top
        if (playerY > getHeight()) {
            playerX = 100;
            playerY = 100;
            velocityY = 0;
        }

        // Remove expired echoes
        Iterator<Echo> iter = echoes.iterator();
        while (iter.hasNext()) {
            Echo echo = iter.next();
            echo.delay--;
            if (echo.delay < 0) {
                iter.remove();
            }
        }

        repaint();
    }

    // Rest of the code remains the same...
    private boolean isCollidingWithPlatformTop(int x, int y, int width, int height, Platform platform) {
        return x < platform.x + platform.width &&
                x + width > platform.x &&
                y + height >= platform.y &&
                y + height <= platform.y + 10;
    }

    private boolean isCollidingWithEchoTop(int x, int y, int width, int height, Echo echo) {
        return x < echo.x + ECHO_WIDTH &&
                x + width > echo.x &&
                y + height >= echo.y &&
                y + height <= echo.y + 10;
    }

    private boolean isCollidingWithPlatformSide(int x, int y, int width, int height, Platform platform) {
        return x + width > platform.x &&
                x < platform.x + platform.width &&
                y + height > platform.y &&
                y < platform.y + platform.height;
    }

    private boolean isCollidingWithEchoSide(int x, int y, int width, int height, Echo echo) {
        return x + width > echo.x &&
                x < echo.x + ECHO_WIDTH &&
                y + height > echo.y &&
                y < echo.y + ECHO_HEIGHT;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());

        // Draw player
        g.setColor(Color.RED);
        g.fillRect(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);

        // Draw platforms
        g.setColor(Color.BLACK);
        for (Platform platform : platforms) {
            g.fillRect(platform.x, platform.y, platform.width, platform.height);
        }

        // Draw echoes
        for (Echo echo : echoes) {
            g.setColor(new Color(100, 100, 100, echo.collisionEnabled ? 200 : 150));
            g.fillRect(echo.x, echo.y, ECHO_WIDTH, ECHO_HEIGHT);
        }

        //Draw player coordinates
        g.drawString("x: " + playerX + ", y: " + playerY, 20, 20);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && onGround) {
            velocityY = -15;
            jumping = true;
            onGround = false;
        }
        if (e.getKeyCode() == KeyEvent.VK_E) {
            Echo newEcho = new Echo(playerX, playerY);
            echoes.add(newEcho);
            lastPlacedEcho = newEcho;
        }
        if (e.getKeyCode() == KeyEvent.VK_D) dPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_A) aPressed = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_D) dPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_A) aPressed = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private class Echo {
        int x, y;
        boolean collisionEnabled = false;
        int delay;

        Echo(int x, int y) {
            this.x = x;
            this.y = y;
            this.delay = 200;
        }
    }

    private class Platform {
        int x, y, width, height;

        Platform(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    public static void main(String[] args) {
        new EchoJumpGame();
    }
}