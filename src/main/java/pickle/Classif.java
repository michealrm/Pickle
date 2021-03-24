package pickle;
public enum Classif 
{
    EMPTY,      // empty
    IDENTIFIER, // identifier for declarations
    OPERAND,    // constants, identifier
    OPERATOR,   // + - * / < > = !
    SEPARATOR,  // ( ) , : ; [ ] 
    FUNCTION,   // TBD
    CONTROL,    // TBD
    EOF         // EOF encountered
}

