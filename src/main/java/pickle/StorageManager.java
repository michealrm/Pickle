package pickle;

import java.util.HashMap;

public class StorageManager {

    private static HashMap<String, Object> GlobalStorageManager;

    public Object retrieve(String symbol) {
        return GlobalStorageManager.get(symbol);
    }

    public void store(String symbol, Object value) {
        GlobalStorageManager.put(symbol, value);
    }
}
