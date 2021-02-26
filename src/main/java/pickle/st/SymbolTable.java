package pickle.st;

import pickle.Classif;
import pickle.SubClassif;
import pickle.Token;

import java.util.HashMap;

public class SymbolTable {

    public static SymbolTable globalSymbolTable;

    public HashMap<String, STEntry> hm = new HashMap<>();

    public void putSymbol(String key, STEntry value) {
        hm.put(key, value);
    }

    public STEntry getSymbol(String key) {
        return hm.get(key);
    }

    /**
     * Initialize global symbol table
     */
    public static void initGlobal() {
        globalSymbolTable = new SymbolTable();

        globalSymbolTable.putSymbol("def", new STControl("def", Classif.CONTROL, SubClassif.FLOW));
        globalSymbolTable.putSymbol("enddef", new STControl("enddef", Classif.CONTROL, SubClassif.END));

        globalSymbolTable.putSymbol("if", new STControl("if", Classif.CONTROL, SubClassif.FLOW));
        globalSymbolTable.putSymbol("endif", new STControl("endif", Classif.CONTROL, SubClassif.END));
        globalSymbolTable.putSymbol("else", new STControl("else", Classif.CONTROL, SubClassif.END));

        globalSymbolTable.putSymbol("for", new STControl("for", Classif.CONTROL, SubClassif.FLOW));
        globalSymbolTable.putSymbol("endfor", new STControl("endfor", Classif.CONTROL, SubClassif.END));

        globalSymbolTable.putSymbol("while", new STControl("while", Classif.CONTROL, SubClassif.FLOW));
        globalSymbolTable.putSymbol("endwhile", new STControl("endwhile", Classif.CONTROL, SubClassif.END));

        globalSymbolTable.putSymbol("print", new STFunction("print", Classif.FUNCTION, SubClassif.VAR_ARGS, SubClassif.VOID, SubClassif.BUILTIN));

        globalSymbolTable.putSymbol("Int", new STControl("Int", Classif.CONTROL, SubClassif.DECLARE));
        globalSymbolTable.putSymbol("Float", new STControl("Float", Classif.CONTROL, SubClassif.DECLARE));
        globalSymbolTable.putSymbol("String", new STControl("String", Classif.CONTROL, SubClassif.DECLARE));
        globalSymbolTable.putSymbol("Bool", new STControl("Bool", Classif.CONTROL, SubClassif.DECLARE));
        globalSymbolTable.putSymbol("Date", new STControl("Date", Classif.CONTROL, SubClassif.DECLARE));

        globalSymbolTable.putSymbol("and", new STEntry("and", Classif.OPERATOR));
        globalSymbolTable.putSymbol("or", new STEntry("or", Classif.OPERATOR));
        globalSymbolTable.putSymbol("not", new STEntry("not", Classif.OPERATOR));
        globalSymbolTable.putSymbol("in", new STEntry("in", Classif.OPERATOR));
        globalSymbolTable.putSymbol("notin", new STEntry("notin", Classif.OPERATOR));

    }
}
