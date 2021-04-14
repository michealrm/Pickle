package pickle;

import java.util.ArrayList;

public class PickleArray {

    private ArrayList<ResultValue> arrayList;

    public PickleArray() {
        arrayList = new ArrayList<>();
    }

    public ResultValue getMaxElem() {
        return null;
    }

    public ResultValue getElem() {
        return null;
    }

    public void add(ResultValue value) {
        arrayList.add(value);
    }

    public ResultValue get(int index) {
        return arrayList.get(index);
    }
}
