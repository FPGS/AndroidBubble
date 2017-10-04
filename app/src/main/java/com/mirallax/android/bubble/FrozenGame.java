package com.mirallax.android.bubble;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.mirallax.android.bubble.manager.BubbleManager;
import com.mirallax.android.bubble.manager.LevelManager;
import com.mirallax.android.bubble.sprite.BmpWrap;
import com.mirallax.android.bubble.sprite.BubbleSprite;
import com.mirallax.android.bubble.sprite.ImageSprite;
import com.mirallax.android.bubble.sprite.LaunchBubbleSprite;
import com.mirallax.android.bubble.sprite.Sprite;

import java.util.ArrayList;
import java.util.Vector;
import java.util.Random;

public class FrozenGame extends GameScreen {
    private final static int HORIZONTAL_MOVE = 0;
    private final static int FIRE = 1;

    private final static int KEY_UP = 38;
    private final static int KEY_LEFT = 37;
    private final static int KEY_RIGHT = 39;

    private boolean youWin = false;

    private final ArrayList<BmpWrap> bubbles;
    private final Random rndm;

    private LaunchBubbleSprite launchBubble;
    private double launchBubblePosition;

    private final Compressor compressor;

    private ImageSprite nextBubble;
    private int currentColor;
    private int nextColor;

    private BubbleSprite movingBubble;
    private final BubbleManager bubbleManager;
    private final LevelManager levelManager;

    private Vector <Sprite> jumping;
    private Vector <Sprite> falling;

    private BubbleSprite[][] bPlay;

    private int fixedBubbles;
    private double mDown;

    private int nbBubbles;

    private ImageSprite runForrestRun;
    private int limitTime;

    private boolean readyToFire;
    private boolean gameOver;

    public FrozenGame(ArrayList<BmpWrap> bubbles,
                      BmpWrap hurry_arg,
                      BmpWrap compressorHead_arg,
                      Drawable launcher_arg,
                      LevelManager levelManager_arg) {
        rndm = new Random(System.currentTimeMillis());
        this.bubbles = bubbles;
        this.levelManager = levelManager_arg;

        launchBubblePosition = 20;

        this.compressor = new Compressor(compressorHead_arg);

        runForrestRun = new ImageSprite(new Rect(203, 265, 203 + 240, 265 + 90),
                hurry_arg);

        jumping = new Vector<>();
        falling = new Vector<>();

        bPlay = new BubbleSprite[8][13];

        bubbleManager = new BubbleManager(bubbles);
        byte[][] currentLevel = levelManager.getCurrentLevel();

        if (currentLevel == null) {
            return;
        }

        for (int j = 0; j < 12; j++) {
            for (int i = j % 2; i < 8; i++) {
                if (currentLevel[i][j] != -1) {
                    BubbleSprite newOne = new BubbleSprite(
                            new Rect(190 + i * 32 - (j % 2) * 16, 44 + j * 28, 32, 32),
                            currentLevel[i][j],
                            bubbles.get(currentLevel[i][j]),
                            bubbleManager,
                            this);
                    bPlay[i][j] = newOne;
                    this.addSprite(newOne);
                }
            }
        }

        currentColor = bubbleManager.nextBubbleIndex(rndm);
        nextColor = bubbleManager.nextBubbleIndex(rndm);

        nextBubble = new ImageSprite(new Rect(302, 440, 302 + 32, 440 + 32),
                bubbles.get(nextColor));
        this.addSprite(nextBubble);

        launchBubble = new LaunchBubbleSprite(currentColor,
                (int) launchBubblePosition,
                launcher_arg, bubbles);

        this.spriteToBack(launchBubble);

        nbBubbles = 0;
    }

