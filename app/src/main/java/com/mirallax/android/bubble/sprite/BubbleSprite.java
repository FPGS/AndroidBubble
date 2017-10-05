package com.mirallax.android.bubble.sprite;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;

import com.mirallax.android.bubble.FrozenGame;
import com.mirallax.android.bubble.manager.BubbleManager;

import java.util.ArrayList;

public class BubbleSprite extends Sprite {
    private static double FALL_SPEED = 1.;
    private static double MAX_BUBBLE_SPEED = 8.;
    private static double MINIMUM_DISTANCE = 841.;

    private int color;
    private BmpWrap bubbleFace;
    private FrozenGame frozen;
    private BubbleManager bubbleManager;
    private double moveX, moveY;
    private double realX, realY;

    private boolean fixed;
    private boolean released;

    private boolean checkJump;
    private boolean checkFall;

    private int fixedAnim;


    public BubbleSprite(Rect area, int color, double moveX, double moveY,
                        double realX, double realY, boolean fixed,
                        boolean released, boolean checkJump, boolean checkFall,
                        int fixedAnim, BmpWrap bubbleFace,
                        BubbleManager bubbleManager,
                        FrozenGame frozen) {
        super(area);
        this.color = color;
        this.moveX = moveX;
        this.moveY = moveY;
        this.realX = realX;
        this.realY = realY;
        this.fixed = fixed;
        this.released = released;
        this.checkJump = checkJump;
        this.checkFall = checkFall;
        this.fixedAnim = fixedAnim;
        this.bubbleFace = bubbleFace;
        this.bubbleManager = bubbleManager;
        this.frozen = frozen;
    }

    public BubbleSprite(Rect area, int direction, int color, BmpWrap bubbleFace,
                        BubbleManager bubbleManager,
                        FrozenGame frozen) {
        super(area);

        this.color = color;
        this.bubbleFace = bubbleFace;
        this.bubbleManager = bubbleManager;
        this.frozen = frozen;

        this.moveX = MAX_BUBBLE_SPEED * -Math.cos(direction * Math.PI / 40.);
        this.moveY = MAX_BUBBLE_SPEED * -Math.sin(direction * Math.PI / 40.);
        this.realX = area.left;
        this.realY = area.top;

        fixed = false;
        fixedAnim = -1;
    }

    public BubbleSprite(Rect area, int color, BmpWrap bubbleFace,
                        BubbleManager bubbleManager,
                        FrozenGame frozen) {
        super(area);

        this.color = color;
        this.bubbleFace = bubbleFace;
        this.bubbleManager = bubbleManager;
        this.frozen = frozen;

        this.realX = area.left;
        this.realY = area.top;

        fixed = true;
        fixedAnim = -1;
        bubbleManager.addBubble(bubbleFace);
    }

    public void saveState(Bundle map, ArrayList savedSprites) {
        if (getSavedId() != -1) {
            return;
        }
        super.saveState(map, savedSprites);
        String color = "%d-color";
        map.putInt(String.format(color, getSavedId()), this.color);
        String moveX = "%d-moveX";
        map.putDouble(String.format(moveX, getSavedId()), this.moveX);
        String moveY = "%d-moveY";
        map.putDouble(String.format(moveY, getSavedId()), this.moveY);
        String realX = "%d-realX";
        map.putDouble(String.format(realX, getSavedId()), this.realX);
        String realY = "%d-realY";
        map.putDouble(String.format(realY, getSavedId()), this.realY);
        String fixed = "%d-fixed";
        map.putBoolean(String.format(fixed, getSavedId()), this.fixed);
        String released = "%d-released";
        map.putBoolean(String.format(released, getSavedId()), this.released);
        String checkJump = "%d-checkJump";
        map.putBoolean(String.format(checkJump, getSavedId()), this.checkJump);
        String checkFall = "%d-checkFall";
        map.putBoolean(String.format(checkFall, getSavedId()), this.checkFall);
        String fixedAnim = "%d-fixedAnim";
        map.putInt(String.format(fixedAnim, getSavedId()), this.fixedAnim);
    }

    public int getTypeId() {
        return TYPE_BUBBLE;
    }

    Point currentPosition() {
        int posY = (int) Math.floor((realY - 28. - frozen.getmDown()) / 28.);
        int posX = (int) Math.floor((realX - 174.) / 32. + 0.5 * (posY % 2));

        if (posX > 7) {
            posX = 7;
        }

        if (posX < 0) {
            posX = 0;
        }

        if (posY < 0) {
            posY = 0;
        }

        return new Point(posX, posY);
    }

    public void removeFromManager() {
        bubbleManager.removeBubble(bubbleFace);
    }

    public boolean fixed() {
        return fixed;
    }

    public boolean checked() {
        return checkFall;
    }

    public boolean released() {
        return released;
    }

    public void moveDown() {
        if (fixed) {
            realY += 28.;
        }

        super.absoluteMove(new Point((int) realX, (int) realY));
    }

