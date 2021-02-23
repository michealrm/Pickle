package main.java.pickle.st;

import main.java.pickle.Classif;
import main.java.pickle.SubClassif;

public class STFunction extends STEntry {

    public SubClassif definedBy;

    public STFunction(String symbol) {
        super(symbol, Classif.FUNCTION);
    }

    public STFunction(String symbol, SubClassif subClassif, SubClassif definedBy) {
        super(symbol, Classif.FUNCTION, subClassif);
        this.definedBy = definedBy;
    }
}
