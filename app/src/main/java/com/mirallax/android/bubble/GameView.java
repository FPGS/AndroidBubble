package com.mirallax.android.bubble;

import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import android.content.Context;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.MotionEvent;

import com.mirallax.android.R;
import com.mirallax.android.bubble.manager.LevelManager;
import com.mirallax.android.bubble.sprite.BmpWrap;
import com.mirallax.android.bubble.sprite.Sprite;

import java.util.ArrayList;
import java.util.Vector;

class GameView extends SurfaceView implements SurfaceHolder.Callback {


    class GameThread extends Thread {
        private static final int FRAME_DELAY = 40;

        public static final int STATE_RUNNING = 1;
        public static final int STATE_PAUSE = 2;

        public static final int GAMEFIELD_WIDTH = 320;
        public static final int GAMEFIELD_HEIGHT = 480;
        public static final int EXTENDED_GAMEFIELD_WIDTH = 640;

        private static final double TRACKBALL_COEFFICIENT = 5;
        private static final double TOUCH_COEFFICIENT = 0.2;
        private static final double TOUCH_FIRE_Y_THRESHOLD = 350;

        private long lastTime;
        private int mode;
        private boolean run = false;

        private boolean left = false;
        private boolean right = false;
        private boolean up = false;
        private boolean fire = false;
        private boolean wasLeft = false;
        private boolean wasRight = false;
        private boolean wasFire = false;
        private boolean wasUp = false;
        private double trackballDX = 0;
        private double touchDX = 0;
        private double touchLastX;
        private boolean touchFire = false;

        private SurfaceHolder surfaceHolder;
        private boolean surfaceOK = false;

        private double displayScale;
        private int displayDX;
        private int displayDY;

        private FrozenGame frozenGame;

        private boolean imagesReady = false;

        private Bitmap backgroundOrig;
        private Bitmap[] bubblesOrig;
        private Bitmap hurryOrig;
        private Bitmap compressorHeadOrig;
        private BmpWrap background;
        private ArrayList<BmpWrap> bubbles;
        private BmpWrap hurry;
        private BmpWrap compressorHead;
        private Drawable launcher;
        private LevelManager levelManager;

        Vector imageList;

        public int getCurrentLevelIndex() {
            synchronized (surfaceHolder) {
                return levelManager.getLevelIndex();
            }
        }

        private BmpWrap NewBmpWrap() {
            int new_img_id = imageList.size();
            BmpWrap new_img = new BmpWrap(new_img_id);
            imageList.addElement(new_img);
            return new_img;
        }

