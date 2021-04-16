package pickle;

import java.io.IOException;

/**
 * This class represents a token for the Scanner Class.  
 */
public class Token
{
    /** string from the source program, possibly modified for literals
     */
    public String tokenStr = "";
    /** Parser uses this to help simplify parsing since many subclasses are 
     * combined.  Some values: OPERAND, SEPARATOR, OPERATOR, EMPTY
     */
    public Classif primClassif = Classif.EMPTY;
    /** a sub-classification of a token also used to simplify parsing.
     * Some values for OPERANDs: IDENTIFIER, INTEGER constant, FLOAT constant,
     * STRING constant.
     */
    public SubClassif dclType = SubClassif.EMPTY;
    /** Line number location in the source file for this token.  Line numbers are
     * * relative to 1.
     */
    public int iSourceLineNr = 0;
    /** Column location in the source file for this token.  column positions are 
     * relative to zero.
     */
    public int iColPos = 0;
    
    public boolean isEmpty() {
        return primClassif == Classif.EOF;
    }

    public Token(int iSourceLineNr, int iColPos) {
        this.iSourceLineNr = iSourceLineNr;
        this.iColPos = iColPos;
    }
    public Token(String value)
    {
        this.tokenStr = value;
    }
    public Token()
    {
        this("");   // invoke the other constructor
    }

    public int preced() {
        switch(tokenStr) {
            case "(":
                return 15;
            // How about unary minus?
            case "^":
                return 11;
            case "*":
            case "/":
                return 9;
            case "+":
            case "-":
                return 8;
            case "#":
                return 7;
            case "<":
            case ">":
            case "<=":
            case ">=":
            case "==":
            case "!=":

            case "in":
            case "notin":
                return 6;
            case "not":
                return 5;
            case "and":
            case "or":
                return 4;
        }
        return 0; // Calling functions will need to deal with token with no precedence. But this value is open to change
    }

    public int stkPreced() {
        switch(tokenStr) {
            case "(":
                return 2;
            // How about unary minus?
            case "^":
                return 10;
            case "*":
            case "/":
                return 9;
            case "+":
            case "-":
                return 8;
            case "#":
                return 7;
            case "<":
            case ">":
            case "<=":
            case ">=":
            case "==":
            case "!=":

            case "in":
            case "notin":
                return 6;
            case "not":
                return 5;
            case "and":
            case "or":
                return 4;
        }
        return 0; // Calling functions will need to deal with token with no precedence. But this value is open to change
    }

    /**
     * Prints the primary classification, sub-classification, and token string
     * <p>
     * If the classification is EMPTY, it uses "**garbage**".
     * If the sub-classification is EMPTY, it uses "-".
     */
    public void printToken()
    {
        String primClassifStr;
        String subClassifStr;

        if (primClassif != Classif.EMPTY)
            primClassifStr = primClassif.toString();
        else
            primClassifStr = "**garbage**";

        if (dclType != SubClassif.EMPTY)
            subClassifStr = dclType.toString();
        else
            subClassifStr = "n/a";

        if (primClassif == Classif.OPERAND &&
                dclType == SubClassif.STRING)
        {
            System.out.printf("%-11s %-12s "
                    , primClassifStr
                    , subClassifStr);
            hexPrint(25, tokenStr);
        }
        else
        {
            System.out.printf("%-11s %-12s %s\n"
                    , primClassifStr
                    , subClassifStr
                    , tokenStr);
        }
    }

    /**
     * Prints a string that may contain non-printable characters as two lines.
     * <p>
     * On the first line, it prints printable characters by simply
     * printing the character.  For non-printable characters
     * in the string, it prints ". ".
     * <p>
     * The second line prints a two character hex value for the non printable
     * characters in the string line.  For the printable characters, it prints
     * a space.
     * <p>
     * It is sometimes necessary to print the first line on the end of
     * an existing line of output.  This would make it difficult to properly
     * align the second line of output.  The indent parameter is for indenting
     * the second line.
     * <p><blockquote><pre>
     * Example for the string "\tTX\tTexas\n"
     *      . TX. Texas.
     *      09  09     0A
     * </pre></blockquote><p>
     * @param indent  the number of spaces to indent the second printed line
     * @param str     the string to print which may contain non-printable characters

     */
    public void hexPrint(int indent, String str)
    {
        int len = str.length();
        char [] charray = str.toCharArray();
        char ch;
        // print each character in the string
        for (int i = 0; i < len; i++)
        {
            ch = charray[i];
            if (ch > 31 && ch < 127)   // ASCII printable characters
                System.out.printf("%c", ch);
            else
                System.out.printf(". ");
        }
        System.out.printf("\n");
        // indent the second line to the number of specified spaces
        for (int i = 0; i < indent; i++)
        {
            System.out.printf(" ");
        }
        // print the second line.  Non-printable characters will be shown
        // as their hex value.  Printable will simply be a space
        for (int i = 0; i < len; i++)
        {
            ch = charray[i];
            // only deal with the printable characters
            if (ch > 31 && ch < 127)   // ASCII printable characters
                System.out.printf(" ", ch);
            else
                System.out.printf("%02X", (int) ch);
        }
        System.out.printf("\n");
    }
}
