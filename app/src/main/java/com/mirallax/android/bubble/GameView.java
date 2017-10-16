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

        private static final int FrameDelay = 30;

        public static final int StateRunning = 1;
        public static final int StatePause = 2;

        public static final int GamefieldWidth = 320;
        public static final int GamefieldHeight = 480;
        public static final int ExtendedGamefieldWidth = 640;


        private static final double TrackballCoefficient = 5;
        private static final double TouchCoefficient = 0.2;
        private static final double TouchFireYThreshold = 350;

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

        private final SurfaceHolder surfaceHolder;
        private boolean surfaceOK = false;

        private double displayScale;
        private int displayDX;
        private int displayDY;

        private FrozenGame frozenGame;

        private boolean imagesReady = false;

        private Bitmap backgroundOrig;
        private Bitmap[] bubblesOrig;
        private Bitmap hurryOrig;
        private Bitmap overOrig;
        private Bitmap winOrig;
        private Bitmap compressorHeadOrig;
        private BmpWrap background;
        private ArrayList<BmpWrap> bubbles;
        private BmpWrap hurry;
        private BmpWrap over;
        private BmpWrap win;
        private BmpWrap compressorHead;
        private Drawable launcher;
        private LevelManager levelManager;

        Vector<BmpWrap> imageList;

        int getCurrentLevelIndex() {
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

        private GameThread(SurfaceHolder surfaceHolder, byte[] customLevels) {
            this.surfaceHolder = surfaceHolder;
            Resources res = mContext.getResources();
            setState(StatePause);

            BitmapFactory.Options options = new BitmapFactory.Options();

            try {
                Field f = options.getClass().getField("inScaled");
                f.set(options, Boolean.FALSE);
            } catch (Exception ignore) {
            }

            backgroundOrig =
                    BitmapFactory.decodeResource(res, R.drawable.background, options);
            bubblesOrig = new Bitmap[8];
            for (int i = 0; i < 8; i++){
                String mDrawableName = "bubble_"+(i+1);
                int resID = getResources().getIdentifier(mDrawableName , "drawable", getContext().getPackageName());
                bubblesOrig[i] = BitmapFactory.decodeResource(res,resID,
                        options);
            }
            hurryOrig = BitmapFactory.decodeResource(res, R.drawable.hurry, options);
            overOrig = BitmapFactory.decodeResource(res, R.drawable.over, options);
            winOrig = BitmapFactory.decodeResource(res, R.drawable.win, options);
            compressorHeadOrig =
                    BitmapFactory.decodeResource(res, R.drawable.compressor, options);
            imageList = new Vector<>();

            background = NewBmpWrap();
            bubbles = new ArrayList<>(8);
            for (int i = 0; i < 8; i++) {
                bubbles.add(NewBmpWrap());
            }
            hurry = NewBmpWrap();
            over = NewBmpWrap();
            win = NewBmpWrap();
            compressorHead = NewBmpWrap();

            launcher = res.getDrawable(R.drawable.launcher);


            if (null == customLevels) {
                try {
                    InputStream is = mContext.getAssets().open("levels.txt");
                    int size = is.available();
                    byte[] levels = new byte[size];
                    is.read(levels);
                    is.close();
                    //Cambio de disposicion de la linea
                    SharedPreferences sp = mContext.getSharedPreferences(FrozenBubble.PREFS_NAME, Context.MODE_PRIVATE);
                    //Aumento del nivel base por el que empieza de 0 a 1.
                    int startingLevel = sp.getInt("level", 1);
                    levelManager = new LevelManager(levels, startingLevel);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }


            frozenGame = new FrozenGame(background, bubbles,
                    hurry, over, win,  compressorHead,

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
            scaleFrom(over, overOrig);
            scaleFrom(win, winOrig);
            scaleFrom(compressorHead, compressorHeadOrig);
            imagesReady = true;
        }

        void pause() {
            synchronized (surfaceHolder) {
                if (mode == StateRunning) {
                    setState(StatePause);
                }
            }
        }

        void newGame() {
            synchronized (surfaceHolder) {
                levelManager.goToFirstLevel();

                frozenGame = new FrozenGame(background, bubbles,
                        hurry, over, win, compressorHead,

                        launcher,
                        levelManager);
            }
        }

        @Override
        public void run() {
            while (run) {
                long now = System.currentTimeMillis();

                long delay = FrameDelay + lastTime - now;
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
                                    if (mode == StateRunning) {
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

        synchronized void restoreState(Bundle map) {
            synchronized (surfaceHolder) {
                setState(StatePause);
                frozenGame.restoreState(map, imageList);
                levelManager.restoreState(map);
            }
        }

        private void setRunning(boolean b) {
            run = b;
        }

        private void setState(int mode) {
            synchronized (surfaceHolder) {
                this.mode = mode;
            }
        }

        private void setSurfaceOK(boolean ok) {
            synchronized (surfaceHolder) {
                surfaceOK = ok;
            }
        }

        private boolean surfaceOK() {
            synchronized (surfaceHolder) {
                return surfaceOK;
            }
        }

        private void setSurfaceSize(int width, int height) {
            synchronized (surfaceHolder) {
                if (width / height >= GamefieldWidth / GamefieldHeight) {
                    displayScale = 1.0 * height / GamefieldHeight;
                    displayDX =
                            (int) ((width - displayScale * ExtendedGamefieldWidth) / 2);
                    displayDY = 0;
                } else {
                    displayScale = 1.0 * width / GamefieldWidth;
                    displayDX = (int) (-displayScale *
                            (ExtendedGamefieldWidth - GamefieldWidth) / 2);
                    displayDY = (int) ((height - displayScale * GamefieldHeight) / 2);
                }
                resizeBitmaps();
            }
        }

        boolean doKeyDown(int keyCode, KeyEvent msg) {
            synchronized (surfaceHolder) {
                stateRunning();

                if (mode == StateRunning) {
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
                if (mode == StateRunning) {
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
                stateRunning();

                if (mode == StateRunning) {
                    if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        trackballDX += event.getX() * TrackballCoefficient;
                        return true;
                    }
                }
                return false;
            }
        }

        private void stateRunning() {
            if (mode != StateRunning) {
                setState(StateRunning);
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
                stateRunning();

                double x = xFromScr(event.getX());
                double y = yFromScr(event.getY());
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (y < TouchFireYThreshold) {
                        touchFire = true;
                    }
                    touchLastX = x;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (y >= TouchFireYThreshold) {
                        touchDX = (x - touchLastX) * TouchCoefficient;
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

                frozenGame = new FrozenGame(background, bubbles,
                        hurry, over, win, compressorHead,

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

        private void cleanUp() {
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
                overOrig.recycle();
                overOrig = null;
                winOrig.recycle();
                winOrig = null;

                if (imagesScaled) {
                    background.bmp.recycle();
                    for (int i = 0; i < bubbles.size(); i++) {
                        bubbles.get(i).bmp.recycle();
                    }
                    hurry.bmp.recycle();
                    over.bmp.recycle();
                    compressorHead.bmp.recycle();
                }
                background.bmp = null;
                background = null;
                bubbles = null;
                hurry.bmp = null;
                hurry = null;
                over.bmp = null;
                over = null;
                win.bmp = null;
                win = null;
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

        thread = new GameThread(holder, null);
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

        thread = new GameThread(holder, levels);
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