    public void saveState(Bundle map) {
        ArrayList<Sprite> savedSprites = new ArrayList<>();
        saveSprites(map, savedSprites);
        for (int i = 0; i < jumping.size(); i++) {
            jumping.elementAt(i).saveState(map, savedSprites);
            String jumping = "jumping-%d";
            map.putInt(String.format(jumping, i),
                    this.jumping.elementAt(i).getSavedId());
        }
        map.putInt("numJumpingSprites", jumping.size());
        for (int i = 0; i < falling.size(); i++) {
            falling.elementAt(i).saveState(map, savedSprites);
            map.putInt(String.format("falling-%d", i),
                    falling.elementAt(i).getSavedId());
        }
        map.putInt("numFallingSprites", falling.size());
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 13; j++) {
                if (bPlay[i][j] != null) {
                    bPlay[i][j].saveState(map, savedSprites);
                    map.putInt(String.format("play-%d-%d", i, j),
                            bPlay[i][j].getSavedId());
                } else {
                    map.putInt(String.format("play-%d-%d", i, j), -1);
                }
            }
        }
        launchBubble.saveState(map, savedSprites);
        map.putInt("launchBubbleId", launchBubble.getSavedId());
        map.putDouble("launchBubblePosition", launchBubblePosition);
        compressor.saveState(map);
        nextBubble.saveState(map, savedSprites);
        map.putInt("nextBubbleId", nextBubble.getSavedId());
        map.putInt("currentColor", currentColor);
        map.putInt("nextColor", nextColor);
        if (movingBubble != null) {
            movingBubble.saveState(map, savedSprites);
            map.putInt("movingBubbleId", movingBubble.getSavedId());
        } else {
            map.putInt("movingBubbleId", -1);
        }
        bubbleManager.saveState(map);
        map.putInt("fixedBubbles", fixedBubbles);
        map.putDouble("mDown", mDown);
        map.putInt("nbBubbles", nbBubbles);
        runForrestRun.saveState(map, savedSprites);
        map.putInt("hurryId", runForrestRun.getSavedId());
        map.putInt("limitTime", limitTime);
        map.putBoolean("readyToFire", readyToFire);
        map.putBoolean("gameOver", gameOver);

        map.putInt("numSavedSprites", savedSprites.size());

        for (int i = 0; i < savedSprites.size(); i++) {
            (savedSprites.get(i)).clearSavedId();
        }
    }

    private Sprite restoreSprite(Bundle map, Vector imageList, int i) {
        int left = map.getInt(String.format("%d-left", i));
        int right = map.getInt(String.format("%d-right", i));
        int top = map.getInt(String.format("%d-top", i));
        int bottom = map.getInt(String.format("%d-bottom", i));
        int type = map.getInt(String.format("%d-type", i));
        if (type == Sprite.TYPE_BUBBLE) {
            int color = map.getInt(String.format("%d-color", i));
            double moveX = map.getDouble(String.format("%d-moveX", i));
            double moveY = map.getDouble(String.format("%d-moveY", i));
            double realX = map.getDouble(String.format("%d-realX", i));
            double realY = map.getDouble(String.format("%d-realY", i));
            boolean fixed = map.getBoolean(String.format("%d-fixed", i));
            boolean released = map.getBoolean(String.format("%d-released", i));
            boolean checkJump = map.getBoolean(String.format("%d-checkJump", i));
            boolean checkFall = map.getBoolean(String.format("%d-checkFall", i));
            int fixedAnim = map.getInt(String.format("%d-fixedAnim", i));
            return new BubbleSprite(new Rect(left, top, right, bottom),
                    color, moveX, moveY, realX, realY,
                    fixed, released, checkJump, checkFall,
                    fixedAnim,
                    bubbles.get(color),
                    bubbleManager, this);
        } else if (type == Sprite.TYPE_IMAGE) {
            int imageId = map.getInt(String.format("%d-imageId", i));
            return new ImageSprite(new Rect(left, top, right, bottom),
                    (BmpWrap) imageList.elementAt(imageId));
        } else {
            Log.e("frozen-bubble", "Unrecognized sprite type: " + type);
            return null;
        }
    }

    public void restoreState(Bundle map, Vector imageList) {
        Vector <Sprite> savedSprites = new Vector<>();
        int numSavedSprites = map.getInt("numSavedSprites");
        for (int i = 0; i < numSavedSprites; i++) {
            savedSprites.addElement(restoreSprite(map, imageList, i));
        }

        restoreSprites(map, savedSprites);
        jumping = new Vector<>();
        int numJumpingSprites = map.getInt("numJumpingSprites");
        for (int i = 0; i < numJumpingSprites; i++) {
            int spriteIdx = map.getInt(String.format("jumping-%d", i));
            jumping.addElement(savedSprites.elementAt(spriteIdx));
        }
        falling = new Vector<>();
        int numFallingSprites = map.getInt("numFallingSprites");
        for (int i = 0; i < numFallingSprites; i++) {
            int spriteIdx = map.getInt(String.format("falling-%d", i));
            falling.addElement(savedSprites.elementAt(spriteIdx));
        }
        bPlay = new BubbleSprite[8][13];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 13; j++) {
                int spriteIdx = map.getInt(String.format("play-%d-%d", i, j));
                if (spriteIdx != -1) {
                    bPlay[i][j] = (BubbleSprite) savedSprites.elementAt(spriteIdx);
                } else {
                    bPlay[i][j] = null;
                }
            }
        }
        int launchBubbleId = map.getInt("launchBubbleId");
        launchBubble = (LaunchBubbleSprite) savedSprites.elementAt(launchBubbleId);
        launchBubblePosition = map.getDouble("launchBubblePosition");
        compressor.restoreState(map);
        int nextBubbleId = map.getInt("nextBubbleId");
        nextBubble = (ImageSprite) savedSprites.elementAt(nextBubbleId);
        currentColor = map.getInt("currentColor");
        nextColor = map.getInt("nextColor");
        int movingBubbleId = map.getInt("movingBubbleId");
        if (movingBubbleId == -1) {
            movingBubble = null;
        } else {
            movingBubble = (BubbleSprite) savedSprites.elementAt(movingBubbleId);
        }
        bubbleManager.restoreState(map);
        fixedBubbles = map.getInt("fixedBubbles");
        mDown = map.getDouble("mDown");
        nbBubbles = map.getInt("nbBubbles");
        int hurryId = map.getInt("hurryId");
        runForrestRun = (ImageSprite) savedSprites.elementAt(hurryId);
        limitTime = map.getInt("limitTime");
        readyToFire = map.getBoolean("readyToFire");
        gameOver = map.getBoolean("gameOver");
    }


    public BubbleSprite[][] getGrid() {
        return bPlay;
    }

    public void addFallingBubble(BubbleSprite sprite) {
        spriteToFront(sprite);
        falling.addElement(sprite);
    }

    public void deleteFallingBubble(BubbleSprite sprite) {
        removeSprite(sprite);
        falling.removeElement(sprite);
    }

    public void addJumpingBubble(BubbleSprite sprite) {
        spriteToFront(sprite);
        jumping.addElement(sprite);
    }

    public void deleteJumpingBubble(BubbleSprite sprite) {
        removeSprite(sprite);
        jumping.removeElement(sprite);
    }

    public Random getRndm() {
        return rndm;
    }

    public double getmDown() {
        return mDown;
    }

    private void sendBubblesDown() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 12; j++) {
                if (bPlay[i][j] != null) {
                    bPlay[i][j].moveDown();

                    if (bPlay[i][j].getSpritePosition().y >= 380) {
                        gameOver = true;
                    }
                }
            }
        }

        mDown += 28.;
        compressor.moveDown();
    }

    public boolean play(boolean key_left, boolean key_right, boolean key_fire,
                        double trackball_dx, double touch_dx) {
        int[] move = new int[2];

        if (key_left && !key_right) {
            move[HORIZONTAL_MOVE] = KEY_LEFT;
        } else if (key_right && !key_left) {
            move[HORIZONTAL_MOVE] = KEY_RIGHT;
        } else {
            move[HORIZONTAL_MOVE] = 0;
        }
        if (key_fire) {
            move[FIRE] = KEY_UP;
        } else {
            move[FIRE] = 0;
        }

        if (move[FIRE] == 0) {
            readyToFire = true;
        }

        if (gameOver) {
            if (move[FIRE] == KEY_UP && readyToFire) {
                if (youWin) {
                    levelManager.goToNextLevel();
                }
                return true;
            }
        } else {
            if (move[FIRE] == KEY_UP || limitTime > 480) {
                if (movingBubble == null && readyToFire) {
                    nbBubbles++;

                    movingBubble = new BubbleSprite(new Rect(302, 390, 32, 32),
                            (int) launchBubblePosition,
                            currentColor,
                            bubbles.get(currentColor),
                            bubbleManager, this);
                    this.addSprite(movingBubble);

                    currentColor = nextColor;
                    nextColor = bubbleManager.nextBubbleIndex(rndm);

                    nextBubble.changeImage(bubbles.get(nextColor));
                    launchBubble.changeColor(currentColor);

                    readyToFire = false;
                    limitTime = 0;
                    removeSprite(runForrestRun);
                }
            } else {
                double dx = 0;
                if (move[HORIZONTAL_MOVE] == KEY_LEFT) {
                    dx -= 1;
                }
                if (move[HORIZONTAL_MOVE] == KEY_RIGHT) {
                    dx += 1;
                }
                dx += trackball_dx;
                dx += touch_dx;
                launchBubblePosition += dx;
                if (launchBubblePosition < 1) {
                    launchBubblePosition = 1;
                }
                if (launchBubblePosition > 39) {
                    launchBubblePosition = 39;
                }
                launchBubble.changeDirection((int) launchBubblePosition);
            }
        }

        if (movingBubble != null) {
            movingBubble.move();
            if (movingBubble.fixed()) {
                if (movingBubble.getSpritePosition().y >= 380 &&
                        !movingBubble.released()) {
                    gameOver = true;
                } else if (bubbleManager.countBubbles() == 0) {
                    youWin = true;
                    gameOver = true;
                } else {
                    fixedBubbles++;
                    if (fixedBubbles == 8) {
                        fixedBubbles = 0;
                        sendBubblesDown();
                    }
                }
                movingBubble = null;
            }

            if (movingBubble != null) {
                movingBubble.move();
                if (movingBubble.fixed()) {
                    if (movingBubble.getSpritePosition().y >= 380 &&
                            !movingBubble.released()) {
                        gameOver = true;
                    } else if (bubbleManager.countBubbles() == 0) {
                        gameOver = true;
                        youWin = true;
                    } else {
                        fixedBubbles++;
                        if (fixedBubbles == 8) {
                            fixedBubbles = 0;
                            sendBubblesDown();
                        }
                    }
                    movingBubble = null;
                }
            }
        }

        if (movingBubble == null && !gameOver) {
            limitTime++;
            if (limitTime == 2) {
                removeSprite(runForrestRun);
            }
            if (limitTime >= 240) {
                if (limitTime % 40 == 10) {
                    addSprite(runForrestRun);
                } else if (limitTime % 40 == 35) {
                    removeSprite(runForrestRun);
                }
            }
        }

        for (int i = 0; i < falling.size(); i++) {
            ((BubbleSprite) falling.elementAt(i)).fall();
        }

        for (int i = 0; i < jumping.size(); i++) {
            ((BubbleSprite) jumping.elementAt(i)).jump();
        }

        return false;
    }

    public void paint(Canvas c, double scale, int dx, int dy) {
        compressor.paint(c, scale, dx, dy);
        nextBubble.changeImage(bubbles.get(nextColor));
        super.paint(c, scale, dx, dy);
    }
}
