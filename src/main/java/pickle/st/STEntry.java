package main.java.pickle.st;

import main.java.pickle.Classif;
import main.java.pickle.SubClassif;
import main.java.pickle.Token;

public class STEntry extends Token {

    public STEntry(String symbol, Classif primClassif) {
        super(symbol);
        this.primClassif = primClassif;
    }




}
