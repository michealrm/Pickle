package pickle;

import java.util.ArrayList;

public class PickleArray {

    public ArrayList<ResultValue> arrayList;

    public int length; //0 = unbounded, > 0 = array length
    private ResultValue defaultValue;
    public SubClassif type;
    public int iElem = 0; // the element in an array

    public PickleArray(SubClassif type, int length) throws Exception {
        arrayList = new ArrayList<>();
        this.length = length;

        this.type = type;
        if(type == SubClassif.INTEGER)
            defaultValue = new ResultValue(SubClassif.INTEGER, new Numeric("0", SubClassif.INTEGER));
        if(type == SubClassif.FLOAT)
            defaultValue = new ResultValue(SubClassif.FLOAT, new Numeric("0", SubClassif.FLOAT));
        if(type == SubClassif.STRING)
            defaultValue = new ResultValue(SubClassif.STRING, "");

        // Fill array
        for(int i = 0; i < length; i++)
            arrayList.add(defaultValue);

    }

    public ResultValue getMaxElem() throws Exception {
        return new ResultValue(SubClassif.INTEGER
            , new Numeric(Integer.toString(this.length), SubClassif.INTEGER));
    }

    public ResultValue getElem() throws Exception{
        return new ResultValue(SubClassif.INTEGER
            , new Numeric(String.valueOf(arrayList.size()), SubClassif.INTEGER));
    }

    public void set(int index, ResultValue value) {
        while(index >= arrayList.size())
            arrayList.add(defaultValue);
        arrayList.set(index, value);
    }

    /**
     * Fills the whole array, up to `length`, to `value`. Useful for scalar assignments like arr = 5
     * @param value
     */
    public void fill(ResultValue value) {
        for(int i = 0; i < length; i++)
            arrayList.set(i, value);
    }

    public ResultValue get(int index) {
        if(length != 0 && index >= length)
            throw new ArrayIndexOutOfBoundsException("Index " + index + " is bigger than the max index " + (length - 1) +
                    " (length=" + length + ")");
        if(index < 0)
            throw new ArrayIndexOutOfBoundsException("Index " + index + " must be greater or equal to 0");
        return arrayList.get(index);
    }
}
