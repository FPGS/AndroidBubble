package com.mirallax.android.bubble.sprite;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Vector;

public class LaunchBubbleSprite extends Sprite {
    private int currentColor;
    private int currentDirection;
    private Drawable launcher;
    private ArrayList<BmpWrap> bubbles;

    public LaunchBubbleSprite(int initialColor, int initialDirection,
                              Drawable launcher,
                              ArrayList bubbles) {
        super(new Rect(276, 362, 276 + 86, 362 + 76));

        currentColor = initialColor;
        currentDirection = initialDirection;
        this.launcher = launcher;
        this.bubbles = bubbles;
    }

    public void saveState(Bundle map, ArrayList saved_sprites) {
        if (getSavedId() != -1) {
            return;
        }
        super.saveState(map, saved_sprites);
        map.putInt(String.format("%d-currentColor", getSavedId()), currentColor);
        map.putInt(String.format("%d-currentDirection", getSavedId()),
                currentDirection);
    }

    public int getTypeId() {
        return 9999;
    }

    public void changeColor(int newColor) {
        currentColor = newColor;
    }

    public void changeDirection(int newDirection) {
        currentDirection = newDirection;
    }

    public final void paint(Canvas c, double scale, int dx, int dy) {
        drawImage(bubbles.get(currentColor), 302, 390, c, scale, dx, dy);

        c.save();
        int xCenter = 318;
        int yCenter = 406;
        c.rotate((float) (0.025 * 180 * (currentDirection - 20)),
                (float) (xCenter * scale + dx), (float) (yCenter * scale + dy));
        launcher.setBounds(
                (int) ((xCenter - 50) * scale + dx),
                (int) ((yCenter - 350) * scale + dy),
                (int) ((xCenter + 50) * scale + dx),
                (int) ((yCenter + 55) * scale + dy));
        launcher.draw(c);
        c.restore();
    }
}
