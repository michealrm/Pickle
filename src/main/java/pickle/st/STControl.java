package pickle.st;

import pickle.Classif;
import pickle.SubClassif;

public class STControl extends STEntry {
    public STControl(String symbol) {
        super(symbol, Classif.CONTROL);
    }

    public STControl(String symbol, SubClassif subClassif) {
        super(symbol, Classif.CONTROL, subClassif);
    }
}
