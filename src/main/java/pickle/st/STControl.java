package pickle.st;

import pickle.Classif;
import pickle.SubClassif;

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
