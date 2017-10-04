package com.mirallax.android.bubble;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.mirallax.android.R;

public class FrozenBubble extends Activity {


    private final static int MENU_NEW_GAME = 9;
    public final static String PREFS_NAME = "frozenbubble";

    private GameView.GameThread mGameThread;
    private GameView mGameView;

    private boolean activityCustomStarted = false;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_NEW_GAME, 0, R.string.menu_new_game);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_NEW_GAME:
                mGameThread.newGame();
                return true;
        }
        return false;
    }

    private void setFullscreen() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        mGameView.requestLayout();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent i = getIntent();
        if (null == i || null == i.getExtras() ||
                !i.getExtras().containsKey("levels")) {
            activityCustomStarted = false;
            setContentView(R.layout.main);
            mGameView = (GameView) findViewById(R.id.game);
        }

        mGameThread = mGameView.getThread();

        if (savedInstanceState != null) {
            mGameThread.restoreState(savedInstanceState);
        }
        mGameView.requestFocus();
        setFullscreen();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGameView.getThread().pause();
        Intent i = getIntent();
        if (null == i || !activityCustomStarted) {
            SharedPreferences sp = getSharedPreferences(PREFS_NAME,
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt("level", mGameThread.getCurrentLevelIndex());
            editor.apply();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGameView != null) {
            mGameView.cleanUp();
        }
        mGameView = null;
        mGameThread = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mGameThread.saveState(outState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (null != intent) {
            if (!activityCustomStarted) {
                activityCustomStarted = true;

                SharedPreferences sp = getSharedPreferences(
                        FrozenBubble.PREFS_NAME, Context.MODE_PRIVATE);
                int startingLevel = sp.getInt("levelCustom", 0);
                int startingLevelIntent = intent.getIntExtra("startingLevel", -2);
                startingLevel = (startingLevelIntent == -2) ?
                        startingLevel : startingLevelIntent;

                mGameView = null;
                mGameView = new GameView(
                        this, intent.getExtras().getByteArray("levels"),
                        startingLevel);
                setContentView(mGameView);
                mGameThread = mGameView.getThread();
                mGameThread.newGame();
                mGameView.requestFocus();
                setFullscreen();
            }
        }
    }

}
