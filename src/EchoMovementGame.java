import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

//todo: fix jumping sometimes needing multiple space presses, add echo collission

// Abstract base class for all collidable objects
abstract class Collidable
{
    protected int x, y, width, height;

    public Collidable(int x, int y, int width, int height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean isCollidingWithTop(int objX, int objY, int objWidth, int objHeight)
    {
        return objX < x + width &&
                objX + objWidth > x &&
                objY + objHeight >= y &&
                objY + objHeight <= y + 10;
    }

    public boolean isCollidingWithSide(int objX, int objY, int objWidth, int objHeight)
    {
        return objX + objWidth > x &&
                objX < x + width &&
                objY + objHeight > y &&
                objY < y + height;
    }

    public abstract void draw(Graphics g);
}

class Platform extends Collidable
{
    private Color color;

    public Platform(int x, int y, int width, int height, Color color)
    {
        super(x, y, width, height);
        this.color = color;
    }

    @Override
    public void draw(Graphics g)
    {
        g.setColor(color);
        g.fillRect(x, y, width, height);
    }

    @Override
    public boolean isCollidingWithTop(int objX, int objY, int objWidth, int objHeight)
    {
        return objX + 5 < x + width &&  // Added small offset to prevent edge cases
                objX + objWidth - 5 > x &&
                objY + objHeight >= y + 1 && // Added tolerance for landing
                objY + objHeight <= y + 10;
    }
}

class Echo extends Collidable
{
    private ArrayList<Point> moveHistory;
    private int currentIndex = 0;
    private int delayCounter;
    private boolean done = false;
    private Point lastPosition;

    public Echo(int x, int y, int width, int height, ArrayList<Point> history, int startDelay)
    {
        super(x, y, width, height);
        this.moveHistory = history;
        this.delayCounter = startDelay;
        this.lastPosition = new Point(x, y);
    }

    public Point getNextPosition()
    {
        if (currentIndex + 1 < moveHistory.size())
        {
            return moveHistory.get(currentIndex + 1);
        }
        return null;
    }

    public void update()
    {
        if (delayCounter > 0)
        {
            delayCounter--;
            return;
        }

        if (currentIndex < moveHistory.size())
        {
            lastPosition = new Point(x, y);
            Point nextPos = moveHistory.get(currentIndex);
            x = nextPos.x;
            y = nextPos.y;
            currentIndex++;
        } else
        {
            done = true;
        }
    }

    public Point getLastPosition()
    {
        return lastPosition;
    }

    @Override
    public void draw(Graphics g)
    {
        int alpha = delayCounter <= 0 ? 200 : 150;
        g.setColor(new Color(100, 100, 100, alpha));
        g.fillRect(x, y, width, height);
    }

    public boolean isDone()
    {
        return done;
    }

    public boolean isActive()
    {
        return delayCounter <= 0;
    }

    public Point getCurrentPosition()
    {
        return new Point(x, y);
    }
}

class Button extends Collidable
{
    private Runnable action;
    private Color color;

    public Button(int x, int y, int width, int height, Color color, Runnable action)
    {
        super(x, y, width, height);
        this.color = color;
        this.action = action;
    }

    public void trigger()
    {
        if (action != null)
        {
            action.run();
        }
    }

    @Override
    public void draw(Graphics g)
    {
        g.setColor(color);
        g.fillRect(x, y, width, height);
    }
}

public class EchoMovementGame extends JPanel implements KeyListener
{
    private int playerX = 100, playerY = 100;
    private int velocityY = 0, velocityX = 0;
    private boolean aPressed = false, dPressed = false;
    private boolean jumping = false, onGround = true;
    private Echo currentPlatform = null;

    private final int PLAYER_WIDTH = 50, PLAYER_HEIGHT = 50;
    private final int JUMP_FORCE = -15;
    private final int ECHO_WIDTH = 50, ECHO_HEIGHT = 50;
    private final int MOVEMENT_HISTORY_LENGTH = 180;
    private final int ECHO_START_DELAY = 0;
    private boolean canJump = true; // New flag to control jump availability

