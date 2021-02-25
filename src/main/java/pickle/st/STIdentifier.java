package pickle.st;

import pickle.Classif;
import pickle.SubClassif;

public class STIdentifier extends STEntry {
    public STIdentifier(String symbol, Classif primClassif) {
        super(symbol, primClassif);
    }

    public STIdentifier(String symbol, Classif primClassif, SubClassif subClassif) {
        super(symbol, primClassif, subClassif);
    }
}
