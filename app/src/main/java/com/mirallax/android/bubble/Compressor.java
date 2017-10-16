package com.mirallax.android.bubble;

import android.graphics.Canvas;
import android.os.Bundle;

import com.mirallax.android.bubble.sprite.BmpWrap;

public class Compressor {
    private final BmpWrap compressorHead;
    private int steps;

    public Compressor(BmpWrap compressorHead) {
        this.compressorHead = compressorHead;
        this.steps = 0;
    }

    public void saveState(Bundle map) {
        map.putInt("compressor-steps", steps);
    }

    public void restoreState(Bundle map) {
        steps = map.getInt("compressor-steps");
    }

    public void moveDown() {
        steps++;
    }

    public void paint(Canvas c, double scale, int dx, int dy) {
        c.drawBitmap(compressorHead.bmp,
                (float) (160 * scale + dx),
                (float) ((-7 + 28 * steps) * scale + dy),
                null);
    }
}
