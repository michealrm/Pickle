package main.java.pickle.st;

import main.java.pickle.Classif;
import main.java.pickle.SubClassif;

public class STControl extends STEntry {

    SubClassif subClassif;

    public STControl(String symbol, Classif primClassif) {
        super(symbol, primClassif);
    }

    public STControl(String symbol, Classif primClassif, SubClassif subClassif) {
        super(symbol, primClassif);
        this.subClassif = subClassif;
    }
}
