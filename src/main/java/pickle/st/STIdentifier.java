package main.java.pickle.st;

import main.java.pickle.Classif;
import main.java.pickle.SubClassif;

public class STIdentifier extends STEntry {
    public STIdentifier(String symbol, Classif primClassif) {
        super(symbol, primClassif);
    }

    public STIdentifier(String symbol, Classif primClassif, SubClassif subClassif) {
        super(symbol, primClassif, subClassif);
    }
}
