package pickle.st;

import pickle.Classif;
import pickle.SubClassif;
import pickle.Token;

public class STEntry extends Token {

    public STEntry(String symbol, Classif primClassif) {
        super(symbol);
        this.primClassif = primClassif;
    }
}
