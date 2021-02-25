package pickle.st;

import pickle.Token;

import java.util.HashMap;

public class SymbolTable {

    public HashMap<String, STEntry> hm = new HashMap<>();

    public void put(String key, STEntry value) {
        hm.put(key, value);
    }
    public STEntry get(String key) {
        return hm.get(key);
    }

}
