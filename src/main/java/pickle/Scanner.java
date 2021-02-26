package pickle;

import main.java.pickle.exception.SyntaxExceptionHandler;
import pickle.st.STControl;
import pickle.st.STEntry;
import pickle.st.STFunction;
import pickle.st.SymbolTable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class Scanner {

    public String sourceFileNm;
    public ArrayList<String> sourceLineM;
    public SymbolTable symbolTable;
    public char[] textCharM;
    public int iSourceLineNr;
    public int iColPos;
    public Token currentToken;
    public Token nextToken;
    private int lastLine = -1;
    public static boolean isComment = false;

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

        // Print line number
        if(lastLine < iSourceLineNr && iSourceLineNr < sourceLineM.size()) {
            lastLine = iSourceLineNr;
            System.out.println("  " + (iSourceLineNr + 1) + " " + this.sourceLineM.get(iSourceLineNr));
        }

        if(currentToken.primClassif == Classif.EOF)
            return currentToken;
        int[] nextPos = advanceTokenPos(nextToken);
        nextToken = new Token(nextPos[0], nextPos[1]);
        int[] advancedPos = advanceTokenPos(nextToken);
        while(!isComment && advancedPos != null && isTokenWhitespace(nextToken)) {
            nextToken = new Token(advancedPos[0], advancedPos[1]);
            advancedPos = advanceTokenPos(nextToken);
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

                int[] nextPos = skipEmptyLines(iLineNumber, iColNumber);
                sourceLineBefore = iLineNumber;
                iLineNumber = nextPos[0];
                iColNumber = nextPos[1];

                if (sourceLineBefore != iLineNumber) {
                    textCharM = sourceLineM.get(iLineNumber).toCharArray();
                }

                t.tokenStr = t.tokenStr + textCharM[iColNumber];

                // Skip comment
                if (t.tokenStr.equals("//"))
                {
                    isComment = true;

                    t.tokenStr = ""; // For EMPTY continuesToken returns true, so we'll start reading a new token

                    iColNumber = textCharM.length;
                    int[] ret = nextChar(iLineNumber, iColNumber);
                    iLineNumber = ret[0];
                    iColNumber = ret[1];
                    return packagePositions(iLineNumber, iColNumber);
                }

                // Classify token
                setClassification(t);

                // Calculate next position
                nextPos = nextPos(iLineNumber, iColNumber); // We don't want to skip whitespace because that delimits continuesToken
                sourceLineBefore = iLineNumber;
                iLineNumber = nextPos[0];
                iColNumber = nextPos[1];

                if (sourceLineBefore != iLineNumber) {
                    if (iLineNumber >= sourceLineM.size()) {
                        //t.primClassif = Classif.EOF;
                        break;
                    } else {
                        textCharM = sourceLineM.get(iLineNumber).toCharArray();
                    }
                }

                // TODO: Change later for multiline comments
            } while (sourceLineBefore == iLineNumber && continuesToken(t, textCharM[iColNumber]));

            int[] ret = new int[2];
            ret[0] = iLineNumber;
            ret[1] = iColNumber;
            return ret;
        } finally {
            SyntaxExceptionHandler.tokenException(t, t.iSourceLineNr, t.iColPos);
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

        int[] pos = skipEmptyLines(iLineNumber, iColNumber);
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

        ret = skipEmptyLines(iLineNumber, iColNumber);
        return ret;
    }

    private int[] skipEmptyLines(int r, int c) {
        while(r < sourceLineM.size() && sourceLineM.get(r).length() == 0) {
            r++;
            c = 0;

            if(r >= sourceLineM.size())
                break;
        }
        int[] ret = new int[2];
        ret[0] = r;
        ret[1] = c;
        return ret;
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
    public void setClassification(Token token) {
        String tokenStr = token.tokenStr;
        STEntry stEntry;
        if(token.tokenStr.length() == 0) {
            // Empty token
            token.primClassif = Classif.EMPTY;
        } else if(iSourceLineNr >= sourceLineM.size() - 1 && iColPos == sourceLineM.get(iSourceLineNr).length()) {
            // EOF
            token.primClassif = Classif.EOF;
        } else if(isTokenWhitespace(token)) {
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
        }
        else if(isValidIdentifier(token)) {
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
    public boolean continuesToken(Token token, char c) {
        Token copy = new Token(token.tokenStr + c);
        setClassification(copy);
        if(copy.primClassif != Classif.EMPTY && copy.dclType != token.dclType) {
            token.primClassif = copy.primClassif;
            token.dclType = copy.dclType;
        }
        switch (token.primClassif) {
            case EMPTY:
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
        char c = token.tokenStr.charAt(0);
        return c == ',' || c == ';' || c == '[' || c == ']' || c == '(' || c == ')' || c == '\\';
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
        return t.tokenStr.length() == 0 || isCharWhitespace(t.tokenStr.charAt(0));
    }

    private boolean isCharWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n';
    }

}
