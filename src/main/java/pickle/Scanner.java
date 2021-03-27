package pickle;

import pickle.exception.InvalidReturnTypeException;
import pickle.exception.SyntaxExceptionHandler;
import pickle.st.STEntry;
import pickle.st.STFunction;
import pickle.st.STIdentifier;
import pickle.st.SymbolTable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

public class Scanner {

    private static final boolean PRINT_CURRENT_TOKEN_LINE = false;

    public static LinkedHashMap<Integer, SymbolTable> linkedSymbolTable = new LinkedHashMap<>(); // Volatile list of symbol tables used for depth sensing
    public static Integer currentSymbolTableDepth = 0;

    public String sourceFileNm;
    public ArrayList<String> sourceLineM;
    public SymbolTable symbolTable;
    public char[] textCharM;
    public int iSourceLineNr;
    public int iColPos;
    public Token currentToken;
    public Token nextToken;
    private int lastLine = -1; // Used by advanceTokenPos
    public Debug scanDebug = new Debug();

    public Scanner(String fileNm, SymbolTable symbolTable) {
        this.sourceFileNm = fileNm;
        this.symbolTable = symbolTable;
        sourceLineM = new ArrayList<>();

        iSourceLineNr = 0;
        iColPos = 0;

        try {
            readSourceLineM();
        } catch(IOException ioe) {
            ioe.printStackTrace();
            System.exit(1);
        }

        copyTextCharM();

        currentToken = new Token();
        nextToken = new Token();
    }

