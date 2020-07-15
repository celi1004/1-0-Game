package GameIOIO.game;

import java.util.ArrayList;

public class GameGrid {

    private final Tile[][] mGrid;
    private final Tile[][] mUndoGrid;
    private final Tile[][] mBufferGrid;

    //size만큼 mGrid, mUndoGrid, mBufferGrid 공간 할당하고 내용 지움
    public GameGrid(int size){
        mGrid = new Tile[size][size];
        mUndoGrid = new Tile[size][size];
        mBufferGrid = new Tile[size][size];
        clearGrid();
        clearUndoGrid();
    }

    //availTile 중에 하나 반환
    public Position randomAvailTile(){
        ArrayList<Position> availTile = getAvailTiles();
        if(availTile.size() >= 1){
            return availTile.get((int) Math.floor(Math.random() * availTile.size()));
        }
        return null;
    }

    //mGrid에서 빈칸 Position 반환
    ArrayList<Position> getAvailTiles(){
        ArrayList<Position> availTile = new ArrayList<Position>();
        for( int i = 0; i<mGrid.length; i++){
            for(int j=0; j<mGrid[0].length; j++){
                if(mGrid[i][j] == null){
                    availTile.add(new Position(i, j));
                }
            }
        }
        return availTile;
    }

    //빈 타일이 있는 지 반환 있다면 true
    public boolean isTilesAvail(){
        return (getAvailTiles().size() >= 1);
    }

    //Position에 뭐 없는지 반환
    public boolean isTileAvail(Position tile){
        return !isTilesOccupied(tile);
    }

    //Position에 뭐 있는지 반환
    public boolean isTilesOccupied(Position tile){
        return (getTile(tile) != null);
    }

    //Position의 타일 반환
    public Tile getTile(Position tile){
        if(tile != null && isTileWithinBound(tile)){
            return mGrid[tile.getX()][tile.getY()];
        }
        else{
            return null;
        }
    }

    //(x,y)위의 타일 반환
    public Tile getTileContent(int x, int y){
        if(isTileWithinBound(x,y)){
            return mGrid[x][y];
        }
        else{
            return null;
        }
    }

    //Position이 경계 안에 있는 지
    public boolean isTileWithinBound(Position tile){
        return 0<=tile.getX() && tile.getX() < mGrid.length && 0<=tile.getY() && tile.getY() < mGrid[0].length;
    }

    //(x,y)가 경계 안에 있는 지
    public boolean isTileWithinBound(int x, int y){
        return 0<=x && x<mGrid.length && 0<=y && y<mGrid[0].length;
    }

    //Tile을 mGrid에 넣음
    public void insertTile(Tile tile){
        mGrid[tile.getX()][tile.getY()] = tile;
    }

    //Tile을 mGrid에서 지움
    public void removeTile(Tile tile){
        mGrid[tile.getX()][tile.getY()] = null;
    }

    //BufferGrid의 내용을 UndoGrid에 넣음
    public void saveTile(){
        for(int i = 0; i<mBufferGrid.length; i++){
            for(int j=0; j<mBufferGrid[0].length; j++){
                if(mBufferGrid[i][j] == null){
                    mUndoGrid[i][j] = null;
                }
                else{
                    mUndoGrid[i][j] = new Tile(i, j, mBufferGrid[i][j].getValue());
                }
            }
        }
    }

    //mGrid의 내용을 BufferGrid에 저장
    public void prepareSaveTiles(){
        for(int i = 0; i<mGrid.length; i++){
            for(int j=0; j<mGrid[0].length; j++){
                if(mGrid[i][j] == null){
                    mBufferGrid[i][j] = null;
                }
                else{
                    mBufferGrid[i][j] = new Tile(i, j, mGrid[i][j].getValue());
                }
            }
        }
    }

    //UndoGrid의 내용을 Grid에 저장( 뒤로가기 )
    public void revertTiles(){
        for(int i = 0; i<mUndoGrid.length; i++){
            for(int j=0; j<mUndoGrid[0].length; j++){
                if(mUndoGrid[i][j] == null){
                    mGrid[i][j] = null;
                }
                else{
                    mGrid[i][j] = new Tile(i, j, mUndoGrid[i][j].getValue());
                }
            }
        }
    }

    public void clearGrid(){
        for(int i = 0; i<mGrid.length; i++){
            for(int j=0; j<mGrid[0].length; j++){
                mGrid[i][j] = null;
            }
        }
    }

    public void clearUndoGrid(){
        for(int i = 0; i<mUndoGrid.length; i++){
            for(int j=0; j<mUndoGrid[0].length; j++){
                mUndoGrid[i][j] = null;
            }
        }
    }

    public Tile[][] getGrid(){
        return mGrid;
    }

    public Tile[][] getUndoGrid(){
        return mUndoGrid;
    }
}
