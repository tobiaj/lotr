package se.kth.id2203.supervisor;

/**
 * Created by tobiaj on 2017-02-22.
 */
public class Range {

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    private int min;
    private int max;

    public Range(int min, int max){

        this.min = min;
        this.max = max;
    }
}
