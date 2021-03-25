package pickle;

import java.util.HashMap;

public class StorageManager {

    private static final HashMap<String, ResultValue> storageManager = new HashMap<>();

    public static void storeVariable(String symbol, ResultValue value) {
        storageManager.put(symbol, value);
    }

    public static ResultValue retrieveVariable(String symbol) {
        return storageManager.get(symbol);
    }
}
