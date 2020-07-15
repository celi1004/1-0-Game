package GameIOIO.game;

import android.content.Context;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Game {
    private GameGrid mGameGrid = null;
    static final int DEFAULT_HEIGHT_X = 4;
    static final int DEFAULT_WIDTH_Y = 4;
    static final int DEFAULT_TILE_TYPES = 24;
    private static final int DEFAULT_STARTING_TILES = (int)(Math.random()*2)+8;
    private int mPositionX = DEFAULT_HEIGHT_X;
    private int mPositionY = DEFAULT_WIDTH_Y;
    private int mTileType = DEFAULT_TILE_TYPES;
    private int mStartingTiles = DEFAULT_STARTING_TILES;
    private boolean mCanUndo;
    private State mLastGameState;
    private State mBufferGameState;
    private State mGameState = State.NORMAL;
    private Context mContext;
    private GameView mView;
    private ScoreListener mScoreListener;
    private long mScore = 0;
    private long mLastScore = 0;
    private long mBufferScore = 0;
    private GameStateListener mGamesStateListener;

    public enum State{
        NORMAL, WON, LOSE
    }

    public Game(Context context){
        mContext = context;
    }

    public interface ScoreListener{
        public void onNewScore(long score);
    }

    public interface GameStateListener{
        public void onGameStateChanged(State state);
    }

    public void setGamesStateListener(GameStateListener listener){
        this.mGamesStateListener = listener;
    }

    public GameGrid getmGameGrid(){
        return mGameGrid;
    }

    public boolean isCanUndo(){
        return mCanUndo;
    }

    public void setCanUndo(boolean canUndo){
        mCanUndo = canUndo;
    }

    public  State getLastGameState(){
        return mLastGameState;
    }

    public void setLastGameState(State lastGameState){
        mLastGameState = lastGameState;
    }

    public State getGameState(){
        return mGameState;
    }

    public long getScore(){
        return mScore;
    }

    public void setScore(long score){
        mScore = score;
    }

    public long getLastScore(){
        return mLastScore;
    }

    public void setLastScore(long lastScore){
        mLastScore = lastScore;
    }

    public void setScoreListener(ScoreListener listener){
        mScoreListener = listener;
    }

    public boolean isGameOnGoing(){
        return mGameState != State.WON && mGameState != State.LOSE;
    }

    public void setup(GameView view){
        mView = view;
    }

    private void updateScore(long score){
        mScore = score;
        if(mScoreListener != null){
            mScoreListener.onNewScore(mScore);
        }
    }

    public void newGame(){
        if(mGameGrid == null){
            mGameGrid = new GameGrid(mPositionX);
        }
        else{
            prepareUndoState();
            saveUndoState();
            mGameGrid.clearGrid();;
        }

        updateScore(0);
        updateGameState(State.NORMAL);
        mView.setGameState(mGameState);
        addStartTiles();
        mView.setRefreshLastTime(true);
        mView.resyncTime();
        mView.invalidate();
    }

    private void addStartTiles(){
        for(int i=0; i<mStartingTiles; i++){
            addRandomTiles();
        }
    }

    //빈타일 공간있으면 랜덤 타일 하나 value 0 or 1로 해서 생성
    private void addRandomTiles(){
        if(mGameGrid.isTilesAvail()){
            int value = Math.random() < 0.79 ? 1:0;
            Tile tile = new Tile(mGameGrid.randomAvailTile(), value);
            spawnTile(tile);
        }
    }

    //mGameGrid에 tile넣고 animation도 등록
    private void spawnTile(Tile tile){
        mGameGrid.insertTile(tile);
        mView.spawnTile(tile);
    }

    //tile 위치에 뭐 있으면 setMergeFrom null로
    private void prepareTiles(){
        for(Tile[] array:mGameGrid.getGrid()){
            for(Tile tile: array){
                if(mGameGrid.isTilesOccupied(tile)){
                    tile.setMergedFrom(null);
                }
            }
        }
    }

    //tile을 cell 위치로 옮기고 tile의 위치 없데이트
    private void moveTile(Tile tile, Position cell){
        mGameGrid.getGrid()[tile.getX()][tile.getY()] = null;
        mGameGrid.getGrid()[cell.getX()][cell.getY()] = tile;
        tile.updatePosition(cell);
    }

    //BufferGrid내용 UndoGrid에 넣고, Undo 가능 상태로, Score에도 저장
    private void saveUndoState(){
        mGameGrid.saveTile();
        mCanUndo = true;
        mLastScore = mBufferScore;
        mLastGameState = mBufferGameState;
    }

    //Grid 내용 BufferGrid에 넣고 스코어도
    private void prepareUndoState(){
        mGameGrid.prepareSaveTiles();
        mBufferScore = mScore;
        mBufferGameState = mGameState;
    }

    //Undo 가능 상태면 불가능으로 만들고 뒤로 돌림
    public void revertUndoState(){
        if(mCanUndo){
            mCanUndo = false;
            mView.cancelAnimations();
            mGameGrid.revertTiles();
            updateScore(mLastScore);
            updateGameState(mLastGameState);
            mView.setGameState(mGameState);
            mView.setRefreshLastTime(true);
            mView.invalidate();
        }
    }

    public void updateUI(){
        updateScore(mScore);
        mView.setGameState(mGameState);
        mView.setRefreshLastTime(true);
        mView.invalidate();
    }

    public static final int UP = 0;
    public static final int RIGHT = 1;
    public static final int DOWN = 2;
    public static final int LEFT = 3;

    public void move(int direction){
        mView.cancelAnimations();
        if(!isGameOnGoing()){
            return;
        }

        prepareUndoState();
        Position vector = Position.getVector(direction);
        List<Integer> traversalsX = buildTraversalsX(vector);
        List<Integer> traversalsY = buildTraverslasY(vector);
        boolean moved = false;

        prepareTiles();

        for(int x: traversalsX){
            for(int y: traversalsY) {
                Position cell = new Position(x, y);
                Tile tile = mGameGrid.getTile(cell);

                if(tile != null){
                    Position[] positions = findFarthestPostion(cell, vector);
                    Tile next = mGameGrid.getTile(positions[1]);

                    if(next != null){
                        Tile merged = new Tile(positions[1], tile.getValue() * next.getValue());
                        Tile[] temp = {tile, next};
                        merged.setMergedFrom(temp);

                        mGameGrid.insertTile(merged);
                        mGameGrid.removeTile(tile);

                        tile.updatePosition(positions[1]);

                        int[] extras = {x,y};

                        mView.moveTile(merged.getX(), merged.getY(), extras);
                        mView.mergedTile(merged.getX(), merged.getY());

                        updateScore(mScore + (merged.getValue()*2));
                    }
                    else{
                        moveTile(tile, positions[0]);
                        int[] extras = {x, y , 0};
                        mView.moveTile(positions[0].getX(), positions[0].getY(), extras);
                    }

                    if(!Position.eqaul(cell,tile)){
                        moved = true;
                    }
                }
            }
        }
        mView.updateGrid(mGameGrid);
        if(moved){
            saveUndoState();
            int addTilenum = (int)(Math.random()*2)+1;
            for(int i=0; i<addTilenum; i++) {
                addRandomTiles();
            }
            checkLose();
        }
        mView.resyncTime();
        mView.invalidate();
    }

    private Position[] findFarthestPostion(Position cell, Position vector){
        Position previous;
        Position nextCell = new Position(cell.getX(), cell.getY());

        do{
            previous = nextCell;
            nextCell = new Position(previous.getX()+vector.getX(), previous.getY()+vector.getY());
        }while(mGameGrid.isTileWithinBound(nextCell) && mGameGrid.isTileAvail(nextCell));

        Position[] answer = {previous, nextCell};
        return answer;
    }

    public void updateGameState(State state){
        mGameState = state;
        if(mGamesStateListener != null){
            mGamesStateListener.onGameStateChanged(mGameState);
        }
    }

    private void checkLose(){
        if(!isMovePossible()){
            updateGameState(State.LOSE);
            mView.setGameState(mGameState);
            endGame();
            return;
        }

        boolean isFinished = true;
        int[] andValuex = new int[4];
        int[] andValuey = new int[4];
        for(int x=0; x<mPositionX; x++){
            for(int y=0; y<mPositionY; y++){
                Tile tile = mGameGrid.getTile(new Position(x,y));
                Tile tile2 = mGameGrid.getTile(new Position(y,x));
                if(tile != null){
                    andValuex[x] = andValuex[x] + tile.getValue();
                }
                if(tile2 != null){
                    andValuey[x] = andValuey[x] + tile2.getValue();
                }
            }
        }

        for(int i=0; i<4; i++){
            if(andValuex[i] != 0 || andValuey[i] != 0){
                isFinished = false;
                break;
            }
        }

        if(isFinished == true){
            updateGameState(State.LOSE);
            mView.setGameState(mGameState);
            endGame();
            return;
        }
    }

    private void endGame(){
        mView.endGame();
        updateScore(mScore);
    }

    private List<Integer> buildTraversalsX(Position vector){
        List<Integer> traversals = new ArrayList<Integer>();
        for(int x=0; x<mPositionX; x++){
            traversals.add(x);
        }
        if(vector.getX() == 1){
            Collections.reverse(traversals);
        }

        return traversals;
    }

    private List<Integer> buildTraverslasY(Position vector){
        List<Integer> traversals = new ArrayList<Integer>();
        for(int x=0; x<mPositionY; x++){
            traversals.add(x);
        }
        if(vector.getY() == 1){
            Collections.reverse(traversals);
        }

        return traversals;
    }

    private boolean isMovePossible(){
        return mGameGrid.isTilesAvail() || tileMatchesAvail();
    }

    private boolean tileMatchesAvail(){
        Tile tile;
        for(int x= 0; x<mPositionX; x++){
            for(int y=0; y<mPositionY; y++){
                tile = mGameGrid.getTile(new Position(x, y));
                if(tile != null){
                    for(int direction=0; direction<4; direction++){
                        Position vector = Position.getVector(direction);
                        Position cell = new Position(x+vector.getX(), y+vector.getY());
                        Tile other = mGameGrid.getTile(cell);
                        if(other != null && other.getValue() == tile.getValue()){
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
