import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

// Abstract base class for all collidable objects
abstract class Collidable
{
    protected int x, y, width, height, layer;

    public Collidable(int x, int y, int width, int height)
    {
        this(x, y, width, height, 0); // Default layer 0
    }

    public Collidable(int x, int y, int width, int height, int layer)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.layer = layer;
    }

    public boolean isCollidingWithTop(int objX, int objY, int objWidth, int objHeight, int playerLayer)
    {
        if (this.layer != playerLayer) return false;

        return objX < x + width &&
                objX + objWidth > x &&
                objY + objHeight >= y &&
                objY + objHeight <= y + 10;
    }

    public boolean isCollidingWithSide(int objX, int objY, int objWidth, int objHeight, int playerLayer)
    {
        if (this.layer != playerLayer) return false;

        return objX + objWidth > x &&
                objX < x + width &&
                objY + objHeight > y &&
                objY < y + height;
    }

    public abstract void draw(Graphics g, int playerLayer); // Modified draw method
}

class Platform extends Collidable
{
    private Color color;

    public Platform(int x, int y, int width, int height, Color color)
    {
        this(x, y, width, height, color, 0);
    }

    public Platform(int x, int y, int width, int height, Color color, int layer)
    {
        super(x, y, width, height, layer);
        this.color = color;
    }

    @Override
    public void draw(Graphics g, int playerLayer) // Implementation of modified draw
    {
        if (this.layer != playerLayer)
        {
            Color opaqueColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);
            g.setColor(opaqueColor);
        } else
        {
            g.setColor(color); // Default color for same layer
        }
        g.fillRect(x, y, width, height);
    }

    @Override
    public boolean isCollidingWithTop(int objX, int objY, int objWidth, int objHeight, int playerLayer)
    {
        return super.isCollidingWithTop(objX, objY, objWidth, objHeight, playerLayer) &&
                objX + 5 < x + width && // Left edge of player is to the left of the platform's right edge
                objX + objWidth - 5 > x && // Right edge of player is to the right of the platform's left edge
                objY + objHeight >= y && // Player's bottom edge is at or below the platform's top edge
                objY + objHeight <= y + 5; // Player's bottom edge is within 5 pixels of the platform's top edge
    }

    @Override
    public boolean isCollidingWithSide(int objX, int objY, int objWidth, int objHeight, int playerLayer)
    {
        return super.isCollidingWithSide(objX, objY, objWidth, objHeight, playerLayer);
    }
}

class Mountain extends Collidable
{
    private Color color;

    public Mountain(int x, int y, int width, int height, Color color)
    {
        this(x, y, width, height, color, 0);
    }

    public Mountain(int x, int y, int width, int height, Color color, int layer)
    {
        super(x, y, width, height, layer);
        this.color = color;
    }

