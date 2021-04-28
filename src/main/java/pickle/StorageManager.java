package pickle;

import java.util.HashMap;

public class StorageManager {

    private HashMap<String, ResultValue> storageManager = new HashMap<>();

    public void storeVariable(String symbol, ResultValue value) {
        storageManager.put(symbol, value);
    }

    public ResultValue retrieveVariable(String symbol) {
        return storageManager.get(symbol);
    }

    public ResultValue deleteVariable(String symbol) {
        return storageManager.remove(symbol);
    }
}
