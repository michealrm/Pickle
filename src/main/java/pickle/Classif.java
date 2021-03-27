package pickle;
public enum Classif 
{
    EMPTY("EMPTY"),      // empty
    IDENTIFIER("IDENTIFIER"), // identifier for declarations
    OPERAND("OPERAND"),    // constants, identifier
    OPERATOR("OPERATOR"),   // + - * / < > = !
    SEPARATOR("SEPARATOR"),  // ( ) , : ; [ ]
    FUNCTION("FUNCTION"),   // TBD
    CONTROL("CONTROL"),    // TBD
    DEBUG("DEBUG"),         // Debug statement
    EOF("EOF");         // EOF encountered

    private String value;

    Classif(String value) {
        this.value = value;
    }

    public String toString() {
        return value;
    }

}

