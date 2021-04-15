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
                    // ifStmt, whileStmt, and forStmt will catch that after returning res
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
                case "for":
                    System.out.println(String.format("\n>forStmt: %s", scan.readToColon()));
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
            case "for":
                flowStack.push("for");
                forStmt(bExec);   // Will change the token position
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
            else if(scan.currentToken.tokenStr.equals("to") || scan.currentToken.tokenStr.equals("by")) {
                initializeTempForVariables();
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

        // We're using nextToken because to fallthrough to assignmentStmt(), we need currentToken to be on variableName
        // Assignment
        if(scan.nextToken.tokenStr.equals("=")) {
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
            }
            assignmentStmt();
        }
        // Array
        else if(scan.nextToken.tokenStr.equals("[")) {
            int iArrayLen = -1;

            scan.getNext(); // Skip to '['
            scan.getNext(); // Skip past '[' to either expr or ']'
            typeStr += "["; // Make, for example, Int -> Int[

            // Array Length
            // It's okay if this (iArrLen) is zero if the array is not unbounded (ex: Int arr[] = 1,2,3;, even though
            // 0 is reserved for unbounded, because in the branch for the list of values, we initially have length=0,
            // but at the end we set the length to the index, which is the same length as the list of values.
            if(!scan.currentToken.tokenStr.equals("]")) {
                if(scan.currentToken.tokenStr.equals("unbound")) {
                    iArrayLen = 0;
                    scan.getNext();
                } else {
                    ResultValue arrLenExpr = expr(true);
                    if (arrLenExpr.iDatatype != SubClassif.INTEGER)
                        error("Array index must be an integer");
                    iArrayLen = ((Numeric) arrLenExpr.value).intValue;
                    // Now on the ']'
                }
            }
            if(!scan.currentToken.tokenStr.equals("]"))
                errorWithCurrent("Expected a ']' to close " + typeStr);
            scan.getNext(); // Skip past ']' to either = or ';'

            // Instantiation only
            if(scan.currentToken.tokenStr.equals(";")) {
                if(iArrayLen == -1)
                    error("If you're only instantiating an array, you must specify size or unbound");

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
                scan.getNext(); // Skip to either identifier, first value in list of values, first value of expr() for fill
                // Assignment to another variable
                if(scan.currentToken.dclType == SubClassif.IDENTIFIER) {
                    scan.getNext();

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
                else if(scan.nextToken.tokenStr.equals(",")){
                    // currentToken is on the first value

                    // If we have an array length, set it. If no length (length is number of values in list we're about
                    // to read) or unbound, init array to unbounded. If no length, we'll set it after we read the values
                    int initLen = iArrayLen == -1 ? 0 : iArrayLen;
                    SubClassif subclassif = null;
                    PickleArray arr = null;
                    if(typeStr.equals("Int[")) {
                        subclassif = SubClassif.INTEGERARR;
                        arr = new PickleArray(SubClassif.INTEGER, initLen);
                    }
                    if(typeStr.equals("Float[")) {
                        subclassif = SubClassif.INTEGERARR;
                        arr = new PickleArray(SubClassif.FLOAT, initLen);
                    }
                    if(typeStr.equals("String[")) {
                        subclassif = SubClassif.STRINGARR;
                        arr = new PickleArray(SubClassif.STRING, initLen);
                    }

                    int i = 0;

                    do {
                        ResultValue arrElement = expr(true);
                        if(typeStr.equals("Int[") && arrElement.iDatatype != SubClassif.INTEGER)
                            errorWithCurrent("Expected an integer for integer array declaration/assignment");
                        if(typeStr.equals("Float[") && arrElement.iDatatype != SubClassif.FLOAT)
                            errorWithCurrent("Expected an float for float array declaration/assignment");
                        if(typeStr.equals("String[") && arrElement.iDatatype != SubClassif.STRING)
                            errorWithCurrent("Expected an string for string array declaration/assignment");

                        arr.set(i, arrElement);
                        i++;

                        if(!scan.currentToken.tokenStr.equals(","))
                            break;
                        scan.getNext();
                    } while(true);

                    if(!scan.currentToken.tokenStr.equals(";"))
                        errorWithCurrent("Since current token is not a ',', we Expected ';' after array assignment");

                    // If no length specified (and not unbound), set length
                    if(iArrayLen == -1)
                        arr.length = arr.arrayList.size();

                    // Now put variable in symbol table and store array into variable using StorageManager
                    scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, subclassif));
                    assign(variableStr, new ResultValue(subclassif,  arr));

                    scan.getNext(); // Skip to next statement
                }
                // Scalar assignment (not a list of variables, so it must be a expr())
                else {
                    if(iArrayLen == -1 || iArrayLen == 0)
                        error("Cannot assign scalar to unbounded / array defined without a length");

                    ResultValue scalar = expr(true);

                    if(!scan.currentToken.tokenStr.equals(";"))
                        errorWithCurrent("Expected ';' after array scalar assignment");

                    PickleArray arr = null;
                    if(typeStr.equals("Int["))
                        arr = new PickleArray(SubClassif.INTEGER, iArrayLen);
                    if(typeStr.equals("Float["))
                        arr = new PickleArray(SubClassif.FLOAT, iArrayLen);
                    if(typeStr.equals("String["))
                        arr = new PickleArray(SubClassif.STRING, iArrayLen);

                    if(scalar.iDatatype != arr.type)
                        error("Scalar evaluated to \"" + scalar.value + "\" which is not a " + arr.type);

                    arr.fill(scalar);

                    // Put variable in symbol table, then create and assign PickleArray
                    switch(typeStr) {
                        case "Int[":
                            scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, SubClassif.INTEGERARR));
                            assign(variableStr, new ResultValue(SubClassif.INTEGERARR, arr));
                            break;
                        case "Float[":
                            scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, SubClassif.FLOATARR));
                            assign(variableStr, new ResultValue(SubClassif.FLOATARR, arr));
                            break;
                        case "String[":
                            scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, SubClassif.STRINGARR));
                            assign(variableStr, new ResultValue(SubClassif.STRINGARR, arr));
                            break;
                    }

                    if(!scan.currentToken.tokenStr.equals(";"))
                        errorWithCurrent("Expected ';' after array assignment to a scalar");

                    scan.getNext(); // skip to next statement
                }
            } else {
                errorWithCurrent("Expected either a '=', assignment, or ';', declaration only for an array definition");
            }
        // Instantiation, no assignment
        } else if(scan.currentToken.tokenStr.equals(";")) {
            switch (typeStr) {
                case "Int":
                    scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, SubClassif.INTEGER));
                    assign(variableStr, new ResultValue(SubClassif.INTEGER, new Numeric("0", SubClassif.INTEGER)));
                    break;
                case "Float":
                    scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, SubClassif.FLOAT));
                    assign(variableStr, new ResultValue(SubClassif.INTEGER, new Numeric("0", SubClassif.INTEGER)));
                    break;
                case "String":
                    scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, SubClassif.STRING));
                    assign(variableStr, new ResultValue(SubClassif.INTEGER, new Numeric("0", SubClassif.INTEGER)));
                    break;
                case "Bool":
                    scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, SubClassif.BOOLEAN));
                    assign(variableStr, new ResultValue(SubClassif.INTEGER, new Numeric("0", SubClassif.INTEGER)));
                    break;
                // Arrays handled above (if we see a '[' after the variable name
                default:
                    error("Unsupported declare type " + typeStr);
            }
        // Instantiation, no assignment since it's not an array or assignment (=)
        } else {
            errorWithCurrent("Variable name for assignment must be follow by either an array definition, equals " +
                    "for assignment, or ';' for instantiation, no assignment.");
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
            errorWithCurrent("Expected assignment operator for assignment statement");

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

    void forStmt(boolean bExec) throws Exception {
        if(bExec) {

            String iteratorVariable;

            if(StorageManager.retrieveVariable("0tempLimit") == null) { // We must initialize the values first

                int iStartOperandColPos;

                // ITERATOR VARIABLE

                scan.getNext(); // Skip past the "for" to the iterator variable

                iteratorVariable = scan.currentToken.tokenStr;

                if (StorageManager.retrieveVariable(scan.currentToken.tokenStr) == null) {   // Store the iterator variable if it doesn't already exit
                    scan.currentToken.primClassif = Classif.IDENTIFIER; // Set the classification to an identifier
                    StorageManager.storeVariable(scan.currentToken.tokenStr, new ResultValue(SubClassif.INTEGER, 0));
                }

                if (scan.currentToken.primClassif != Classif.IDENTIFIER) {
                    errorWithCurrent("Expected identifier for 'for' iterator variable");
                }

                scan.getNext();

                if (!scan.currentToken.tokenStr.equals("=")) {
                    errorWithCurrent("Expected '=' after 'for' iterator variable");
                }

                // ITERATOR INITIAL VALUE

                iStartOperandColPos = scan.iColPos;

                scan.getNext();

                if (!(scan.currentToken.primClassif == Classif.OPERAND)) {
                    errorWithCurrent("Expected operand after 'for' iterator variable");
                }

                if (scan.nextToken.primClassif == Classif.OPERATOR) { // If we found another operand, it's an expression.
                    scan.iColPos = iStartOperandColPos;

                    StorageManager.storeVariable(iteratorVariable, expr(true));    // Store the evaluated expression
                }   // expr() should land us on the "to" position
                else {
                    StorageManager.storeVariable(iteratorVariable, new ResultValue(SubClassif.INTEGER, scan.currentToken.tokenStr));
                    scan.getNext();
                }

                // LIMIT VALUE

                if (!scan.currentToken.tokenStr.equals("to")) {
                    errorWithCurrent("Expected 'to' for 'for' limit");
                }

                iStartOperandColPos = scan.iColPos;

                scan.getNext();

                if (!(scan.currentToken.primClassif == Classif.OPERAND)) {
                    errorWithCurrent("Expected operand after 'for' limit");
                }

                if (scan.nextToken.primClassif == Classif.OPERATOR) { // If we found another operand, it's an expression.
                    scan.iColPos = iStartOperandColPos;

                    StorageManager.storeVariable("0tempLimit", expr(true));    // Store the evaluated expression
                }   // expr() should land us on the "by" position
                else {
                    StorageManager.storeVariable("0tempLimit", new ResultValue(SubClassif.INTEGER, scan.currentToken.tokenStr));
                    scan.getNext();
                }

                // INCREMENT VALUE

                if (!scan.currentToken.tokenStr.equals("by")) {
                    errorWithCurrent("Expected 'by' for 'for' increment");
                }

                iStartOperandColPos = scan.iColPos;

                scan.getNext();

                if (!(scan.currentToken.primClassif == Classif.OPERAND)) {
                    errorWithCurrent("Expected operand after 'for' increment");
                }

                if (scan.nextToken.primClassif == Classif.OPERATOR) { // If we found another operand, it's an expression.
                    scan.iColPos = iStartOperandColPos;

                    StorageManager.storeVariable("0tempIncrement", expr(true));    // Store the evaluated expression
                }   // expr() should land us on the ":" position
                else {
                    StorageManager.storeVariable("0tempIncrement", new ResultValue(SubClassif.INTEGER, scan.currentToken.tokenStr));
                    scan.getNext();
                }

                if (!scan.currentToken.tokenStr.equals(":")) {
                    errorWithCurrent("Expected ':' to end 'for' statement)");
                }

                scan.getNext();

            } else {
                scan.getNext(); // Skip past the "for" to the iterator variable

                iteratorVariable = scan.currentToken.tokenStr;

                skipAfter(":");
            }

            int iStartSourceLineNr = scan.iSourceLineNr; // Save position at the condition to loop back
            int iStartColPos = scan.iColPos;
            int iEndSourceLineNr; // Save position of endwhile to jump to when resCond is false
            int iEndColPos;

            // Get iEndSourceLineNr and iEndColPos
            while(!scan.currentToken.tokenStr.equals("endfor"))
                scan.getNext();
            //Save iEnd
            iEndSourceLineNr = scan.iSourceLineNr;
            iEndColPos = scan.iColPos;
            // Go back to start of expression for evalCond
            scan.goTo(iStartSourceLineNr, iStartColPos);

            while(Integer.parseInt(StorageManager.retrieveVariable(iteratorVariable).value.toString()) <= Integer.parseInt(StorageManager.retrieveVariable("0tempLimit").value.toString())) {

                ResultValue resTemp = executeStatements(true);

                if (!resTemp.scTerminatingStr.equals("endfor"))
                    errorWithCurrent("Expected an 'endfor' for a 'for'");
                iEndSourceLineNr = scan.iSourceLineNr;
                iEndColPos = scan.iColPos;

                // Jump back to beginning
                scan.goTo(iStartSourceLineNr, iStartColPos);

                // Add the increment to the iterator
                StorageManager.storeVariable(iteratorVariable, new ResultValue(SubClassif.INTEGER, Integer.parseInt(StorageManager.retrieveVariable(iteratorVariable).value.toString()) + Integer.parseInt(StorageManager.retrieveVariable("0tempIncrement").value.toString())));
            }
            // Jump to endfor
            scan.goTo(iEndSourceLineNr, iEndColPos);
            if(!scan.currentToken.tokenStr.equals("endfor"))
                errorWithCurrent("Expected an 'endfor' for a 'for'");
            scan.getNext(); // Skip past endfor
            if(!scan.currentToken.tokenStr.equals(";"))
                errorWithCurrent("Expected';' after a 'endfor'");
            scan.getNext(); // Skip past ';'
        } else {
            // Delete temporary limit and increment variables in the StorageManager
            StorageManager.deleteVariable("0tempLimit");
            StorageManager.deleteVariable("0tempIncrement");

            // expr() has already been called outside this if/else, so we should be on ':'
            if (!scan.currentToken.tokenStr.equals(":"))
                errorWithCurrent("Expected ':' after for");
            scan.getNext(); // Skip past ':'

            ResultValue resTemp = executeStatements(false);

            if (!resTemp.scTerminatingStr.equals("endfor"))
                errorWithCurrent("Expected an 'endfor' for a 'endfor'");
            scan.getNext(); // Skip past endwhile
            if(!scan.currentToken.tokenStr.equals(";"))
                errorWithCurrent("Expected';' after an 'endfor'");
            scan.getNext(); // Skip past ';'
        }
    }

    private void initializeTempForVariables() throws Exception {
        if(scan.currentToken.tokenStr.equals("to")) {
            StorageManager.storeVariable("0tempLimit", new ResultValue(SubClassif.INTEGER, 0));
        }
        else if(scan.currentToken.tokenStr.equals("by")) {
            StorageManager.storeVariable("0tempIncrement", new ResultValue(SubClassif.INTEGER, 0));
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

        /*
        Stack<Token> out = new Stack<>();
        Stack<Token> stack = new Stack<>();
        Token popped;

        while(scan.currentToken.primClassif != Classif.SEPARATOR
                && !scan.currentToken.tokenStr.equals("to")
                && !scan.currentToken.tokenStr.equals("by")) {
            switch(scan.currentToken.primClassif) {
                case OPERAND:
                    out.push(scan.currentToken);
                    break;
                case OPERATOR:
                    while(!stack.isEmpty()) {
                        if(scan.currentToken.preced() > stack.peek().stkPreced())
                            break;
                        out.push(stack.pop());
                    }
                    stack.push(scan.currentToken);
            }
            scan.getNext();
        }

        while(!stack.isEmpty())
            out.push(stack.pop());

        while(!out.isEmpty())
            System.out.println(out.pop());
*/
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
                String variableName = scan.currentToken.tokenStr;
                STEntry stEntry = scan.symbolTable.getSymbol(variableName);

                if(stEntry == null)
                    errorWithCurrent("Didn't find " + variableName + " in the symbol table");

                if((stEntry.dclType == SubClassif.INTEGERARR
                        || stEntry.dclType == SubClassif.FLOATARR
                        || stEntry.dclType == SubClassif.STRINGARR)
                        && scan.nextToken.tokenStr.equals("[")) {
                    scan.getNext();
                    scan.getNext();
                    ResultValue index = expr(true);

                    if(index.iDatatype != SubClassif.INTEGER)
                        errorWithCurrent("Expected expression that results in an INTEGER for an array subscript");

                    resOperand1 = ((PickleArray)getVariableValue(variableName).value).get(((Numeric)index.value).intValue);
                } else {
                    resOperand1 = StorageManager.retrieveVariable(scan.currentToken.tokenStr);
                }
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
        if(!scan.isSeparator(scan.currentToken.tokenStr) && !scan.isSeparator(scan.nextToken.tokenStr) && !scan.currentToken.tokenStr.equals("by") && !scan.nextToken.tokenStr.equals("by") && !scan.currentToken.tokenStr.equals(":") && !scan.nextToken.tokenStr.equals(":"))
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
        // Arrays
        if(value != null && stEntry != null && value.iDatatype != stEntry.dclType) {
            if(stEntry.dclType == SubClassif.INTEGERARR
                    || stEntry.dclType == SubClassif.FLOATARR
                    || stEntry.dclType == SubClassif.STRINGARR) {
                // We're only handling array assignments to scalars here
                // For array to array assignments, they're the same subclassif, so StorageManager will assign them
                // But for scalars, since they're not the same type as the array, we need to do the fill here
                PickleArray arr = ((PickleArray) getVariableValue(variableName).value);

                if(arr.type != value.iDatatype)
                    errorWithCurrent("Array \"" + variableName + "\" holds " + arr.type + ", but the scalar " +
                            "was a " + value.iDatatype);

                arr.fill(value);

                value = getVariableValue(variableName); // Set the assignment to the array for StorageManager below
                // since we already did the fill. We don't want to actually
                // assign a scalar to an array variable
            }
            // Float f = 5;, 5 is an integer,m but needs to be stored as a float
            else if (value.iDatatype == SubClassif.INTEGER || value.iDatatype == SubClassif.FLOAT) {
                value = new ResultValue(stEntry.dclType, new Numeric(String.valueOf(value.value), stEntry.dclType));
            }
            else {
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
            res = new ResultValue(scan.currentToken.dclType, storage.get(scan.currentToken.tokenStr));  // WHY USE THIS STORAGE OBJECT, NOT StorageManager?
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
