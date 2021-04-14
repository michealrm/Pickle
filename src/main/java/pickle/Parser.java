package pickle;

import pickle.exception.ParserException;
import pickle.exception.ScannerTokenFormatException;
import pickle.st.STEntry;
import pickle.st.STIdentifier;

import java.util.HashMap;
import java.util.Stack;

public class Parser {

    public Scanner scan;
    private HashMap<String, Object> storage;
    private Stack<String> flowStack = new Stack<>();

    public static int currentIfLine;
    public static int currentWhileLine;
    public static boolean onStmtLine = false;

    public Parser(Scanner scanner) throws Exception {
        scan = scanner;
        this.storage = new HashMap<String, Object>();
        scan.getNext(); // Call initial getNext() to get first token
    }

    ////////////////
    // Statements //
    ////////////////

    /**
     *
     * @param bExec
     * @return The ResultValue evaluated from the statements. Will likely be EMPTY with an empty string value.
     *  The important part is that scTerminatingStr is set
     */
    ResultValue executeStatements(boolean bExec) throws Exception {
        ResultValue res;
        while(true) {
            if(scan.scanDebug.bShowStmt) { // Print line if any debugging is enabled
                scan.printLineDebug(scan.iSourceLineNr);
            }
            res = new ResultValue(SubClassif.EMPTY, "");
            ResultValue resTemp = executeStmt(bExec);

            // EOF
            if(scan.currentToken.primClassif == Classif.EOF)
                System.exit(0);

            if(resTemp.scTerminatingStr != null && resTemp.iDatatype == SubClassif.END) {
                if(flowStack.isEmpty()) {
                    // Either flow is higher in the call stack or this is an invalid/non-matching termination
                    // ifStmt and whileStmt will catch that after returning res
                    res.scTerminatingStr = resTemp.scTerminatingStr;
                    return res;
                }

                // scTerminatingStr's END token has a matching FLOW at the front of the queue
                String frontFlow = flowStack.peek();
                switch(frontFlow) {
                    case "if":
                        currentIfLine = scan.iSourceLineNr;
                        onStmtLine = true;
                        // If we started an if, that can end with either else or endif
                        // If it ends with else, we need to add an else to the flowQueue that needs to end with endif
                        if(!resTemp.scTerminatingStr.equals("else") && !resTemp.scTerminatingStr.equals("endif")) {
                            errorWithCurrent("Expected an else or endif to terminate an if");
                        }

                        res.scTerminatingStr = resTemp.scTerminatingStr;
                        flowStack.pop();

                        if(resTemp.scTerminatingStr.equals("else"))
                            flowStack.add("else"); // else must be checked later that it ends with an endif
                        break;
                    case "else":
                        if(!resTemp.scTerminatingStr.equals("endif")) {
                            errorWithCurrent("Expected an endif to terminate an else");
                        }

                        res.scTerminatingStr = resTemp.scTerminatingStr;
                        flowStack.pop();
                        break;
                    case "while":
                        currentWhileLine = scan.iSourceLineNr;
                        onStmtLine = true;
                        if(!resTemp.scTerminatingStr.equals("endwhile")) {
                            errorWithCurrent("Expected an endwhile to terminate a while");
                        }

                        res.scTerminatingStr = resTemp.scTerminatingStr;
                        flowStack.pop();
                }

                // Hit a terminator in flow queue, so executeStatements should return to higher statements in call
                // stack to pass scTerminatingStr
                break;
            }
        }

        return res;
    }

    ResultValue executeStmt(boolean bExec) throws Exception {
        if (scan.scanDebug.bShowExpr) {
            switch (scan.currentToken.tokenStr) {
                case "while":
                    System.out.println(String.format("\n>whileStmt: %s", scan.readToColon()));
                    break;
                case "if":
                    System.out.println(String.format("\n>ifStmt: %s", scan.readToColon()));
                    break;
            }
        }

        // Check for FLOW token
        switch(scan.currentToken.tokenStr) {
            case "while":
                flowStack.push("while");
                whileStmt(bExec);   // Will change the token position
                break;
            case "if":
                flowStack.push("if");
                ifStmt(bExec);   // Will change the token position
        }

        // Check for END
        if(scan.currentToken.dclType == SubClassif.END) {
            ResultValue res = new ResultValue(SubClassif.END, scan.currentToken.tokenStr);
            res.scTerminatingStr = scan.currentToken.tokenStr;
            // DO NOT skip past endX;. We need this if XStmt for exception handling

            return res;
        }

        // Check for FLOW token
        switch(scan.currentToken.tokenStr) {
            case "while":
                flowStack.push("while");
                whileStmt(bExec);
                break;
            case "if":
                flowStack.push("if");
                ifStmt(bExec);
        }

        // Check for END
        if(scan.currentToken.dclType == SubClassif.END) {
            ResultValue res = new ResultValue(SubClassif.END, scan.currentToken.tokenStr);
            res.scTerminatingStr = scan.currentToken.tokenStr;
            // DO NOT skip past endX;. We need this if XStmt for exception handling

            return res;
        }

        if(bExec) {
            if(scan.currentToken.primClassif == Classif.EOF) {
                // executeStatements will check for EOF, we just need to get out of this function
                return new ResultValue(SubClassif.EMPTY, "");
            }
            else if (scan.currentToken.dclType == SubClassif.DECLARE) {
                declareStmt();
            }
            else if (scan.currentToken.dclType == SubClassif.IDENTIFIER) {
                return assignmentStmt();
            }
            else if(scan.currentToken.primClassif == Classif.FUNCTION) {
                callBuiltInFunc(bExec);
            }
            // If file starts with a comment and currentToken is empty
            else if(scan.currentToken.primClassif == Classif.EMPTY && scan.currentToken.dclType == SubClassif.EMPTY)
                scan.getNext();
            else if(scan.currentToken.tokenStr.equals("debug")) {
                parseDebugStmt();
            }
            else {
                error("Unsupported statement type for token %s. Classif: %s/%s", scan.currentToken.tokenStr, scan.currentToken.primClassif, scan.currentToken.dclType);
            }
        }
        else
            skipAfter(";");
        return new ResultValue(SubClassif.EMPTY, "");
    }

