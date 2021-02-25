package main.java.pickle.st;

import main.java.pickle.Classif;
import main.java.pickle.SubClassif;

public class STIdentifier extends STEntry {

    SubClassif structure = SubClassif.EMPTY;
    SubClassif parm = SubClassif.EMPTY;
    int nonLocal;

    public STIdentifier(String symbol, Classif primClassif, SubClassif dclType) {
        super(symbol, primClassif);
        this.dclType = dclType;
    }

    public STIdentifier(String symbol, Classif primClassif, SubClassif type, SubClassif structure) {
        this(symbol, primClassif, type);
        this.structure = structure;
    }

    public STIdentifier(String symbol, Classif primClassif, SubClassif type, SubClassif structDef, SubClassif parm) {
        this(symbol, primClassif, type);
        this.parm = parm;
    }
}
