package pickle.st;

import pickle.Classif;
import pickle.SubClassif;
import pickle.Token;

public class STEntry extends Token {

    public SubClassif parameterType;

    public STEntry(String symbol, Classif primClassif) {
        super(symbol);
        this.primClassif = primClassif;
    }

    public STEntry(String symbol, Classif primClassif, SubClassif dclType) {
        super(symbol);
        this.primClassif = primClassif;
        this.dclType = dclType;
    }

    public STEntry(String symbol, Classif primClassif, SubClassif dclType, SubClassif parameterType) {
        super(symbol);
        this.primClassif = primClassif;
        this.dclType = dclType;
        this.parameterType = parameterType;
    }
}
