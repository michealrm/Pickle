package pickle.st;

import pickle.Classif;
import pickle.Scanner;
import pickle.SubClassif;

import java.util.HashMap;

public class SymbolTable {

    public static SymbolTable globalSymbolTable; // Symbols stored here are either Pickle reserve words, or any symbols outside of a function

    public HashMap<String, STEntry> hm = new HashMap<>(); // Individual symbol table

    public String symbolTableName;

    public SymbolTable() {
        
    }

    public SymbolTable(String symbolTableName) {
        this.symbolTableName = symbolTableName;
    }

    // Symbols

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

        globalSymbolTable.putSymbol("print", new STFunction("print", Classif.FUNCTION, -1, SubClassif.VOID, SubClassif.BUILTIN));
        globalSymbolTable.putSymbol("LENGTH", new STFunction("LENGTH", Classif.FUNCTION, 1, SubClassif.BUILTIN));
        globalSymbolTable.putSymbol("SPACES", new STFunction("SPACES", Classif.FUNCTION, 1,SubClassif.BUILTIN));
        globalSymbolTable.putSymbol("ELEM", new STFunction("ELEM", Classif.FUNCTION, 1,SubClassif.BUILTIN));
        globalSymbolTable.putSymbol("MAXELEM", new STFunction("MAXELEM", Classif.FUNCTION, 1,SubClassif.BUILTIN));

        globalSymbolTable.putSymbol("Int", new STControl("Int", Classif.CONTROL, SubClassif.DECLARE));
        globalSymbolTable.putSymbol("Float", new STControl("Float", Classif.CONTROL, SubClassif.DECLARE));
        globalSymbolTable.putSymbol("String", new STControl("String", Classif.CONTROL, SubClassif.DECLARE));
        globalSymbolTable.putSymbol("Bool", new STControl("Bool", Classif.CONTROL, SubClassif.DECLARE));
        globalSymbolTable.putSymbol("Date", new STControl("Date", Classif.CONTROL, SubClassif.DECLARE));
        globalSymbolTable.putSymbol("Int[", new STControl("Int", Classif.CONTROL, SubClassif.DECLARE));
        globalSymbolTable.putSymbol("Float[", new STControl("Float", Classif.CONTROL, SubClassif.DECLARE));
        globalSymbolTable.putSymbol("String[", new STControl("String", Classif.CONTROL, SubClassif.DECLARE));

        globalSymbolTable.putSymbol("and", new STEntry("and", Classif.OPERATOR));
        globalSymbolTable.putSymbol("or", new STEntry("or", Classif.OPERATOR));
        globalSymbolTable.putSymbol("not", new STEntry("not", Classif.OPERATOR));
        globalSymbolTable.putSymbol("in", new STEntry("in", Classif.OPERATOR));
        globalSymbolTable.putSymbol("notin", new STEntry("notin", Classif.OPERATOR));

    }
}
