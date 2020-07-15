package com.example.ioiogame;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;
import android.util.Log;

import androidx.fragment.app.Fragment;

import GameIOIO.game.*;
import GameIOIO.tools.*;

public class MainActivity extends Activity implements Game.GameStateListener {

    private static final String TAG = "GAME IOIO:GameFragment";
    public static boolean sReversed = false;

    private GameView mGameView;
    private Game mGame;

    private static final String SCORE = "savegame.score";
    private static final String UNDO_SCORE = "savegame.undoscore";
    private static final String CAN_UNDO = "savegame.canundo";
    private static final String UNDO_GRID = "savegame.undo";
    private static final String GAME_STATE = "savegame.gamestate";
    private static final String UNDO_GAME_STATE = "savegame.undogamestate";

    private TextView mScoreText;
    private TextView mHighScoreText;
    private TextView mOverlay;
    private ScoreKeeper mScoreKeeper;
    private ImageButton mRestartBtn, mUndoBtn;


    @Override
    public void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mGameView = (GameView) findViewById(R.id.gameview);
        mScoreText = (TextView) findViewById(R.id.score);
        mHighScoreText = (TextView) findViewById(R.id.bestScore);
        mRestartBtn = (ImageButton) findViewById(R.id.restart);
        mUndoBtn = (ImageButton) findViewById(R.id.goback);
        mOverlay = (TextView) findViewById(R.id.endgame_overlay);

        mScoreKeeper = new ScoreKeeper(getApplicationContext());
        mScoreKeeper.setViews(mScoreText, mHighScoreText);
        mGame = new Game(getApplicationContext());
        mGame.setup(mGameView);
        mGame.setScoreListener(mScoreKeeper);
        mGame.setGamesStateListener(this);
        mGame.newGame();
        InputListener input = new InputListener();
        input.setView(mGameView);
        input.setGame(mGame);

        mRestartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGame.newGame();
            }
        });

        mRestartBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(getApplicationContext(), getString(R.string.new_game), Toast.LENGTH_LONG).show();
                return true;
            }
        });

        mUndoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGame.revertUndoState();
            }
        });

        mUndoBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(getApplicationContext(), getString(R.string.undo_last_move), Toast.LENGTH_LONG).show();
                return true;
            }
        });
    }

    @Override
    public void onResume(){
        load();
        super.onResume();
    }

    @Override
    public void onPause(){
        if(mGame != null)
            save();
        super.onPause();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        Log.d(TAG, "키 이벤트 발생"+keyCode);
        if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN){
            mGame.move(Game.DOWN);
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_DPAD_UP){
            mGame.move(Game.UP);
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT){
            mGame.move(Game.LEFT);
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
            mGame.move(Game.RIGHT);
            return true;
        }
        return true;
    }

    private void save(){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = settings.edit();
        Tile[][] field = mGame.getmGameGrid().getGrid();
        Tile[][] undoField = mGame.getmGameGrid().getUndoGrid();

        for(int x=0; x<field.length; x++){
            for(int y=0; y<field[0].length; y++){
                if(field[x][y] != null){
                    editor.putInt(x+" "+y, field[x][y].getValue());
                }
                else{
                    editor.putInt(x+" "+y, 0);
                }

                if(undoField[x][y] != null){
                    editor.putInt(UNDO_GRID+x+" "+y, undoField[x][y].getValue());
                }
                else{
                    editor.putInt(UNDO_GRID+x+" "+y, 0);
                }
            }
        }
        editor.putLong(SCORE, mGame.getScore());
        editor.putLong(UNDO_SCORE, mGame.getLastScore());
        editor.putBoolean(CAN_UNDO, mGame.isCanUndo());
        editor.putString(GAME_STATE, mGame.getGameState().name());
        editor.putString(UNDO_GAME_STATE, mGame.getLastGameState().name());
        editor.commit();
    }

    private void load(){
        mGameView.cancelAnimations();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        for(int x=0; x<mGame.getmGameGrid().getGrid().length; x++){
            for(int y=0; y<mGame.getmGameGrid().getGrid()[0].length; y++){
                int value = settings.getInt(x+" "+y, -1);
                if(value>0){
                    mGame.getmGameGrid().getGrid()[x][y] = new Tile(x,y,value);
                }
                else if(value == 0){
                    mGame.getmGameGrid().getGrid()[x][y] = null;
                }

                int undoValue = settings.getInt(UNDO_GRID+x+" "+y, -1);
                if(undoValue >0){
                    mGame.getmGameGrid().getUndoGrid()[x][y] = new Tile(x,y,undoValue);
                }
                else if(value == 0){
                    mGame.getmGameGrid().getUndoGrid()[x][y] = null;
                }
            }
        }
        mGame.setScore(settings.getLong(SCORE,0));
        mGame.setLastScore(settings.getLong(UNDO_SCORE, 0));
        mGame.setCanUndo(settings.getBoolean(CAN_UNDO, mGame.isCanUndo()));

        try{
            mGame.updateGameState(Game.State.valueOf(settings.getString(GAME_STATE, Game.State.NORMAL.name())));
        }catch (IllegalArgumentException e){
            mGame.updateGameState(Game.State.NORMAL);
        }

        try {
            mGame.setLastGameState(Game.State.valueOf(settings.getString(UNDO_GAME_STATE, Game.State.NORMAL.name())));
        }catch (IllegalArgumentException e){
            mGame.setLastGameState(Game.State.NORMAL);
        }
        mGame.updateUI();;
    }

    @Override
    public void onGameStateChanged(Game.State state){
        if(state == Game.State.LOSE){
            mOverlay.setVisibility(View.VISIBLE);
            mOverlay.setText(R.string.game_over);
        }
        else{
            mOverlay.setVisibility(View.GONE);
        }
    }
}