    /**
     * Opens a file using sourceFileNm and reads source lines using [] into sourceLineM
     */
    public void readSourceLineM() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(sourceFileNm));
        String line;
        while((line = br.readLine()) != null) {
            sourceLineM.add(line);
        }
    }

    /**
     * Copies line from sourceLineM using iSourceLineNr into textCharM
     */
    public void copyTextCharM() {
        textCharM = sourceLineM.get(iSourceLineNr).toCharArray();
    }

    /**
     * Uses iSourceLineNr and iColPos to skip lastToken and whitespace after the token,
     * then reads a token
     * @return The token at iSourceLineNr and iColPos
     */
    public Token getNext() throws Exception {

        iSourceLineNr = nextToken.iSourceLineNr;
        iColPos = nextToken.iColPos;
        currentToken = nextToken;

        if(PRINT_CURRENT_TOKEN_LINE) {
            printLine(iSourceLineNr);
        }

        if(currentToken.primClassif == Classif.EOF)
            return currentToken;
        int[] nextPos = advanceTokenPos(nextToken);
        nextToken = new Token(nextPos[0], nextPos[1]);
        int[] advancedPos = advanceTokenPos(nextToken);

        // Skip whitespace tokens
        while(advancedPos != null && isTokenWhitespace(nextToken)) {
            //System.out.println(nextToken.iSourceLineNr);
            nextToken = new Token(advancedPos[0], advancedPos[1]);
            advancedPos = advanceTokenPos(nextToken);
        }
        
        if (scanDebug.bShowToken)
        {
            currentToken.printToken();
        }

        return currentToken;
    }

    /**
     * Returns where the current token ends + 1, skipping whitespace. While it's
     * advancing it sets the primClassif, subclassif, and tokenStr of token t.
     *
     * @param t The token with the starting position defined in iSourceLineNr and iColPos
     *
     * @return A array of size two where the source line number is defined in position
     * zero and the column position is defined in position one **where the current token
     * ends + 1**
     */
    public int[] advanceTokenPos(Token t) throws Exception {
        try {
            int iLineNumber = t.iSourceLineNr;
            int iColNumber = t.iColPos;
            int sourceLineBefore;

            if(iLineNumber >= sourceLineM.size()) {
                t.primClassif = Classif.EOF;
                return null;
            }

            // Clear tokenStr in case this is a re-read
            t.tokenStr = "";
            t.primClassif = Classif.EMPTY;
            t.dclType = SubClassif.EMPTY;
            char[] textCharM = sourceLineM.get(iLineNumber).toCharArray();

            do {

                // Print line
                // This ends up printing the line for nextToken, so the last token of the last line (usually ;) will
                // be printed after the next line is printed
                if(PRINT_CURRENT_TOKEN_LINE) {
                    printLine(iLineNumber);
                }
                int[] nextPos = skipEmptyLine(iLineNumber, iColNumber);
                sourceLineBefore = iLineNumber;
                iLineNumber = nextPos[0];
                iColNumber = nextPos[1];

                if (sourceLineBefore != iLineNumber) {
                    textCharM = sourceLineM.get(iLineNumber).toCharArray();
                }

                t.tokenStr = t.tokenStr + textCharM[iColNumber];

                // Skip comment
                if (t.tokenStr.equals("/"))
                {
                    t.tokenStr = ""; // For EMPTY continuesToken returns true, so we'll start reading a new token

                    iColNumber = textCharM.length;
                    int[] ret = nextChar(iLineNumber, iColNumber);
                    iLineNumber = ret[0];
                    iColNumber = ret[1];
                    return packagePositions(iLineNumber, iColNumber);
                }

                // Classify token
                setClassification(t);   // Classify each token

                // Calculate next position
                nextPos = nextPos(iLineNumber, iColNumber); // We don't want to skip whitespace because that delimits continuesToken
                sourceLineBefore = iLineNumber;
                iLineNumber = nextPos[0];   // nextPos[2] has first element as line number, second element as column number
                iColNumber = nextPos[1];

                if (sourceLineBefore != iLineNumber) {
                    if (iLineNumber >= sourceLineM.size()) {
                        //t.primClassif = Classif.EOF;
                        break;
                    } else {
                        textCharM = sourceLineM.get(iLineNumber).toCharArray();
                    }
                }

            } while (sourceLineBefore == iLineNumber && continuesToken(t, textCharM[iColNumber]));

            return packagePositions(iLineNumber, iColNumber);
        } finally {
            SyntaxExceptionHandler.tokenException(t, t.iSourceLineNr, t.iColPos);

            // Remove surrounding quotes and replace escaped characters for strings
            if (t.primClassif == Classif.OPERAND && t.dclType == SubClassif.STRING) {
                t.tokenStr = t.tokenStr.replaceAll("^\"|\"$", "");
                t.tokenStr = t.tokenStr.replace("\\n", "\n");
                t.tokenStr = t.tokenStr.replace("\\t", "\t");
            }
        }
    }

    private void printLine(int iLineNumber) {
        if(lastLine < iLineNumber && iLineNumber < sourceLineM.size()) {
            lastLine = iLineNumber;
            System.out.println("  " + (iLineNumber + 1) + " " + this.sourceLineM.get(iLineNumber));
        }
    }

    /**
     * Returns the next character, skipping blank lines if necessary.
     * This does not skip whitespace
     *
     * @param iLineNumber
     * @param iColNumber
     * @return
     */
    public int[] nextPos(int iLineNumber, int iColNumber) {
        if(iLineNumber >= sourceLineM.size())
            return packagePositions(iLineNumber, iColNumber);

        int[] pos = skipEmptyLine(iLineNumber, iColNumber);
        iLineNumber = pos[0];
        iColNumber = pos[1];

        // Increment
        int[] ret;
        if(iColNumber + 1 >= sourceLineM.get(iLineNumber).length()) {
            iLineNumber++;
            iColNumber = 0;
        } else {
            iColNumber++;
        }

        ret = packagePositions(iLineNumber, iColNumber);    // The start and end position of the currently built token
        //ret = skipEmptyLine(iLineNumber, iColNumber);
        return ret;
    }

    private int[] skipEmptyLine(int r, int c) {
        if(r < sourceLineM.size() && sourceLineM.get(r).length() == 0) {
            r++;
            c = 0;
        }
        return packagePositions(r, c);
    }

    private int[] packagePositions(int r, int c) {
        int[] ret = new int[2];
        ret[0] = r;
        ret[1] = c;
        return ret;
    }

    /**
     * Sets the classification and subclassification of the token
     * @param token Token that will be classified
     */
    public void setClassification(Token token) throws Exception {

        String tokenStr = token.tokenStr;
        STEntry stEntry;

        if(token.tokenStr.length() == 0) {

            // Empty token
            token.primClassif = Classif.EMPTY;

        } else if(iSourceLineNr >= sourceLineM.size() - 1 && iColPos == sourceLineM.get(iSourceLineNr).length()) {

            // EOF
            token.primClassif = Classif.EOF;

        } else if(token.tokenStr.equals("debug")) {
            token.primClassif = Classif.DEBUG;
            token.dclType = SubClassif.DEBUG;
        } /* else if(token.tokenStr.compareTo("def") == 0) {   // TODO 1: Add parameters as identifiers within the function symbol table
                                                            // TODO 2: Set STFunction numArgs in parser
            // Data for declaration of a function

            Token funcReturnType = currentToken = getNext();
            Token funcName = currentToken = getNext();

            SubClassif funcReturnTypeClassif = getDataType(funcReturnType.tokenStr.substring(0, funcReturnType.tokenStr.length() - 2));

            if(funcReturnTypeClassif == null)
                throw new InvalidReturnTypeException(String.format("Invalid function return type at line %d, column %d", iSourceLineNr, iColPos));

            // Put function name identifier in current symbol table
            linkedSymbolTable.get(currentSymbolTableDepth).putSymbol(funcName.tokenStr, new STFunction(funcName.tokenStr, Classif.FUNCTION, null, funcReturnTypeClassif, SubClassif.USER));

            // Go to the next depth of symbol table (within the function)
            ++currentSymbolTableDepth;

            // Put the symbol table in the hashmap of symbol tables within the SymbolTable class
            SymbolTable.putSymbolTable(funcName.tokenStr, new SymbolTable());

            // Put the symbol table in the linked hashmap of symbol tables that is used for depth sensing
            linkedSymbolTable.put(currentSymbolTableDepth, SymbolTable.getSymbolTable(funcName.tokenStr));

        } else if(token.tokenStr.compareTo("enddef") == 0) {

            --currentSymbolTableDepth; // We encountered an enddef, so the function has ended, and we go to the outer symbol table

        } else if(  (token.tokenStr.compareTo("Int") == 0 || token.tokenStr.compareTo("Float") == 0 || token.tokenStr.compareTo("String") == 0 || token.tokenStr.compareTo("Date") == 0 || // TODO Set STIdentifier parm and nonlocal values in parser
                (token.tokenStr.length() >= 3 && token.tokenStr.substring(0, 3).compareTo("Int[") == 0) ||
                (token.tokenStr.length() >= 5 && token.tokenStr.substring(0, 5).compareTo("Float[") == 0) ||
                (token.tokenStr.length() >= 6 && token.tokenStr.substring(0, 6).compareTo("String[") == 0) ||
                (token.tokenStr.length() >= 4 && token.tokenStr.substring(0, 4).compareTo("Date[") == 0))
                    && iColPos <= 7) { // iColPos should be <= 7, since it should be a declaration at the start of a line

            // Data for declaration of an identifier
            Token identifierName = currentToken = getNext();
            System.out.println("IDENTIFIER NAME" + identifierName);
            SubClassif dataType = getDataType(token.tokenStr.substring(0, token.tokenStr.length() - 2));

            SubClassif identifierStructure = null;

            if(!isArray(dataType))
                identifierStructure = SubClassif.PRIMITIVE;
            else
                identifierStructure = SubClassif.FIXED_ARRAY;

            // Put the symbol in the current symbol table
            linkedSymbolTable.get(currentSymbolTableDepth).putSymbol(identifierName.tokenStr, new STIdentifier(identifierName.tokenStr, Classif.IDENTIFIER, dataType, identifierStructure));

        } */
         else if(isTokenWhitespace(token)) {

            // Token is whitespace
            token.primClassif = Classif.SEPARATOR;

        } else if(isSeparator(token)) {

            // Separator
          token.primClassif = Classif.SEPARATOR;

        } else if((stEntry = SymbolTable.globalSymbolTable.getSymbol(tokenStr)) != null) {

            // Found in global symbol table
            token.primClassif = stEntry.primClassif;
            token.dclType = stEntry.dclType;

        } else if((stEntry = symbolTable.getSymbol(tokenStr)) != null) {

            // Found in local symbol table
            token.primClassif = stEntry.primClassif;
            token.dclType = stEntry.dclType;

        } else if(token.tokenStr.charAt(0) == '"' || token.tokenStr.charAt(0) == '\'') {

            // String
            token.primClassif = Classif.OPERAND;
            token.dclType = SubClassif.STRING;

        } else if(token.tokenStr.equals("T") || token.tokenStr.equals("F")) {

            token.primClassif = Classif.OPERAND;
            token.dclType = SubClassif.BOOLEAN;

        } else if(PickleUtil.isInt(token.tokenStr)) {

            // Int
            token.primClassif = Classif.OPERAND;
            token.dclType = SubClassif.INTEGER;

        } else if(PickleUtil.isFloat(token.tokenStr)) {

            // Float
            token.primClassif = Classif.OPERAND;
            token.dclType = SubClassif.FLOAT;

        } else if(isOperator(token)) {
            token.primClassif = Classif.OPERATOR;

        }  else if(isValidIdentifier(token)) {  // This is the condition we hit a lot when scanning for new tokens and building up token.tokenStr
            // Identifier
            token.primClassif = Classif.OPERAND;
            token.dclType = SubClassif.IDENTIFIER;
        }
    }

    /**
     * Uses classifications in token and returns true if char c continues the token
     *
     * @param token
     * @param c
     * @return
     */
    public boolean continuesToken(Token token, char c) throws Exception {
        Token copy = new Token(token.tokenStr + c);
        setClassification(copy);    // Classify the currently built token.tokenStr (The copy is one character ahead of token.tokeStr), which should build into the correct classification
        if(copy.primClassif != Classif.EMPTY && copy.dclType != token.dclType) {
            token.primClassif = copy.primClassif;
            token.dclType = copy.dclType;
        }
        switch (token.primClassif) {
            case EMPTY:                 // If there's no classification, then we continue looking for more tokens to build token.tokenStr
                return true;
            case EOF:
            default:
                return false;
            case OPERAND:
                switch (token.dclType) {
                    case INTEGER: // Numerics
                    case FLOAT:
                        return Character.isDigit(c) || c == '.';
                    case BOOLEAN:
                        return containsIn(token.tokenStr + c, "T", "F");
                    case STRING:
                        char start = token.tokenStr.charAt(0);
                        char end = token.tokenStr.charAt(token.tokenStr.length() - 1);

                        return token.tokenStr.length() == 1 || start != end ||
                                token.tokenStr.charAt(token.tokenStr.length() - 2) == '\\';
                    case USER:
                    case IDENTIFIER:
                        return Character.isLetterOrDigit(c);
                    default:
                        // Built-in operands like and, or, etc. that don't have a subclassif
                        return containsIn(token.tokenStr + c, SymbolTable.globalSymbolTable.hm);
                    // ??? Possibly other cases
                }
            case OPERATOR:
                // == ?
                return ((token.tokenStr.equals(">") ||
                        token.tokenStr.equals("<") ||
                        token.tokenStr.equals("=") ||
                        token.tokenStr.equals("!") ||
                        token.tokenStr.equals("+") ||
                        token.tokenStr.equals("-") ||
                        token.tokenStr.equals("*") ||
                        token.tokenStr.equals("/") ||
                        token.tokenStr.equals("^"))
                        && c == '=')
                        || (token.tokenStr.equals("+") && c == '+')
                        || (token.tokenStr.equals("-") && c == '-')
                        || (token.tokenStr.equals("/") && c == '/'); // Comment
            case SEPARATOR:
                return isTokenWhitespace(token) && isCharWhitespace(c);
            // Other separators are only one character
            case FUNCTION:
                switch (token.dclType) {
                    case BUILTIN:
                        return containsIn(token.tokenStr + c, SymbolTable.globalSymbolTable.hm);
                    case USER:
                        return containsIn(token.tokenStr + c, symbolTable.hm);
                    case VOID:
                        return containsIn(token.tokenStr + c, SymbolTable.globalSymbolTable.hm) ||
                                containsIn(token.tokenStr + c, symbolTable.hm);
                }
            case DEBUG:
                switch(token.dclType) {
                    case DEBUG:
                        return Character.isLetterOrDigit(c); // The terminating token should be whitespace, so false will be returned when whitespace is encountered
                }
            case CONTROL:
                return containsIn(token.tokenStr + c, SymbolTable.globalSymbolTable.hm);
        }
    }

    private boolean isOperator(Token token) {
        char c = token.tokenStr.charAt(0); // TODO: Add support for multi-character operators
        switch(c) {
            case '<':
            case '>':
            case '=':
            case '+':
            case '-':
            case '*':
            case '/':
            case ':':
            case '!':
            case '^':
                return true;
            default:
                return false;
        }
    }

    private boolean isSeparator(Token token) {
        return isSeparator(token.tokenStr);
    }

    public boolean isSeparator(String tokenStr) {
        char c = tokenStr.charAt(0);
        return c == ',' || c == ';' || c == ':' || c == '[' || c == ']' || c == '(' || c == ')' || c == '\\';
    }

    private boolean isValidIdentifier(Token token) {
        boolean alphaNumeric = true;
        for(int i = 0; i < token.tokenStr.length(); i++)
            if(!Character.isLetterOrDigit(token.tokenStr.charAt(i)))
                alphaNumeric = false;
        return alphaNumeric;
    }

    public boolean containsIn(String match, String... in) {
        for(String s : in)
            if(s.contains(match))
                return true;
        return false;
    }

    /**
     * Searches hm to see if any key contains match
     * @param match
     * @param hm
     * @return
     */
    public boolean containsIn(String match, HashMap<String, STEntry> hm) {
        Set<String> keySet = hm.keySet();
        for(String s : keySet)
            if(s.contains(match))
                return true;
        return false;
    }

    /**
     * Returns the next character (start of next token), skipping whitespace
     * and lines if necessary
     * @param iSourceLineNr
     * @param iColPos
     * @return
     */
    public int[] nextChar(int iSourceLineNr, int iColPos) {

        String line = sourceLineM.get(iSourceLineNr);
        do {
            int[] pos = nextPos(iSourceLineNr, iColPos);
            iSourceLineNr = pos[0];
            iColPos = pos[1];
        } while(line.isEmpty() || isCharWhitespace(line.charAt(iColPos)));
        int[] pos = new int[2];
        pos[0] = iSourceLineNr;
        pos[1] = iColPos;
        return pos;
    }

    public boolean isTokenWhitespace(Token t) {
        if(t.dclType == SubClassif.STRING)
            return false;
        return t.tokenStr.length() == 0 || isCharWhitespace(t.tokenStr.charAt(0));
    }

    private boolean isCharWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n';
    }

    private SubClassif getDataType(String tokenStr) {
        switch(tokenStr.substring(0, tokenStr.length() - 2)) {
            case "Int":
                return SubClassif.INTEGER;

            case "Int[":
                return SubClassif.INTEGERARR;

            case "Float":
                return SubClassif.FLOAT;

            case "Float[":
                return SubClassif.FLOATARR;

            case "String":
                return SubClassif.STRING;

            case "String[":
                return SubClassif.STRINGARR;

            case "Date":
                return SubClassif.DATE;

            case "Date[":
                return SubClassif.DATEARR;

            default:
                return null;
        }
    }

    private boolean isArray(SubClassif dataType) {
        switch(dataType) {

            case INTEGERARR:
            case FLOATARR:
            case STRINGARR:
            case DATEARR:
                return true;
            default:
                return false;
        }
    }

    public void goTo(int iSourceLineNr, int iColPos) throws Exception {
        this.iSourceLineNr = iSourceLineNr;
        this.iColPos = iColPos;
        currentToken = new Token(iSourceLineNr, iColPos);
        nextToken = new Token(iSourceLineNr, iColPos);
        getNext(); // Load currentToken and nextToken with correct values
    }

    public static class Debug
    {
        public boolean bShowToken;
        public boolean bShowAssign;
        public boolean bShowExpr;
        public boolean bShowStmt;

        public Debug()
        {
            bShowToken = false;
            bShowAssign = false;
            bShowExpr = false;
            bShowStmt = false;
        }
    }
}
