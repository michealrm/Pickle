package main.java.pickle.st;

import main.java.pickle.Classif;
import main.java.pickle.SubClassif;

public class STControl extends STEntry {
    public STControl(String symbol) {
        super(symbol, Classif.CONTROL);
    }

    public STControl(String symbol, SubClassif subClassif) {
        super(symbol, Classif.CONTROL, subClassif);
    }
}
