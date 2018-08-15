package daslab.com.fieldmonitoringv2;

public class PixelCoordinate {
    int x, y;
    public PixelCoordinate(int x, int y){
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        String string = x + "," + y;
        return string;
    }

    @Override
    public int hashCode() {
        int hash = (this.x * 2) + (this.y * 3);
        return hash;
    }

    @Override
    public boolean equals( Object obj ) {
        if(obj == null){
            return false;
        }
        if (!(obj instanceof PixelCoordinate)){
            return false;
        }
        if (obj == this){
            return true;
        }
        return ((this.x == ((PixelCoordinate) obj).x) && (this.y == ((PixelCoordinate) obj).y));
    }
}
