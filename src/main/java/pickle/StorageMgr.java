package pickle;

import java.util.HashMap;

public class StorageMgr {

    private static HashMap<String, ResultValue> values = new HashMap<>();

    public static Object getValue(String name) {
        return values.get(name);
    }

    public static void putValue(String name, ResultValue value) {
        values.put(name, value);
    }

}