        public GameThread(SurfaceHolder surfaceHolder, byte[] customLevels,
                          int startingLevel) {
            this.surfaceHolder = surfaceHolder;
            Resources res = mContext.getResources();
            setState(STATE_PAUSE);

            BitmapFactory.Options options = new BitmapFactory.Options();

            try {
                Field f = options.getClass().getField("inScaled");
                f.set(options, Boolean.FALSE);
            } catch (Exception ignore) {
            }

            backgroundOrig =
                    BitmapFactory.decodeResource(res, R.drawable.background, options);
            bubblesOrig = new Bitmap[8];
            bubblesOrig[0] = BitmapFactory.decodeResource(res, R.drawable.bubble_1,
                    options);
            bubblesOrig[1] = BitmapFactory.decodeResource(res, R.drawable.bubble_2,
                    options);
            bubblesOrig[2] = BitmapFactory.decodeResource(res, R.drawable.bubble_3,
                    options);
            bubblesOrig[3] = BitmapFactory.decodeResource(res, R.drawable.bubble_4,
                    options);
            bubblesOrig[4] = BitmapFactory.decodeResource(res, R.drawable.bubble_5,
                    options);
            bubblesOrig[5] = BitmapFactory.decodeResource(res, R.drawable.bubble_6,
                    options);
            bubblesOrig[6] = BitmapFactory.decodeResource(res, R.drawable.bubble_7,
                    options);
            bubblesOrig[7] = BitmapFactory.decodeResource(res, R.drawable.bubble_8,
                    options);
            hurryOrig = BitmapFactory.decodeResource(res, R.drawable.hurry, options);
            compressorHeadOrig =
                    BitmapFactory.decodeResource(res, R.drawable.compressor, options);
            imageList = new Vector();

            background = NewBmpWrap();
            bubbles = new ArrayList<>(8);
            for (int i = 0; i < 8; i++) {
                bubbles.add(NewBmpWrap());
            }
            hurry = NewBmpWrap();
            compressorHead = NewBmpWrap();

            launcher = res.getDrawable(R.drawable.launcher);


            if (null == customLevels) {
                try {
                    InputStream is = mContext.getAssets().open("levels.txt");
                    int size = is.available();
                    byte[] levels = new byte[size];
                    is.read(levels);
                    is.close();
                    SharedPreferences sp = mContext.getSharedPreferences(
                            FrozenBubble.PREFS_NAME, Context.MODE_PRIVATE);
                    startingLevel = sp.getInt("level", 0);
                    levelManager = new LevelManager(levels, startingLevel);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            frozenGame = new FrozenGame(bubbles,
                    hurry, compressorHead,
                    launcher,
                    levelManager);
        }

        private void scaleFrom(BmpWrap image, Bitmap bmp) {
            if (image.bmp != null && image.bmp != bmp) {
                image.bmp.recycle();
            }

            if (displayScale > 0.99999 && displayScale < 1.00001) {
                image.bmp = bmp;
                return;
            }
            int dstWidth = (int) (bmp.getWidth() * displayScale);
            int dstHeight = (int) (bmp.getHeight() * displayScale);
            image.bmp = Bitmap.createScaledBitmap(bmp, dstWidth, dstHeight, true);
        }

        private void resizeBitmaps() {
            scaleFrom(background, backgroundOrig);
            for (int i = 0; i < bubblesOrig.length; i++) {
                scaleFrom(bubbles.get(i), bubblesOrig[i]);
            }
            scaleFrom(hurry, hurryOrig);
            scaleFrom(compressorHead, compressorHeadOrig);
            imagesReady = true;
        }

        public void pause() {
            synchronized (surfaceHolder) {
                if (mode == STATE_RUNNING) {
                    setState(STATE_PAUSE);
                }
            }
        }

        public void newGame() {
            synchronized (surfaceHolder) {
                levelManager.goToFirstLevel();
                frozenGame = new FrozenGame(bubbles,
                        hurry, compressorHead,
                        launcher,
                        levelManager);
            }
        }

        @Override
        public void run() {
            while (run) {
                long now = System.currentTimeMillis();
                long delay = FRAME_DELAY + lastTime - now;
                if (delay > 0) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException e) {
                    }
                }
                lastTime = now;
                Canvas c = null;
                try {
                    if (surfaceOK()) {
                        c = surfaceHolder.lockCanvas(null);
                        if (c != null) {
                            synchronized (surfaceHolder) {
                                if (run) {
                                    if (mode == STATE_RUNNING) {
                                        updateGameState();
                                    }
                                    doDraw(c);
                                }
                            }
                        }
                    }
                } finally {
                    if (c != null) {
                        surfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }

        public Bundle saveState(Bundle map) {
            synchronized (surfaceHolder) {
                if (map != null) {
                    frozenGame.saveState(map);
                    levelManager.saveState(map);
                }
            }
            return map;
        }

        public synchronized void restoreState(Bundle map) {
            synchronized (surfaceHolder) {
                setState(STATE_PAUSE);
                frozenGame.restoreState(map, imageList);
                levelManager.restoreState(map);
            }
        }

        public void setRunning(boolean b) {
            run = b;
        }

        public void setState(int mode) {
            synchronized (surfaceHolder) {
                this.mode = mode;
            }
        }

        public void setSurfaceOK(boolean ok) {
            synchronized (surfaceHolder) {
                surfaceOK = ok;
            }
        }

        public boolean surfaceOK() {
            synchronized (surfaceHolder) {
                return surfaceOK;
            }
        }

        public void setSurfaceSize(int width, int height) {
            synchronized (surfaceHolder) {
                if (width / height >= GAMEFIELD_WIDTH / GAMEFIELD_HEIGHT) {
                    displayScale = 1.0 * height / GAMEFIELD_HEIGHT;
                    displayDX =
                            (int) ((width - displayScale * EXTENDED_GAMEFIELD_WIDTH) / 2);
                    displayDY = 0;
                } else {
                    displayScale = 1.0 * width / GAMEFIELD_WIDTH;
                    displayDX = (int) (-displayScale *
                            (EXTENDED_GAMEFIELD_WIDTH - GAMEFIELD_WIDTH) / 2);
                    displayDY = (int) ((height - displayScale * GAMEFIELD_HEIGHT) / 2);
                }
                resizeBitmaps();
            }
        }

        boolean doKeyDown(int keyCode, KeyEvent msg) {
            synchronized (surfaceHolder) {
                if (mode != STATE_RUNNING) {
                    setState(STATE_RUNNING);
                }

                if (mode == STATE_RUNNING) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        left = true;
                        wasLeft = true;
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        right = true;
                        wasRight = true;
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                        fire = true;
                        wasFire = true;
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        up = true;
                        wasUp = true;
                        return true;
                    }
                }

                return false;
            }
        }

        boolean doKeyUp(int keyCode, KeyEvent msg) {
            synchronized (surfaceHolder) {
                if (mode == STATE_RUNNING) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        left = false;
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        right = false;
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                        fire = false;
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        up = false;
                        return true;
                    }
                }
                return false;
            }
        }

