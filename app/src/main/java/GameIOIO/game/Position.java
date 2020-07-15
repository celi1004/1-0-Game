package GameIOIO.game;

public class Position {

    private int x;
    private int y;

    public Position(){

    }

    public Position(int x, int y){
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public static boolean eqaul(Position first, Position second){
        return first.getX() == second.getX() && first.getY() == second.getY();
    }

    public static Position getVector(int direction){
        Position[] map = {
                new Position(0,-1),
                new Position(1, 0),
                new Position(0,1),
                new Position(-1, 0)
        };
        return map[direction];
    }
}
