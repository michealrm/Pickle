package pickle;
public enum SubClassif 
{
    EMPTY("EMPTY"),          // empty
    // OPERAND's subclassifications
    IDENTIFIER("IDENTIFIER"),     // identifier
    INTEGER("INTEGER"),        // integer constant
    INTEGERARR("INTEGERARR"),     // integer array
    FLOAT("FLOAT"),          // float constant
    FLOATARR("FLOATARR"),       // float array
    BOOLEAN("BOOLEAN"),        // boolean constant
    BOOLEANARR("BOOLEANARR"),     // boolean array
    STRING("STRING"),         // string constant
    STRINGARR("STRINGARR"),      // string array
    DATE("DATE"),           // date constant
    DATEARR("DATEARR"),        // date array
    VOID("VOID"),           // void
    // CONTROL's subclassifications
    FLOW("FLOW"),           // flow statement (e.g., if)
    END("END"),            // end statement (e.g., endif)
    FOR_LIMIT("FOR_LIMIT"), // For "to"
    FOR_INCREMENT("FOR_INCREMENT"), // For "by"
    DECLARE("DECLARE"),        // declare statement (e.g., Int)
    // FUNCTION's subclassfications
    BUILTIN("BUILTIN"),        // builtin function (e.g., print)
    USER("USER"),           // user-defined function
    VAR_ARGS("VAR_ARGS"),       // For when a function is declared with a variable number of arguments
    DEBUG("DEBUG"),         // Debug statement
    DEBUG_TOKEN("DEBUG_TOKEN"), // Debug token statement
    DEBUG_ASSIGN("DEBUG_ASSIGN"), // Debug assignment statement
    DEBUG_EXPR("DEBUG_EXPR"), // Debug expression statement
    DEBUG_STMT("DEBUG_STMT"), // Debug statement statement
    DEBUG_VALUE("DEBUG_VALUE"), // Debug value
    // IDENTIFIER's subclassifications
    PRIMITIVE("PRIMITIVE"),      // Primitive type
    FIXED_ARRAY("FIXED_ARRAY"),    // Array type
    UNBOUNDED_ARRAY("UNBOUNDED_ARRAY"), // Pointer
    // IDENTIFIER's Parameter subclassifications
    REFERENCE("REFERENCE"),
    VALUE("VALUE");

    private String name;

    SubClassif(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