    private ArrayList<Echo> echoes = new ArrayList<>();
    private ArrayList<Platform> platforms = new ArrayList<>();
    private ArrayList<Platform> disappearingPlatforms = new ArrayList<>();
    private Queue<Point> movementHistory = new LinkedList<>();
    private ArrayList<Button> buttons = new ArrayList<>();

    public EchoMovementGame()
    {
        JFrame frame = new JFrame("Echo Jump");
        frame.setSize(1500, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.addKeyListener(this);
        frame.setVisible(true);

        initializeLevel();

        Timer timer = new Timer(16, e -> gameLoop());
        timer.start();
    }

    private void initializeLevel()
    {
        // Add regular platforms
        platforms.add(new Platform(50, 150, 200, 20, Color.BLACK));
        platforms.add(new Platform(100, 400, 200, 20, Color.BLACK));
        platforms.add(new Platform(900, 400, 200, 20, Color.BLACK));

        // Add disappearing platforms
        addDisappearingPlatforms();

        // Add buttons with different actions
        buttons.add(new Button(1050, 350, 40, 40, Color.GREEN, this::clearDisappearingPlatforms));
        // Example of adding another button with a different action
        buttons.add(new Button(800, 350, 40, 40, Color.BLUE, () -> System.out.println("Blue button pressed!")));
    }

    private void addDisappearingPlatforms()
    {
        disappearingPlatforms.add(new Platform(300, 400, 600, 20, Color.RED));
    }

    private void clearDisappearingPlatforms()
    {
        disappearingPlatforms.clear();
    }

    private void gameLoop()
    {
        killPlayer();
        recordPosition();
        updatePlayerVelocity();
        handleMovement();
        updateEchoes();
        repaint();
    }

    private void killPlayer()
    {
        if (playerY > 600) restart();
    }

    private void updatePlayerVelocity()
    {
        // Apply gravity if in the air
        if (!onGround)
        {
            velocityY += 1; // Gravity
        } else if (jumping)
        { // Apply initial jump force only ONCE
            velocityY = JUMP_FORCE;
            jumping = false; // Prevent repeated jumping while still in the air
            onGround = false; // Immediately set onGround to false after jump
        }

        // Handle horizontal movement
        if (aPressed && !dPressed)
        {
            velocityX = -10;
        } else if (dPressed && !aPressed)
        {
            velocityX = 10;
        } else
        {
            velocityX = 0;
        }
    }


    private void handleMovement() {
        int initialX = playerX;
        int initialY = playerY;

        if (currentPlatform != null && currentPlatform.isActive()) {
            Point currentPos = currentPlatform.getCurrentPosition();
            Point lastPos = currentPlatform.getLastPosition();
            int deltaX = currentPos.x - lastPos.x;
            int deltaY = currentPos.y - lastPos.y;
            initialX += deltaX;
            initialY += deltaY;
            playerX = initialX;
            playerY = initialY;
        }

        int targetX = initialX + velocityX;
        int targetY = initialY + velocityY;

        checkButtonCollisions(initialX, initialY);

        handleHorizontalMovement(initialX, initialY, targetX);
        handleVerticalMovement(playerX, initialY, targetY);
    }

    private void handleHorizontalMovement(int initialX, int initialY, int targetX) {
        int steps = Math.abs(velocityX) + 1;
        for (int i = 1; i <= steps; i++) {
            float progress = (float) i / steps;
            int testX = initialX + (int) ((targetX - initialX) * progress);

            boolean collision = false;
            ArrayList<Platform> allPlatforms = new ArrayList<>(platforms);
            allPlatforms.addAll(disappearingPlatforms);

            for (Platform platform : allPlatforms) {
                if (platform.isCollidingWithSide(testX, initialY, PLAYER_WIDTH, PLAYER_HEIGHT)) {
                    collision = true;
                    if (velocityX > 0) {
                        playerX = platform.x - PLAYER_WIDTH;
                    } else if (velocityX < 0) {
                        playerX = platform.x + platform.width;
                    }
                    velocityX = 0; // Stop horizontal movement on collision
                    break;
                }
            }

            if (!collision) {
                playerX = testX;
            } else {
                break;
            }
        }
    }

    private void handleVerticalMovement(int currentX, int initialY, int targetY) {
        int steps = Math.abs(velocityY) + 1;
        boolean landed = false;

        for (int i = 1; i <= steps; i++) {
            float progress = (float) i / steps;
            int testY = initialY + (int) ((targetY - initialY) * progress);

            ArrayList<Platform> allPlatforms = new ArrayList<>(platforms);
            allPlatforms.addAll(disappearingPlatforms);


            for (Platform platform : allPlatforms) {
                if (platform.isCollidingWithTop(currentX, testY, PLAYER_WIDTH, PLAYER_HEIGHT)) {
                    playerY = platform.y - PLAYER_HEIGHT;
                    velocityY = 0;
                    onGround = true;
                    landed = true;
                    canJump = true; // Enable jumping again immediately after landing
                    break;
                }

                if (velocityY < 0 && platform.isCollidingWithSide(currentX, testY, PLAYER_WIDTH, PLAYER_HEIGHT)) {
                    playerY = platform.y + platform.height;
                    velocityY = 0; // Stop vertical movement on ceiling collision
                    break;
                }
            }

            if (!landed) {
                playerY = testY;
                onGround = false;
            } else {
                break;
            }
        }
    }

    private void checkButtonCollisions(int currentX, int currentY)
    {
        for (Button button : buttons)
        {
            if (button.isCollidingWithSide(currentX, currentY, PLAYER_WIDTH, PLAYER_HEIGHT))
            {
                button.trigger();
            }
        }
    }

    private void updateEchoes()
    {
        Iterator<Echo> iter = echoes.iterator();
        while (iter.hasNext())
        {
            Echo echo = iter.next();
            echo.update();
            if (echo.isDone())
            {
                if (echo == currentPlatform)
                {
                    currentPlatform = null;
                }
                iter.remove();
            }
        }
    }

    private void recordPosition()
    {
        movementHistory.offer(new Point(playerX, playerY));
        if (movementHistory.size() > MOVEMENT_HISTORY_LENGTH)
        {
            movementHistory.poll();
        }
    }

    private void restart()
    {
        playerX = 100;
        playerY = 100;
        velocityY = 0;
        currentPlatform = null;
        addDisappearingPlatforms();
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());

        // Draw player
        g.setColor(Color.RED);
        g.fillRect(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);

        // Draw all game objects
        platforms.forEach(platform -> platform.draw(g));
        disappearingPlatforms.forEach(platform -> platform.draw(g));
        echoes.forEach(echo -> echo.draw(g));
        buttons.forEach(button -> button.draw(g));
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        switch (e.getKeyCode())
        {
            case KeyEvent.VK_SPACE:
                if (onGround && canJump)
                {
                    velocityY = -15;
                    jumping = true;
                }
                break;
            case KeyEvent.VK_E:
                ArrayList<Point> historyClone = new ArrayList<>(movementHistory);
                Echo newEcho = new Echo(playerX, playerY, ECHO_WIDTH, ECHO_HEIGHT,
                        historyClone, ECHO_START_DELAY);
                echoes.add(newEcho);
                break;
            case KeyEvent.VK_D:
                dPressed = true;
                break;
            case KeyEvent.VK_A:
                aPressed = true;
                break;
            case KeyEvent.VK_R:
                restart();
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_D) dPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_A) aPressed = false;
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
    }

    public static void main(String[] args)
    {
        new EchoMovementGame();
    }
}