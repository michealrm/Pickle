package pickle.st;

import pickle.Classif;
import pickle.SubClassif;

import java.util.HashMap;

public class STFunction extends STEntry {

    SubClassif returnType = SubClassif.EMPTY;
    SubClassif definedBy = SubClassif.EMPTY;
    HashMap<String, SubClassif> parmList = new HashMap<String, SubClassif>();
    public int numArgs;
    public boolean hasVarArgs;
    SymbolTable symbolTable;

    int functionLineNumber;
    int functionColumnPosition;

    public STFunction(String symbol, Classif primClassif, int numArgs, SubClassif definedBy) {
        super(symbol, primClassif);
        this.numArgs = numArgs;
        this.definedBy = definedBy;
    }

    public STFunction(String symbol, Classif primClassif, int numArgs, SubClassif returnType, SubClassif definedBy) {
        this(symbol, primClassif, numArgs, definedBy);
        this.returnType = returnType;
        this.symbolTable = symbolTable;
        this.hasVarArgs = hasVarArgs;
    }

    public STFunction(String symbol, Classif primClassif, SymbolTable symbolTable, int numArgs, boolean hasVarArgs, SubClassif returnType, SubClassif definedBy, int functionLineNumber, int functionColumnPosition) {
        this(symbol, primClassif, numArgs, definedBy);
        this.returnType = returnType;
        this.symbolTable = symbolTable;
        this.hasVarArgs = hasVarArgs;
        this.functionLineNumber = functionLineNumber;
        this.functionColumnPosition = functionColumnPosition;
    }
}