        boolean doTrackballEvent(MotionEvent event) {
            synchronized (surfaceHolder) {
                if (mode != STATE_RUNNING) {
                    setState(STATE_RUNNING);
                }

                if (mode == STATE_RUNNING) {
                    if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        trackballDX += event.getX() * TRACKBALL_COEFFICIENT;
                        return true;
                    }
                }
                return false;
            }
        }

        private double xFromScr(float x) {
            return (x - displayDX) / displayScale;
        }

        private double yFromScr(float y) {
            return (y - displayDY) / displayScale;
        }

        boolean doTouchEvent(MotionEvent event) {
            synchronized (surfaceHolder) {
                if (mode != STATE_RUNNING) {
                    setState(STATE_RUNNING);
                }

                double x = xFromScr(event.getX());
                double y = yFromScr(event.getY());
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (y < TOUCH_FIRE_Y_THRESHOLD) {
                        touchFire = true;
                    }
                    touchLastX = x;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (y >= TOUCH_FIRE_Y_THRESHOLD) {
                        touchDX = (x - touchLastX) * TOUCH_COEFFICIENT;
                    }
                    touchLastX = x;
                }
                return true;
            }
        }

        private void drawBackground(Canvas c) {
            Sprite.drawImage(background, 0, 0, c, displayScale,
                    displayDX, displayDY);
        }


        private void doDraw(Canvas canvas) {
            if (!imagesReady) {
                return;
            }
            if (displayDX > 0 || displayDY > 0) {
                canvas.drawRGB(0, 0, 0);
            }
            drawBackground(canvas);
            frozenGame.paint(canvas, displayScale, displayDX, displayDY);
        }

        private void updateGameState() {
            if (frozenGame.play(left || wasLeft, right || wasRight,
                    fire || up || wasFire || wasUp || touchFire,
                    trackballDX, touchDX)) {
                frozenGame = new FrozenGame(bubbles,
                        hurry, compressorHead,
                        launcher,
                        levelManager);
            }
            wasLeft = false;
            wasRight = false;
            wasFire = false;
            wasUp = false;
            trackballDX = 0;
            touchFire = false;
            touchDX = 0;
        }

        public void cleanUp() {
            synchronized (surfaceHolder) {
                imagesReady = false;
                boolean imagesScaled = (backgroundOrig == background.bmp);
                backgroundOrig.recycle();
                backgroundOrig = null;
                for (int i = 0; i < bubblesOrig.length; i++) {
                    bubblesOrig[i].recycle();
                    bubblesOrig[i] = null;
                }
                bubblesOrig = null;
                hurryOrig.recycle();
                hurryOrig = null;

                if (imagesScaled) {
                    background.bmp.recycle();
                    for (int i = 0; i < bubbles.size(); i++) {
                        bubbles.get(i).bmp.recycle();
                    }
                    hurry.bmp.recycle();
                    compressorHead.bmp.recycle();
                }
                background.bmp = null;
                background = null;
                bubbles = null;
                hurry.bmp = null;
                hurry = null;
                compressorHead.bmp = null;
                compressorHead = null;

                imageList = null;
                levelManager = null;
                frozenGame = null;
            }
        }
    }

    private Context mContext;
    private GameThread thread;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        thread = new GameThread(holder, null, 0);
        setFocusable(true);
        setFocusableInTouchMode(true);

        thread.setRunning(true);
        thread.start();
    }

    public GameView(Context context, byte[] levels, int startingLevel) {
        super(context);

        mContext = context;
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        thread = new GameThread(holder, levels, startingLevel);
        setFocusable(true);
        setFocusableInTouchMode(true);

        thread.setRunning(true);
        thread.start();
    }

    public GameThread getThread() {
        return thread;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
        return thread.doKeyDown(keyCode, msg);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent msg) {
        return thread.doKeyUp(keyCode, msg);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        return thread.doTrackballEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return thread.doTouchEvent(event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) {
            thread.pause();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        thread.setSurfaceSize(width, height);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        thread.setSurfaceOK(true);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        thread.setSurfaceOK(false);
    }

    public void cleanUp() {
        thread.cleanUp();
        mContext = null;
    }
}