    @Override
    public void draw(Graphics g, int playerLayer) // Implementation of modified draw
    {
        if (this.layer != playerLayer)
        {
            Color opaqueColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);
            g.setColor(opaqueColor);
        } else
        {
            g.setColor(color); // Default color for same layer
        }
        g.fillRect(x, y, width, height / 2); // Draw upper rectangle
        g.fillArc(x, y + height / 4, width, height / 2, 180, 180);
    }

    @Override
    public boolean isCollidingWithTop(int objX, int objY, int objWidth, int objHeight, int playerLayer)
    {
        if (!super.isCollidingWithTop(objX, objY, objWidth, objHeight, playerLayer)) return false;

        // Ensure collision is only checked if object is within the mountain's X bounds
        if (objX + objWidth < x || objX > x + width)
        {
            return false;
        }

        // Collision with flat part
        if (objY + objHeight >= y && objY + objHeight <= y + 5)
        {
            return true;
        }

        // Collision with curved part (treat it as a bounding box for simplicity)
        int centerX = x + width / 2;
        int centerY = y + height;
        int radius = width / 2;

        int objCenterX = objX + objWidth / 2;
        int objBottomY = objY + objHeight;

        double dist = Math.sqrt(Math.pow(objCenterX - centerX, 2) + Math.pow(objBottomY - centerY, 2));
        return dist <= radius;
    }

    @Override
    public boolean isCollidingWithSide(int objX, int objY, int objWidth, int objHeight, int playerLayer)
    {
        return super.isCollidingWithSide(objX, objY, objWidth, objHeight, playerLayer);
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
        this(x, y, width, height, history, startDelay, 0);
    }

    public Echo(int x, int y, int width, int height, ArrayList<Point> history, int startDelay, int layer)
    {
        super(x, y, width, height, layer);
        this.moveHistory = history;
        this.delayCounter = startDelay;
        this.lastPosition = new Point(x, y);
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
    public void draw(Graphics g, int playerLayer) // Implementation of modified draw
    {
        if (this.layer != playerLayer)
        {
            g.setColor(new Color(100, 100, 100, 50)); // Opaque grey for echo when different layer
        } else
        {
            g.setColor(new Color(100, 100, 100)); // Default grey for same layer
        }
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

    @Override
    public boolean isCollidingWithTop(int objX, int objY, int objWidth, int objHeight, int playerLayer)
    {
        return super.isCollidingWithTop(objX, objY, objWidth, objHeight, playerLayer);
    }

    @Override
    public boolean isCollidingWithSide(int objX, int objY, int objWidth, int objHeight, int playerLayer)
    {
        return super.isCollidingWithSide(objX, objY, objWidth, objHeight, playerLayer);
    }
}

class Button extends Collidable
{
    private Runnable action;
    private Color color;

    public Button(int x, int y, int width, int height, Color color, Runnable action)
    {
        this(x, y, width, height, color, action, 0);
    }

    public Button(int x, int y, int width, int height, Color color, Runnable action, int layer)
    {
        super(x, y, width, height, layer);
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
    public void draw(Graphics g, int playerLayer) // Implementation of modified draw
    {
        if (this.layer != playerLayer)
        {
            Color opaqueColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);
            g.setColor(opaqueColor);
        } else
        {
            g.setColor(color); // Default color for same layer
        }
        g.fillRect(x, y, width, height);
    }

    @Override
    public boolean isCollidingWithSide(int objX, int objY, int objWidth, int objHeight, int playerLayer)
    {
        return super.isCollidingWithSide(objX, objY, objWidth, objHeight, playerLayer);
    }

    @Override
    public boolean isCollidingWithTop(int objX, int objY, int objWidth, int objHeight, int playerLayer)
    {
        return super.isCollidingWithTop(objX, objY, objWidth, objHeight, playerLayer);
    }
}

public class EchoMovementGame extends JPanel implements KeyListener
{
    private int playerX = 100, playerY = 100;
    private int velocityY = 0, velocityX = 0;
    private boolean aPressed = false, dPressed = false;
    private boolean jumping = false, onGround = true;
    private Echo currentPlatform = null;
    private int layer = 0; // Player layer is 0
    private int level = 1;

    private final int PLAYER_WIDTH = 50, PLAYER_HEIGHT = 50;
    private final int JUMP_FORCE = -15;
    private final int ECHO_WIDTH = 50, ECHO_HEIGHT = 50;
    private final int MOVEMENT_HISTORY_LENGTH = 180;
    private final int ECHO_START_DELAY = 0;
    private boolean canJump = true; // New flag to control jump availability

    private ArrayList<Echo> echoes = new ArrayList<>();
    private ArrayList<Platform> platforms = new ArrayList<>();
    private ArrayList<Platform> disappearingPlatforms = new ArrayList<>();
    private ArrayList<Mountain> mountains = new ArrayList<>();
    private Queue<Point> movementHistory = new LinkedList<>();
    private ArrayList<Button> buttons = new ArrayList<>();
    private boolean levelComplete = false;

    public EchoMovementGame()
    {
        JFrame frame = new JFrame("Echo Jump");
        frame.setSize(1500, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.addKeyListener(this);
        frame.setVisible(true);

        initializeLevel(level);

        Timer timer = new Timer(16, e -> gameLoop());
        timer.start();
    }

    private void clearLevel()
    {
        movementHistory.clear();
        mountains.clear();
        disappearingPlatforms.clear();
        platforms.clear();
        echoes.clear();
        buttons.clear();
    }

    private void initializeLevel(int level)
    {
        clearLevel();
        switch (level)
        {
            case 1:
                // Add regular platforms
                platforms.add(new Platform(50, 150, 200, 20, Color.BLACK));
                platforms.add(new Platform(100, 400, 200, 20, Color.BLACK));
                platforms.add(new Platform(900, 400, 200, 20, Color.BLACK));
                platforms.add(new Platform(50, 120, 20, 200, Color.BLACK));

                // Add disappearing platforms
                addDisappearingPlatforms();

                // Add buttons with different actions
                buttons.add(new Button(1050, 350, 40, 40, Color.GREEN, this::clearDisappearingPlatforms));
                buttons.add(new Button(110, 360, 40, 40, Color.CYAN, this::completeLevel));
                break;
            case 2:
                platforms.add(new Platform(50, 400, 200, 20, Color.BLACK, 0));
                mountains.add(new Mountain(400, 300, 100, 122, Color.GREEN, 1)); // Layer 1, opaque
                mountains.add(new Mountain(600, 300, 100, 122, Color.GREEN, 0)); // Layer 0, default opacity
                break;
        }

    }

    private void completeLevel()
    {
        levelComplete = true;
    }

    private void addDisappearingPlatforms()
    {
        disappearingPlatforms.add(new Platform(300, 400, 600, 20, Color.RED));
        disappearingPlatforms.add(new Platform(160, 170, 20, 230, Color.RED));
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
        }

        // Apply jump force if jumping
        if (jumping)
        {
            velocityY = JUMP_FORCE; // Apply initial jump force
            onGround = false; // Player is no longer on the ground
            jumping = false; // Reset jumping flag after applying jump force
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

    private void handleVerticalMovement(int currentX, int initialY, int targetY)
    {
        int steps = Math.abs(velocityY) + 1;
        boolean landed = false;

        for (int i = 1; i <= steps; i++)
        {
            float progress = (float) i / steps;
            int testY = initialY + (int) ((targetY - initialY) * progress);

            // Build a unified list of collidables (platforms, disappearingPlatforms, and echoes)
            ArrayList<Collidable> collidables = new ArrayList<>();
            collidables.addAll(platforms);
            collidables.addAll(disappearingPlatforms);
            collidables.addAll(echoes);
            collidables.addAll(mountains);

            for (Collidable collidable : collidables)
            {
                // Only check top collision when falling
                if (velocityY >= 0 && collidable.isCollidingWithTop(currentX, testY, PLAYER_WIDTH, PLAYER_HEIGHT, layer))
                {
                    playerY = collidable.y - PLAYER_HEIGHT; // Snap the player to the top
                    velocityY = 0;
                    onGround = true;
                    landed = true;
                    canJump = true;
                    // If colliding with an echo, you might want to stick to it:
                    if (collidable instanceof Echo)
                    {
                        currentPlatform = (Echo) collidable;
                    }
                    break;
                }
                // Check for ceiling collisions when jumping upward
                if (velocityY < 0 && collidable.isCollidingWithSide(currentX, testY, PLAYER_WIDTH, PLAYER_HEIGHT, layer))
                {
                    playerY = collidable.y + collidable.height;
                    velocityY = 0;
                    break;
                }
            }

            if (!landed)
            {
                playerY = testY;
                onGround = false;
            } else
            {
                break;
            }
        }
    }

    private void handleHorizontalMovement(int initialX, int initialY, int targetX)
    {
        int steps = Math.abs(velocityX) + 1;
        for (int i = 1; i <= steps; i++)
        {
            float progress = (float) i / steps;
            int testX = initialX + (int) ((targetX - initialX) * progress);

            boolean collision = false;
            // Build the unified list of collidables
            ArrayList<Collidable> collidables = new ArrayList<>();
            collidables.addAll(platforms);
            collidables.addAll(disappearingPlatforms);
            collidables.addAll(echoes);
            collidables.addAll(mountains);

            for (Collidable collidable : collidables)
            {
                // Skip side collision if it's a top landing
                if (collidable.isCollidingWithTop(testX, initialY, PLAYER_WIDTH, PLAYER_HEIGHT, layer))
                {
                    continue;
                }
                if (collidable.isCollidingWithSide(testX, initialY, PLAYER_WIDTH, PLAYER_HEIGHT, layer))
                {
                    collision = true;
                    if (velocityX > 0)
                    {
                        playerX = collidable.x - PLAYER_WIDTH;
                    } else if (velocityX < 0)
                    {
                        playerX = collidable.x + collidable.width;
                    }
                    velocityX = 0;
                    break;
                }
            }
            if (!collision)
            {
                playerX = testX;
            } else
            {
                break;
            }
        }
    }

    private void handleMovement()
    {
        int initialX = playerX;
        int initialY = playerY;

        if (currentPlatform != null && currentPlatform.isActive())
        {
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

    private void checkButtonCollisions(int currentX, int currentY)
    {
        for (Button button : buttons)
        {
            if (button.isCollidingWithSide(currentX, currentY, PLAYER_WIDTH, PLAYER_HEIGHT, layer))
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
        levelComplete = false;
        initializeLevel(level);
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

        // Draw all game objects, passing player's layer
        platforms.forEach(platform -> platform.draw(g, layer));
        disappearingPlatforms.forEach(platform -> platform.draw(g, layer));
        echoes.forEach(echo -> echo.draw(g, layer));
        buttons.forEach(button -> button.draw(g, layer));
        mountains.forEach(mountain -> mountain.draw(g, layer));

        if (levelComplete)
        {
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.setColor(Color.GREEN);
            g.drawString("Level Complete! Press R to restart", getWidth() / 2 - 100, getHeight() / 2);
        }

        g.setColor(Color.red);
        g.drawString("onground: " + onGround, 20, 20);
        g.drawString("canJump: " + canJump, 20, 40);
        g.drawString("jumping: " + jumping, 20, 60);
        g.drawString("y velocity: " + velocityY, 20, 80);
        g.drawString("player x: " + playerX, 20, 100);
        g.drawString("player y: " + playerY, 20, 120);
        g.drawString("layer: "+ layer, 20, 140);
        g.drawString("level: " + level, 20, 160);

        drawControlsOverlay(g); // Call the new method to draw controls
    }

    private void drawControlsOverlay(Graphics g) {
        g.setColor(Color.BLACK); // Or any color that stands out against your background
        g.setFont(new Font("Arial", Font.PLAIN, 20)); // Choose a suitable font and size
        int x = getWidth() - 800; // X position from the right edge
        int y = 20; // Starting Y position
        int lineHeight = 22; // Spacing between lines

        g.drawString("Controls:", x, y);
        y += lineHeight;
        g.drawString("A + D: Move", x, y);
        y += lineHeight;
        g.drawString("Space: Jump", x, y);
        y += lineHeight;
        g.drawString("E: Spawn Echo that repeats your last three seconds of movement. Ride it!", x, y);
        y += lineHeight;
        g.drawString("R: Restart", x, y);
        y += lineHeight;
        g.drawString("number keys: Pick Level", x, y);
        y += lineHeight;
        g.drawString("Arrow up/down: change layer (try it in level 2!)", x, y);
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        switch (e.getKeyCode())
        {
            case KeyEvent.VK_SPACE:
                if (onGround && canJump)
                {
                    velocityY = JUMP_FORCE; // Apply jump force
                    jumping = true;
                    canJump = false; // Prevent double jumping
                    currentPlatform = null;
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
            case KeyEvent.VK_UP:
                if (level == 2) layer = layer == 0 ? 1 : 0; //the reason I have this duplicate bit is so I can easily add more layers (for example 0, 1, 2 -> rolls back over to 0). Also nice not at all confusing ternary, will be replaced with a simple if statement if more layers are added
                break;
            case KeyEvent.VK_DOWN:
                if (level == 2) layer = layer == 0 ? 1 : 0;
                break;
            case KeyEvent.VK_1:
                level = 1;
                restart();
                break;
            case KeyEvent.VK_2:
                level = 2;
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