    public void move() {
        realX += moveX;

        if (realX >= 414.) {
            moveX = -moveX;
            realX += (414. - realX);
        } else if (realX <= 190.) {
            moveX = -moveX;
            realX += (190. - realX);
        }

        realY += moveY;

        Point currentPosition = currentPosition();
        ArrayList neighbors = getNeighbors(currentPosition);

        if (checkCollision(neighbors) || realY < 44. + frozen.getmDown()) {
            realX = 190. + currentPosition.x * 32 - (currentPosition.y % 2) * 16;
            realY = 44. + currentPosition.y * 28 + frozen.getmDown();

            fixed = true;

            ArrayList checkJump = new ArrayList();
            this.checkJump(checkJump, neighbors);

            BubbleSprite[][] grid = frozen.getGrid();

            if (checkJump.size() >= 3) {
                released = true;

                for (int i = 0; i < checkJump.size(); i++) {
                    BubbleSprite current = (BubbleSprite) checkJump.get(i);
                    Point currentPoint = current.currentPosition();

                    frozen.addJumpingBubble(current);
                    if (i > 0) {
                        current.removeFromManager();
                    }
                    grid[currentPoint.x][currentPoint.y] = null;
                }

                for (int i = 0; i < 8; i++) {
                    if (grid[i][0] != null) {
                        grid[i][0].checkFall();
                    }
                }

                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 12; j++) {
                        if (grid[i][j] != null) {
                            if (!grid[i][j].checked()) {
                                frozen.addFallingBubble(grid[i][j]);
                                grid[i][j].removeFromManager();
                                grid[i][j] = null;
                            }
                        }
                    }
                }

            } else {
                bubbleManager.addBubble(bubbleFace);
                grid[currentPosition.x][currentPosition.y] = this;
                moveX = 0.;
                moveY = 0.;
                fixedAnim = 0;
            }
        }

        super.absoluteMove(new Point((int) realX, (int) realY));
    }

    ArrayList getNeighbors(Point point) {
        BubbleSprite[][] grid = frozen.getGrid();

        ArrayList list = new ArrayList();

        if ((point.y % 2) == 0) {
            if (point.x > 0) {
                list.add(grid[point.x - 1][point.y]);
            }

            if (point.x < 7) {
                list.add(grid[point.x + 1][point.y]);

                if (point.y > 0) {
                    list.add(grid[point.x][point.y - 1]);
                    list.add(grid[point.x + 1][point.y - 1]);
                }

                if (point.y < 12) {
                    list.add(grid[point.x][point.y + 1]);
                    list.add(grid[point.x + 1][point.y + 1]);
                }
            } else {
                if (point.y > 0) {
                    list.add(grid[point.x][point.y - 1]);
                }

                if (point.y < 12) {
                    list.add(grid[point.x][point.y + 1]);
                }
            }
        } else {
            if (point.x < 7) {
                list.add(grid[point.x + 1][point.y]);
            }

            if (point.x > 0) {
                list.add(grid[point.x - 1][point.y]);

                if (point.y > 0) {
                    list.add(grid[point.x][point.y - 1]);
                    list.add(grid[point.x - 1][point.y - 1]);
                }

                if (point.y < 12) {
                    list.add(grid[point.x][point.y + 1]);
                    list.add(grid[point.x - 1][point.y + 1]);
                }
            } else {
                if (point.y > 0) {
                    list.add(grid[point.x][point.y - 1]);
                }

                if (point.y < 12) {
                    list.add(grid[point.x][point.y + 1]);
                }
            }
        }

        return list;
    }

    void checkJump(ArrayList jump, BmpWrap compare) {
        if (checkJump) {
            return;
        }
        checkJump = true;

        if (this.bubbleFace == compare) {
            checkJump(jump, this.getNeighbors(this.currentPosition()));
        }
    }

    void checkJump(ArrayList jump, ArrayList neighbors) {
        jump.add(this);

        for (int i = 0; i < neighbors.size(); i++) {
            BubbleSprite current = (BubbleSprite) neighbors.get(i);

            if (current != null) {
                current.checkJump(jump, this.bubbleFace);
            }
        }
    }

    public void checkFall() {
        if (checkFall) {
            return;
        }
        checkFall = true;

        ArrayList v = this.getNeighbors(this.currentPosition());

        for (int i = 0; i < v.size(); i++) {
            BubbleSprite current = (BubbleSprite) v.get(i);

            if (current != null) {
                current.checkFall();
            }
        }
    }

    boolean checkCollision(ArrayList neighbors) {
        for (int i = 0; i < neighbors.size(); i++) {
            BubbleSprite current = (BubbleSprite) neighbors.get(i);

            if (current != null) {
                if (checkCollision(current)) {
                    return true;
                }
            }
        }

        return false;
    }

    boolean checkCollision(BubbleSprite sprite) {
        double value =
                (sprite.getSpriteArea().left - this.realX) *
                        (sprite.getSpriteArea().left - this.realX) +
                        (sprite.getSpriteArea().top - this.realY) *
                                (sprite.getSpriteArea().top - this.realY);

        return (value < MINIMUM_DISTANCE);
    }

    public void jump() {
        if (fixed) {
            moveX = -6. + frozen.getRndm().nextDouble() * 12.;
            moveY = -5. - frozen.getRndm().nextDouble() * 10.;

            fixed = false;
        }

        moveY += FALL_SPEED;
        realY += moveY;
        realX += moveX;

        super.absoluteMove(new Point((int) realX, (int) realY));

        if (realY >= 680.) {
            frozen.deleteJumpingBubble(this);
        }
    }

    public void fall() {
        if (fixed) {
            moveY = frozen.getRndm().nextDouble() * 5.;
        }

        fixed = false;

        moveY += FALL_SPEED;
        realY += moveY;

        super.absoluteMove(new Point((int) realX, (int) realY));

        if (realY >= 680.) {
            frozen.deleteFallingBubble(this);
        }
    }


    public final void paint(Canvas c, double scale, int dx, int dy) {
        checkJump = false;
        checkFall = false;

        Point point = getSpritePosition();

        drawImage(bubbleFace, point.x, point.y, c, scale, dx, dy);

        if (fixedAnim != -1) {
            fixedAnim++;
            if (fixedAnim == 6) {
                fixedAnim = -1;
            }
        }
    }
}
