package pickle;

import java.util.HashMap;

public class StorageManager {

    private static HashMap<String, Object> storageManager = new HashMap<>();

    public void storeVariable(String symbol, Object value) {
        storageManager.put(symbol, value);
    }

    public Object retrieveVariable(String symbol) {
        return storageManager.get(symbol);
    }
}