    /**
     * Calls scan.getNext() until ';' is hit and calls an additional scan.getNext() to end on the position after the ';'
     *
     * @return An EMPTY ResultValue with value ""
     */
    private ResultValue scanStmt() throws Exception {
        skipAfter(";");
        return new ResultValue(SubClassif.EMPTY, "");
    }

    /**
     * Reads a declare statement starting at a DECLARE token, ending at the character after the semicolon
     *
     * Note: declareStmt() was reworked to call assignmentStmt() when current token = variable name after it has
     * put the variable in the symbol table. FOR ARRAYS, the parsing is done within declareStmt() since we can't have
     *
     * @return An empty ResultValue
     * @throws Exception
     */
    private ResultValue declareStmt() throws Exception {
        if(scan.currentToken.dclType != SubClassif.DECLARE)
            error("Expected a DECLARE token like Int, Float, etc.");
        String typeStr = scan.currentToken.tokenStr;

        scan.getNext();
        if(scan.currentToken.dclType != SubClassif.IDENTIFIER)
            error("Expected a variable for the target of a declaration");
        String variableStr = scan.currentToken.tokenStr;

        // Array
        if(scan.nextToken.tokenStr.equals("[")) {
            int iArrayLen = 0;

            scan.getNext(); // Skip to '['
            scan.getNext(); // Skip past '[' to either expr or ']'
            typeStr += "["; // Make, for example, Int -> Int[

            // Array Length
            // It's okay if this (iArrLen) is zero if the array is not unbounded (ex: Int arr[] = 1,2,3;, even though
            // 0 is reserved for unbounded, because in the branch for the list of values, we initially have length=0,
            // but at the end we set the length to the index, which is the same length as the list of values.
            if(!scan.currentToken.tokenStr.equals("]")) {
                ResultValue arrLenExpr = expr(true);
                if(arrLenExpr.iDatatype != SubClassif.INTEGER)
                    error("Array index must be an integer");
                iArrayLen = ((Numeric)arrLenExpr.value).intValue;
                // Now on the ']'
            }
            if(!scan.currentToken.tokenStr.equals("]"))
                errorWithCurrent("Expected a ']' to close " + typeStr);
            scan.getNext(); // Skip past ']' to either = or ';'

            // Instantiation only
            if(scan.currentToken.tokenStr.equals(";")) {
                // Put variable in symbol table, then create and assign PickleArray
                switch(typeStr) {
                    case "Int[":
                        scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, SubClassif.INTEGERARR));
                        assign(variableStr, new ResultValue(SubClassif.INTEGERARR, new PickleArray(SubClassif.INTEGER, iArrayLen)));
                        break;
                    case "Float[":
                        scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, SubClassif.FLOATARR));
                        assign(variableStr, new ResultValue(SubClassif.FLOATARR, new PickleArray(SubClassif.FLOAT, iArrayLen)));
                        break;
                    case "String[":
                        scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, SubClassif.STRINGARR));
                        assign(variableStr, new ResultValue(SubClassif.STRINGARR, new PickleArray(SubClassif.STRING, iArrayLen)));
                        break;
                }
            // Assignment to either to another array or a list of values
            } else if(scan.currentToken.tokenStr.equals("=")) {
                // Assignment to another variable
                if(scan.nextToken.dclType == SubClassif.IDENTIFIER) {
                    scan.getNext(); // Skip to ';'

                    if(!scan.currentToken.tokenStr.equals(";"))
                        errorWithCurrent("Expected ';' after array assignment");

                    String sourceTypeStr = null;
                    STEntry sourceSTEntry = scan.symbolTable.getSymbol(scan.currentToken.tokenStr);
                    if(sourceSTEntry == null)
                        errorWithCurrent("Source variable " + scan.currentToken.tokenStr + " was not found in " +
                                "symbol table for assignment.");

                    // Check that source and destination are the same type
                    SubClassif sourceDclType = sourceSTEntry.dclType;
                    if(sourceDclType == SubClassif.INTEGERARR)
                        sourceTypeStr = "Int[";
                    if(sourceDclType == SubClassif.FLOATARR)
                        sourceTypeStr = "Float[";
                    if(sourceDclType == SubClassif.STRINGARR)
                        sourceTypeStr = "String[";

                    if(!typeStr.equals(sourceTypeStr))
                        errorWithCurrent("Invalid source type for assignment. Destination variable %s was type %s" +
                                ", but source variable %s was type %s", variableStr, typeStr, scan.currentToken.tokenStr, sourceTypeStr);

                    assign(variableStr, getVariableValue(scan.currentToken.tokenStr));
                }
                // Assignment to a list of variables
                else {
                    // currentToken is on the first value

                    // iArrayLen initially 0 (reserved for unbounded), we'll set it after we read in the values
                    PickleArray arr = null;
                    if(typeStr.equals("Int["))
                        arr = new PickleArray(SubClassif.INTEGER, iArrayLen);
                    if(typeStr.equals("Float["))
                        arr = new PickleArray(SubClassif.FLOAT, iArrayLen);
                    if(typeStr.equals("String["))
                        arr = new PickleArray(SubClassif.STRING, iArrayLen);
                    int i = 0;

                    do {
                        scan.getNext(); // Skip past '=' or ','
                        ResultValue arrElement = expr(true);
                        if(typeStr.equals("Int[") && arrElement.iDatatype != SubClassif.INTEGER)
                            errorWithCurrent("Expected an integer for integer array declaration/assignment");
                        if(typeStr.equals("Float[") && arrElement.iDatatype != SubClassif.FLOAT)
                            errorWithCurrent("Expected an float for float array declaration/assignment");
                        if(typeStr.equals("String[") && arrElement.iDatatype != SubClassif.STRING)
                            errorWithCurrent("Expected an string for string array declaration/assignment");

                        arr.set(i, arrElement);
                        i++;
                        // expr() will advance to the ','
                    } while(scan.currentToken.tokenStr.equals(","));

                    if(!scan.currentToken.tokenStr.equals(";"))
                        errorWithCurrent("Since current token is not a ',', we Expected ';' after array assignment");

                    scan.getNext(); // Skip to next statement
                }
            } else {
                error("You must either instantiate with or without '=' assignment.");
            }
        } else if(scan.nextToken.primClassif == Classif.OPERATOR) {
            // Check that variable is not yet instantiated

            // When a variable has already been instantiated, simply update its variable with the new type and value
            //if(scan.symbolTable.getSymbol(variableStr) != null)
            //    error("\"" + variableStr + "\" has already been instantiated");

            // Put variable in the symbol table

            // Only instantiation, no assignment
            if(scan.currentToken.tokenStr.equals(";")) {
                switch(typeStr) {
                    case "Int":
                        scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, SubClassif.INTEGER));
                        break;
                    case "Float":
                        scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, SubClassif.FLOAT));
                        break;
                    case "String":
                        scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, SubClassif.STRING));
                        break;
                    case "Bool":
                        scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, SubClassif.BOOLEAN));
                        break;
                    case "Int[":
                        scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, SubClassif.INTEGERARR));
                        break;
                    case "Float[":
                        scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, SubClassif.FLOATARR));
                        break;
                    case "String[":
                        scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, SubClassif.STRINGARR));
                        break;
                    default:
                        error("Unsupported declare type " + typeStr);
                }

                // Assign statement
                assignmentStmt();
            }
        }

        return new ResultValue(SubClassif.EMPTY, "");
    }

    /**
     * Reads a assignment statement starting at an IDENTIFIER token
     * TODO: Throw in error if variable to be assigned is not in the symbol table
     * @return
     * @throws Exception
     */
    private ResultValue assignmentStmt() throws Exception {
        ResultValue res = null;
        if(scan.currentToken.dclType != SubClassif.IDENTIFIER)
            error("Expected a variable for the target of an assignment");
        String variableName = scan.currentToken.tokenStr;

        scan.getNext();
        if(scan.currentToken.primClassif != Classif.OPERATOR)
            error("Expected assignment operator for assignment statement");

        String operatorStr = scan.currentToken.tokenStr;
        scan.getNext();
        ResultValue exprToAssign = expr(true);
        switch(operatorStr) {
            case "=":
                res = assign(variableName, exprToAssign);
                break;
            case "-=":
                assign(variableName, getVariableValue(variableName).executeOperation(exprToAssign, "-="));
                // TODO: Add line, col number, and parameter num in executeOperation's exception handling (like Parser.error())
                break;
            case "+=":
                assign(variableName, getVariableValue(variableName).executeOperation(exprToAssign, "+="));
                break;
            default:
                error("Expected assignment operator for assignment statement");
        }

        if(!scan.currentToken.tokenStr.equals(";"))
            errorWithCurrent("Expected a ';' to terminate assignment");
        scan.getNext();

        // Debug statement
        if (scan.scanDebug.bShowAssign && res != null)
        {
            System.out.println( String.format("... Assign result into '%s' is '%s'", variableName, res.toString()));
        }

        return res;
    }

    void ifStmt(boolean bExec) throws Exception {
        if(bExec) {
            scan.getNext(); // Skip past the "if" to the opening parenthesis of the condition expression
            ResultValue resCond = evalCond(bExec, "if");
            if(Boolean.parseBoolean(String.valueOf(resCond.value))) {
                if(!scan.currentToken.tokenStr.equals(":"))
                    errorWithCurrent("Expected ':' after if");
                scan.getNext(); // Skip past ':'
                ResultValue resTemp = executeStatements(true);
                if(resTemp.scTerminatingStr.equals("else")) {
                    if(!scan.getNext().tokenStr.equals(":"))
                        errorWithCurrent("Expected ':' after else");
                    resTemp = executeStatements(false);
                }
                if(!resTemp.scTerminatingStr.equals("endif"))
                    errorWithCurrent("Expected an 'endif' for an 'if'");

                scan.getNext(); // Skip past 'endif'
                if(!scan.currentToken.tokenStr.equals(";"))
                    errorWithCurrent("Expected';' after an 'endif'");
                scan.getNext(); // Skip past ';'
            }
            else {
                if(!scan.currentToken.tokenStr.equals(":"))
                    errorWithCurrent("Expected ':' after if");
                scan.getNext(); // Skip past ':'
                ResultValue resTemp = executeStatements(false);
                if(resTemp.scTerminatingStr.equals("else")) {
                    if(!scan.getNext().tokenStr.equals(":"))
                        errorWithCurrent("Expected ':' after 'else'");
                    scan.getNext();
                    resTemp = executeStatements(true);
                }
                if(!scan.getNext().tokenStr.equals(";"))
                    errorWithCurrent("Expected ';' after 'endif'");
                scan.getNext(); // Skip past ';'
            }
        } else {
            skipAfter(":");
            ResultValue resTemp = executeStatements(false);
            if(resTemp.scTerminatingStr.equals("else")) {
                if(!scan.getNext().tokenStr.equals(":"))
                    errorWithCurrent("Expected ':' after else");
                scan.getNext(); // Skip past ':'
                resTemp = executeStatements(bExec);
            }
            if(!resTemp.scTerminatingStr.equals("endif"))
                errorWithCurrent("Expected an 'endif' for an 'if'");
            scan.getNext(); // Skip past endif
            if(!scan.currentToken.tokenStr.equals(";"))
                errorWithCurrent("Expected';' after an 'endif'");
            scan.getNext(); // Skip past ';'
        }
    }
    void whileStmt(boolean bExec) throws Exception {
        if(bExec) {
            scan.getNext(); // Skip past the "while" to the opening parenthesis of the condition expression

            int iStartSourceLineNr = scan.iSourceLineNr; // Save position at the condition to loop back
            int iStartColPos = scan.iColPos;
            int iEndSourceLineNr; // Save position of endwhile to jump to when resCond is false
            int iEndColPos;

            // Get iEndSourceLineNr and iEndColPos
            while(!scan.currentToken.tokenStr.equals("endwhile"))
                scan.getNext();
            //Save iEnd
            iEndSourceLineNr = scan.iSourceLineNr;
            iEndColPos = scan.iColPos;
            // Go back to start of expression for evalCond
            scan.goTo(iStartSourceLineNr, iStartColPos);

            ResultValue resCond = evalCond(bExec, "while");
            while((Boolean)resCond.value) {
                if (!scan.currentToken.tokenStr.equals(":"))
                    errorWithCurrent("Expected ':' after while");
                scan.getNext(); // Skip past ':'

                ResultValue resTemp = executeStatements(true);

                if (!resTemp.scTerminatingStr.equals("endwhile"))
                    errorWithCurrent("Expected an 'endwhile' for a 'while'");
                iEndSourceLineNr = scan.iSourceLineNr;
                iEndColPos = scan.iColPos;

                // Jump back to beginning
                scan.goTo(iStartSourceLineNr, iStartColPos);
                resCond = evalCond(bExec, "while");
            }
            // Jump to endwhile
            scan.goTo(iEndSourceLineNr, iEndColPos);
            if(!scan.currentToken.tokenStr.equals("endwhile"))
                errorWithCurrent("Expected an 'endwhile' for an 'while'");
            scan.getNext(); // Skip past endwhile
            if(!scan.currentToken.tokenStr.equals(";"))
                errorWithCurrent("Expected';' after an 'endwhile'");
            scan.getNext(); // Skip past ';'
        } else {
            // expr() has already been called outside this if/else, so we should be on ':'
            if (!scan.currentToken.tokenStr.equals(":"))
                errorWithCurrent("Expected ':' after while");
            scan.getNext(); // Skip past ':'

            ResultValue resTemp = executeStatements(false);

            if (!resTemp.scTerminatingStr.equals("endwhile"))
                errorWithCurrent("Expected an 'endwhile' for a 'while'");
            scan.getNext(); // Skip past endwhile
            if(!scan.currentToken.tokenStr.equals(";"))
                errorWithCurrent("Expected';' after an 'endwhile'");
            scan.getNext(); // Skip past ';'
        }
    }

    /**
     * This calls any built-in function we made, depending on currentToken's tokenStr
     *
     * @param bFlag
     * @return ResultValue
     * @throws Exception
     */
    private ResultValue callBuiltInFunc(boolean bFlag) throws Exception
    {
        if (scan.currentToken.tokenStr.equals("print"))
        {
            printFunc();
            return null;
        }
        else if (scan.currentToken.tokenStr.equals("LENGTH"))
        {
            return lengthFunc(bFlag);
        }
        else if (scan.currentToken.tokenStr.equals("SPACES"))
        {
            return spacesFunc(bFlag);
        }
        else if (scan.currentToken.tokenStr.equals("ELEM"))
        {
            return elemFunc(bFlag);
        }
        else if (scan.currentToken.tokenStr.equals("MAXELEM"))
        {
            return maxElemFunc(bFlag);
        }
        return new ResultValue();

    }

    private void printFunc() throws Exception {
        StringBuffer msg = new StringBuffer();
        if(!scan.getNext().tokenStr.equals("("))
            errorWithCurrent("Expected '(' for builtin function 'print'");
        do {
            scan.getNext();
            ResultValue msgPart = expr(true);
            switch(msgPart.iDatatype) {
                case INTEGER:
                case FLOAT:
                case BOOLEAN:
                case STRING:
                case IDENTIFIER:
                    msg.append(msgPart.value);
                    break;
                default:
                    error("Unsupported type %s in print function \"%s\"", msgPart.iDatatype, msgPart.value);
            }
            msg.append(" ");    // Add a space whenever there are multiple prints
        } while(scan.currentToken.tokenStr.equals(","));

        if(!scan.currentToken.tokenStr.equals(")"))
            errorWithCurrent("Expected ')' closing after print parameter");
        if(!scan.getNext().tokenStr.equals(";"))
            errorWithCurrent("Expected ';' after print statement");
        scan.getNext(); // Skip past ';'
        System.out.println(msg.toString());
    }

    /**
     * Returns the length
     * @param bExec
     * @return
     * @throws Exception
     */
    private ResultValue lengthFunc(boolean bExec) throws Exception
    {
        scan.getNext();

        // Counts the number of parentheses it finds for error checking
        int iParenCounter = 0;

        // If not found, throw an error; otherwise, continue
        if (!scan.currentToken.tokenStr.equals("("))
        {
            error("Missing left paren");
        }
        iParenCounter++;

        ResultValue param;
        ResultValue result = null;

        scan.getNext();

        if (bExec)
        {
            param = expr(bExec);
            // Convert to String
            // TODO: Create convertType function in ResultValue
            param = ResultValue.convertType(SubClassif.STRING, param);
            if (!scan.currentToken.tokenStr.equals(")"))
            {
                error("Missing right paren");
            }
            else if (param.iDatatype != SubClassif.STRING)
            {
                error("LENGTH can only take in arguments of type String");
            }
            result = new ResultValue(SubClassif.INTEGER, (param.value.toString().length()));
        }
        else
        {
            // Check if function was formed correctly
            while (iParenCounter > 0)
            {
                if (scan.currentToken.tokenStr.equals(";"))
                {
                    error("LENGTH was malformed");
                }

                if (scan.currentToken.tokenStr.equals(")"))
                {
                    iParenCounter--;
                }
                else if (scan.currentToken.tokenStr.equals("("))
                {
                    iParenCounter--;
                }

                if (scan.currentToken.primClassif == Classif.SEPARATOR
                    && scan.nextToken.primClassif == Classif.SEPARATOR)
                {
                    if (!scan.currentToken.tokenStr.equals(")")
                        && scan.nextToken.tokenStr.equals(";"))
                    {
                        error("No arguments between "
                           + scan.currentToken.tokenStr
                           + " " + scan.nextToken.tokenStr);
                    }
                }
                scan.getNext();
            }
        }
        return result;
    }

    private ResultValue spacesFunc(boolean bExec) throws Exception
    {
        scan.getNext();

        int iParenCounter = 0;
        if (!scan.currentToken.tokenStr.equals("("))
        {
            error("Missing left paren");
        }
        iParenCounter++;

        ResultValue param;
        ResultValue result = null;
        scan.getNext();

        if (bExec)
        {
            param = expr(bExec);
            param = ResultValue.convertType(SubClassif.STRING, param);
            if (!scan.currentToken.tokenStr.equals(")"))
            {
                error("Missing right paren");
            }
            else if (param.iDatatype != SubClassif.STRING)
            {
                error("SPACES can only take in arguments of type String");
            }
            boolean bHasSpaces = true;
            String scValue = param.value.toString();

            for (int i = 0; i < scValue.length(); i++)
            {
                if (scValue.charAt(i) != ' '
                    && scValue.charAt(i) != '\t'
                    && scValue.charAt(i) != '\n')
                {
                    bHasSpaces = false;
                }
            }
            if (bHasSpaces)
            {
                result = new ResultValue(SubClassif.BOOLEAN, "T");
            }
            else
            {
                result = new ResultValue(SubClassif.BOOLEAN, "F");
            }
        }
        else
        {
            while (iParenCounter > 0)
            {
                if (scan.currentToken.tokenStr.equals(";"))
                {
                    error("SPACES was malformed");
                }

                if (scan.currentToken.tokenStr.equals(")"))
                {
                    iParenCounter--;
                }
                else if (scan.currentToken.tokenStr.equals("("))
                {
                    iParenCounter++;
                }

                if (scan.currentToken.primClassif == Classif.SEPARATOR
                    && scan.nextToken.primClassif == Classif.SEPARATOR)
                {
                    if (!scan.currentToken.tokenStr.equals(")")
                        && scan.nextToken.tokenStr.equals(";"))
                    {
                        error("No arguments between "
                            + scan.currentToken.tokenStr
                            + " " + scan.nextToken.tokenStr);
                    }
                }
                scan.getNext();
            }
        }
        return result;
    }

    /**
     *
     * @param bExec
     * @return
     * @throws Exception
     */
    private ResultValue maxElemFunc(boolean bExec) throws Exception
    {
        scan.getNext();

        int iParenCounter = 0;
        if (!scan.currentToken.tokenStr.equals("("))
        {
            error("Missing left paren");
        }
        iParenCounter++;

        ResultValue result = null;
        scan.getNext();

        if (bExec)
        {
            // TODO: create this func in ResultValue
            result = convertTokenToResultValue();
            if (result.value instanceof PickleArray)
            {
                scan.getNext();
                if (!scan.currentToken.tokenStr.equals(")"))
                {
                    error("Missing right paren");
                }
                return ((PickleArray) result.value).getMaxElem();
            }
            else
            {
                error("MAXELEM can only take in an array as an argument");
            }
        }
        else
        {
            while (iParenCounter > 0)
            {
                if (scan.currentToken.tokenStr.equals(";"))
                {
                    error("MAXELEM was malformed");
                }

                if (scan.currentToken.tokenStr.equals(")"))
                {
                    iParenCounter--;
                }
                else if (scan.currentToken.tokenStr.equals("("))
                {
                    iParenCounter++;
                }

                if (scan.currentToken.primClassif == Classif.SEPARATOR
                        && scan.nextToken.primClassif == Classif.SEPARATOR)
                {
                    if (!scan.currentToken.tokenStr.equals(")")
                            && scan.nextToken.tokenStr.equals(";"))
                    {
                        error("No arguments between "
                            + scan.currentToken.tokenStr
                            + " " + scan.nextToken.tokenStr);
                    }
                }
                scan.getNext();
            }
        }
        return result;
    }

    private ResultValue elemFunc(boolean bExec) throws Exception
    {
        scan.getNext();

        int iParenCounter = 0;
        if (!scan.currentToken.tokenStr.equals("("))
        {
            error("Missing left paren");
        }
        iParenCounter++;

        ResultValue result = null;
        scan.getNext();

        if (bExec)
        {
            // TODO: create this func in ResultValue
            result = convertTokenToResultValue();
            if (result.value instanceof PickleArray)
            {
                scan.getNext();
                if (!scan.currentToken.tokenStr.equals(")"))
                {
                    error("Missing right paren");
                }
                return ((PickleArray) result.value).getElem();
            }
            else
            {
                error("ELEM can only take in an array as an argument");
            }
        }
        else
        {
            while (iParenCounter > 0)
            {
                if (scan.currentToken.tokenStr.equals(";"))
                {
                    error("ELEM was malformed");
                }

                if (scan.currentToken.tokenStr.equals(")"))
                {
                    iParenCounter--;
                }
                else if (scan.currentToken.tokenStr.equals("("))
                {
                    iParenCounter++;
                }

                if (scan.currentToken.primClassif == Classif.SEPARATOR
                        && scan.nextToken.primClassif == Classif.SEPARATOR)
                {
                    if (!scan.currentToken.tokenStr.equals(")")
                            && scan.nextToken.tokenStr.equals(";"))
                    {
                        error("No arguments between "
                                + scan.currentToken.tokenStr
                                + " " + scan.nextToken.tokenStr);
                    }
                }
                scan.getNext();
            }
        }
        return result;
    }


    //////////
    // Eval //
    //////////

    /**
     * Evaluates an expression and returns the ResultValue
     *
     * Note: currentToken must be at the start of an expression.
     * An expression can also be within parenthesis, like while () <--
     *
     * Ends scan on SEPARATOR token that terminated the expression
     * @param bExecFuncs used for functions
     * @return The ResultValue of the expression
     */
    ResultValue expr(boolean bExecFuncs) throws Exception {
        // Only supports one operator until program 4
        ResultValue expr = null;

        // We're only supporting one set of parenthesis around
        if(scan.currentToken.tokenStr.equals("(")) {
            scan.getNext();
            //ResultValue innerExprValue = expr(); // TODO: Make sure that expr() will stop when it hits a separator
            // TODO: Fix expr
        }

        // Check for unary minus
        Numeric unaryMinusOn = null;
        if(scan.currentToken.tokenStr.equals("-")) {
            scan.getNext(); // Now we should be on either an IDENTIFIER or an INT or FLOAT
            switch(scan.currentToken.dclType) {
                case INTEGER:
                    unaryMinusOn = new Numeric(scan.currentToken.tokenStr, scan.currentToken.dclType);
                    expr = new ResultValue(SubClassif.INTEGER, unaryMinusOn.unaryMinus());
                    break;
                case FLOAT:
                    unaryMinusOn = new Numeric(scan.currentToken.tokenStr, scan.currentToken.dclType);
                    expr = new ResultValue(SubClassif.FLOAT, unaryMinusOn.unaryMinus());
                    break;
                case IDENTIFIER:
                    String variableName = scan.currentToken.tokenStr;
                    ResultValue variableValue = getVariableValue(variableName);
                    if(!variableValue.isNumber) {
                        errorWithCurrent("Expected a variable with a numeric value for unary minus");
                    }
                    unaryMinusOn = (Numeric)variableValue.value;
                    expr = new ResultValue(unaryMinusOn.type, unaryMinusOn.unaryMinus());
                    break;
                default:
                    errorWithCurrent("Expected a INTEGER, FLOAT, or IDENTIFIER for unary minus");
            }
            scan.getNext();
            return expr;
        }
        // Get operand one
        ResultValue resOperand1 = null;
        switch(scan.currentToken.dclType) {
            case INTEGER:
                resOperand1 = new ResultValue(SubClassif.INTEGER, new Numeric(scan.currentToken.tokenStr, scan.currentToken.dclType));
                break;
            case FLOAT:
                resOperand1 = new ResultValue(SubClassif.FLOAT, new Numeric(scan.currentToken.tokenStr, scan.currentToken.dclType));
                break;
            case STRING:
                resOperand1 = new ResultValue(SubClassif.STRING, scan.currentToken.tokenStr);
                break;
            case BOOLEAN:
                resOperand1 = new ResultValue(SubClassif.BOOLEAN, scan.currentToken.tokenStr.equals("T"));
                break;
            case IDENTIFIER:
                resOperand1 = StorageManager.retrieveVariable(scan.currentToken.tokenStr);
                break;
            default:
                errorWithCurrent("Expected a token that can be evaluated in an expression");
        }

        scan.getNext(); // Now we're on the operator

        // Expression is only one operand
        if(scan.currentToken.primClassif == Classif.SEPARATOR) {
            return resOperand1;
        }

        // If currentToken is a FUNCTION, use bExec
        if (scan.currentToken.primClassif == Classif.FUNCTION)
        {
            // Get the ResultValue from callBuiltInFunc and make it into a token
            ResultValue builtInFuncResultValue = callBuiltInFunc(bExecFuncs);
            Token builtInFuncToken = new Token(builtInFuncResultValue.value.toString());
            builtInFuncToken.primClassif = Classif.OPERAND;
            builtInFuncToken.dclType = builtInFuncResultValue.iDatatype;

            // TODO: Deal with adding to infixExpression
        }

        // Get second operand if there is one using nextToken lookahead
        ResultValue resOperand2 = null;
        switch(scan.nextToken.dclType) {
            case INTEGER:
                resOperand2 = new ResultValue(SubClassif.INTEGER, new Numeric(scan.nextToken.tokenStr, scan.nextToken.dclType));
                break;
            case FLOAT:
                resOperand2 = new ResultValue(SubClassif.FLOAT, new Numeric(scan.nextToken.tokenStr, scan.nextToken.dclType));
                break;
            case STRING:
                resOperand2 = new ResultValue(SubClassif.STRING, scan.nextToken.tokenStr);
                break;
            case BOOLEAN:
                resOperand2 = new ResultValue(SubClassif.BOOLEAN, scan.nextToken.tokenStr);
                break;
            case IDENTIFIER:
                resOperand2 = StorageManager.retrieveVariable(scan.nextToken.tokenStr);
                break;
            default:
                // We'll catch the error when we switch the operator
                // We need this ResultValue classification for the unary minus (separator follows minus)
                resOperand2 = new ResultValue(scan.nextToken.dclType, scan.nextToken.tokenStr);
        }

        String operator = scan.currentToken.tokenStr;
        expr = resOperand1.executeOperation(resOperand2, operator); // Note: IDE lies, resOperand1 won't be
        // null (-> NPE) because default case in switch (where resOperand1 would be null) results in an Exception

        scan.getNext(); // On either 2nd operand or separator since max operands is 2
        if(!scan.isSeparator(scan.currentToken.tokenStr) && !scan.isSeparator(scan.nextToken.tokenStr))
            errorWithCurrent("Expected expression to end with a SEPARATOR (e.g. ';', ',')");
        else
            scan.getNext(); // End on SEPARATOR

        // Debug statement
        if (scan.scanDebug.bShowExpr)
        {
            if(expr.toString() == "true") {
                System.out.println(String.format("\n... %s %s %s is T", expr.leftOpGlobal, expr.operationGlobal, expr.rightOpGlobal ));
            } else if(expr.toString() == "false"){
                System.out.println(String.format("\n... %s %s %s is F", expr.leftOpGlobal, expr.operationGlobal, expr.rightOpGlobal));
            } else {
                System.out.println(String.format("\n... %s %s %s is %s", expr.leftOpGlobal, expr.operationGlobal, expr.rightOpGlobal, expr.toString()));
            }
        }

        return expr;
    }



    ResultValue evalCond(boolean bExecFunc, String flowType) throws Exception {
        ResultValue expr = expr(bExecFunc);
        if(expr.iDatatype != SubClassif.BOOLEAN)
            error("%s condition must yield a Bool", flowType);
        return expr;
    }

    /*private void evalDebug() throws Exception {
        scan.getNext();
        switch(scan.currentToken.dclType) {
            case DEBUG:
                boolean debugValue = false;

                if (scan.nextToken.tokenStr.equals("on")) {
                    scan.nextToken.primClassif = Classif.DEBUG;
                    scan.nextToken.dclType = SubClassif.DEBUG_VALUE;
                    debugValue = true;
                } else if (scan.nextToken.tokenStr.equals("off")) {
                    scan.nextToken.primClassif = Classif.DEBUG;
                    scan.nextToken.dclType = SubClassif.DEBUG_VALUE;
                    debugValue = false;
                } else {
                    errorWithCurrent("Expected 'on' or 'off' for debug statement");
                    break;
                }

                if (scan.currentToken.tokenStr.equals("Assign")) {
                    scan.currentToken.primClassif = Classif.DEBUG;
                    scan.currentToken.dclType = SubClassif.DEBUG_ASSIGN;
                    scan.scanDebug.bShowAssign = debugValue;
                } else if (scan.currentToken.tokenStr.equals("Expr")) {
                    scan.currentToken.primClassif = Classif.DEBUG;
                    scan.currentToken.dclType = SubClassif.DEBUG_EXPR;
                } else if (scan.currentToken.tokenStr.equals("Stmt")) {
                    scan.currentToken.primClassif = Classif.DEBUG;
                    scan.currentToken.dclType = SubClassif.DEBUG_STMT;
                }


                break;
        }
    } */

    // Util
    private ResultValue getVariableValue(String variableStr) {
        return StorageManager.retrieveVariable(variableStr);
    }

    /**
     * Skips to `to`, starting with scan.currentToken
     * @param to String to skip to
     */
    private void skipTo(String to) throws Exception {
        while(!scan.currentToken.tokenStr.equals(to))
            scan.getNext();
    }

    private void skipTo(Classif primClassif) throws Exception {
        while(scan.currentToken.primClassif != primClassif)
            scan.getNext();
    }

    private void skipAfter(String to) throws Exception {
        skipTo(to);
        scan.getNext();
    }

    private void skipAfter(Classif primClassif) throws Exception {
        skipTo(primClassif);
        scan.getNext();
    }

    private ResultValue assign(String variableName, ResultValue value) throws Exception {
        STEntry stEntry = scan.symbolTable.getSymbol(variableName);
        if(value != null && stEntry != null && value.iDatatype != stEntry.dclType) {
            if (value.iDatatype == SubClassif.INTEGER || value.iDatatype == SubClassif.FLOAT) {
                value = new ResultValue(stEntry.dclType, new Numeric(String.valueOf(value.value), stEntry.dclType));
            } else {
                error("Variable %s with subclassification %s can not convert assignment value %s in assign statement",
                        variableName, stEntry.dclType, String.valueOf(value.value));
            }
        }
        StorageManager.storeVariable(variableName, value);
        return value;
    }

    /**
     * Check debug statement format and set its values
     * @return rStmt: is true if stmt is valid, false otherwise
     * @throws ScannerTokenFormatException
     */
    private void parseDebugStmt() throws Exception
    {
        scan.getNext();

        switch (scan.currentToken.tokenStr) {
            case "Token":
                scan.getNext();
                switch(scan.currentToken.tokenStr) {
                    case "on":
                        scan.scanDebug.bShowToken = true;
                        break;
                    case "off":
                        scan.scanDebug.bShowToken = false;
                        break;
                    default:
                        errorWithCurrent("Expected 'on' or 'off' for debug statement");
                }
                break;
            case "Assign":
                scan.getNext();
                switch(scan.currentToken.tokenStr) {
                    case "on":
                        scan.scanDebug.bShowAssign = true;
                        break;
                    case "off":
                        scan.scanDebug.bShowAssign = false;
                        break;
                    default:
                        errorWithCurrent("Expected 'on' or 'off' for debug statement");
                }
                break;
            case "Expr":
                scan.getNext();
                switch(scan.currentToken.tokenStr) {
                    case "on":
                        scan.scanDebug.bShowExpr = true;
                        break;
                    case "off":
                        scan.scanDebug.bShowExpr = false;
                        break;
                    default:
                        errorWithCurrent("Expected 'on' or 'off' for debug statement");
                }
                break;
            case "Stmt":
                scan.getNext();
                switch(scan.currentToken.tokenStr) {
                    case "on":
                        scan.scanDebug.bShowStmt = true;
                        break;
                    case "off":
                        scan.scanDebug.bShowStmt = false;
                        scan.printLineDebug(scan.iSourceLineNr);
                        break;
                    default:
                        errorWithCurrent("Expected 'on' or 'off' for debug statement");
                }
                break;
            default:
                errorWithCurrent("Invalid debug statement");
                /* new ParserException(scan.nextToken.iSourceLineNr,
                        "Parser Error: token at column " + scan.nextToken.iColPos
                                + " has invalid format.", "file name");*/
        }

        skipAfter(";");
    }

    private ResultValue convertTokenToResultValue() throws Exception{
        STIdentifier identifier = null;
        ResultValue res = null;
        if(scan.currentToken.primClassif == Classif.IDENTIFIER)
        {
            identifier = (STIdentifier) scan.symbolTable.getSymbol(scan.currentToken.tokenStr);
            if(identifier == null)
            {
                error("variable is undeclared or undefined in this scope");
            }
            res = new ResultValue(scan.currentToken.dclType, storage.get(scan.currentToken.tokenStr));
            //res = new ResultValue(identifier.dclType, storage.get(scan.currentToken.tokenStr));
        }
        else if(scan.currentToken.primClassif == Classif.OPERAND)
        {
            switch(scan.currentToken.dclType)
            {
                case INTEGER:
                    res = new ResultValue(scan.currentToken.dclType, new Numeric(scan.currentToken.tokenStr, SubClassif.INTEGER));
                    break;
                case FLOAT:
                    res = new ResultValue(scan.currentToken.dclType, new Numeric(scan.currentToken.tokenStr, SubClassif.FLOAT));
                    break;
                case BOOLEAN:
                    if(scan.currentToken.tokenStr.equals("T"))
                    {
                        res = new ResultValue(scan.currentToken.dclType, new Boolean(true));
                    }
                    else if(scan.currentToken.tokenStr.equals("F"))
                    {
                        res = new ResultValue(scan.currentToken.dclType, new Boolean(false));
                    }
                    else
                    {
                        error("token's primClassif is OPERAND and subClassif is BOOLEAN but " +
                                "tokenStr " + scan.currentToken.tokenStr + " could not be resolved " +
                                "to a boolean value");
                    }
                    break;
                case STRING:
                    res = new ResultValue(scan.currentToken.dclType, new StringBuilder(scan.currentToken.tokenStr));
                    break;
                default:
                    error("operand is of unhandled type");
            }
        }

        return res;
    }

    // Exceptions

    public void error(String fmt) throws Exception {
        throw new ParserException(scan.iSourceLineNr, scan.iColPos, fmt, scan.sourceFileNm);
    }

    public void error(String fmt, Object... varArgs) throws Exception
    {
        String diagnosticTxt = String.format(fmt, varArgs);
        throw new ParserException(scan.iSourceLineNr
                , scan.iColPos
                , diagnosticTxt
                , scan.sourceFileNm);
    }

    /**
     * Error with the current token. Usually "Read X, Expected Y" when we expect a certain token to follow another token
     * @param fmt The error message to be printed
     */
    public void errorWithCurrent(String fmt) throws Exception {
        error("Read \"%s\", " + fmt, scan.currentToken.tokenStr);
    }

    public void errorWithCurrent(String fmt, Object... varArgs) throws Exception {
        error("Read \"%s\", " + fmt, scan.currentToken.tokenStr, varArgs);
    }


}
