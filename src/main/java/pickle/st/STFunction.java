package pickle.st;

import pickle.Classif;
import pickle.SubClassif;

import java.util.ArrayList;

public class STFunction extends STEntry {

    SubClassif returnType = SubClassif.EMPTY;
    SubClassif definedBy = SubClassif.EMPTY;
    ArrayList<String> parmList = new ArrayList<String>();
    SubClassif varArgs = SubClassif.EMPTY;
    Integer numArgs;
    SymbolTable symbolTable;

    public STFunction(String symbol, Classif primClassif, Object numArgs, SubClassif returnType) {
        super(symbol, primClassif);
        this.returnType = returnType;
    }

    public STFunction(String symbol, Classif primClassif, Object numArgs, SubClassif returnType, SubClassif definedBy) {
        this(symbol, primClassif, numArgs, returnType);
        this.definedBy = definedBy;
    }

    public STFunction(String symbol, Classif primClassif, Object numArgs, SubClassif returnType, SubClassif structDef, ArrayList<String> parmList) {
        this(symbol, primClassif, numArgs, returnType);
        this.parmList = parmList;
    }
}
