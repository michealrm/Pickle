package pickle.st;

import pickle.Classif;
import pickle.SubClassif;

import java.util.ArrayList;

public class STFunction extends STEntry {

    SubClassif returnType = SubClassif.EMPTY;
    SubClassif definedBy = SubClassif.EMPTY;
    ArrayList<String> parmList = new ArrayList<String>();
    public int numArgs; // ** -1 is VAR ARGS **
    SymbolTable symbolTable;

    public STFunction(String symbol, Classif primClassif, int numArgs, SubClassif definedBy) {
        super(symbol, primClassif);
        this.numArgs = numArgs;
        this.definedBy = definedBy;
    }

    public STFunction(String symbol, Classif primClassif, int numArgs, SubClassif returnType, SubClassif definedBy) {
        this(symbol, primClassif, numArgs, definedBy);
        this.returnType = returnType;
    }


}
