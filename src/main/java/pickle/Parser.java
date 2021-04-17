package pickle;

import pickle.exception.ParserException;
import pickle.exception.ScannerTokenFormatException;
import pickle.st.STEntry;
import pickle.st.STFunction;
import pickle.st.STIdentifier;
import pickle.st.SymbolTable;

import javax.xml.transform.Result;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Stack;

public class Parser {

    public Scanner scan;
    private HashMap<String, Object> storage;
    private Stack<String> flowStack = new Stack<>();

    public static int currentIfLine;
    public static int currentWhileLine;
    public static int currentForLine;
    public static boolean onStmtLine = false;
    public static int currentForStmtDepth = 0;

    // Useful in functions like expr() to print all tokens in a range in error messages
    public static int savedRangeStartLine = 0;
    public static int savedRangeStartCol = 0;

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
        //System.out.println(forStmtDepth);
        ResultValue res;
        while(true) {
            if (scan.scanDebug.bShowStmt) { // Print line if any debugging is enabled
                scan.printLineDebug(scan.iSourceLineNr);
            }
            res = new ResultValue(SubClassif.EMPTY, "");
            ResultValue resTemp = executeStmt(bExec);

            // EOF
            if (scan.currentToken.primClassif == Classif.EOF)
                System.exit(0);

            if (resTemp.scTerminatingStr != null && resTemp.iDatatype == SubClassif.END) {
                if (flowStack.isEmpty()) {
                    // Either flow is higher in the call stack or this is an invalid/non-matching termination
                    // ifStmt, whileStmt, and forStmt will catch that after returning res
                    res.scTerminatingStr = resTemp.scTerminatingStr;
                    return res;
                }

                // scTerminatingStr's END token has a matching FLOW at the front of the queue
                String frontFlow = flowStack.peek();
                switch (frontFlow) {
                    case "if":
                        currentIfLine = scan.iSourceLineNr;
                        onStmtLine = true;
                        // If we started an if, that can end with either else or endif
                        // If it ends with else, we need to add an else to the flowQueue that needs to end with endif
                        if (!resTemp.scTerminatingStr.equals("else") && !resTemp.scTerminatingStr.equals("endif")) {
                            errorWithCurrent("Expected an else or endif to terminate an if");
                        }

                        res.scTerminatingStr = resTemp.scTerminatingStr;
                        flowStack.pop();

                        if (resTemp.scTerminatingStr.equals("else"))
                            flowStack.add("else"); // else must be checked later that it ends with an endif
                        break;
                    case "else":
                        if (!resTemp.scTerminatingStr.equals("endif")) {
                            errorWithCurrent("Expected an endif to terminate an else");
                        }

                        res.scTerminatingStr = resTemp.scTerminatingStr;
                        flowStack.pop();
                        break;
                    case "while":
                        currentWhileLine = scan.iSourceLineNr;
                        onStmtLine = true;
                        if (!resTemp.scTerminatingStr.equals("endwhile")) {
                            errorWithCurrent("Expected an endwhile to terminate a while");
                        }

                        res.scTerminatingStr = resTemp.scTerminatingStr;
                        flowStack.pop();
                        break;

                    case "for":
                        currentForLine = scan.iSourceLineNr;
                        onStmtLine = true;
                        if (!resTemp.scTerminatingStr.equals("endfor")) {
                            errorWithCurrent("Expected an endfor to terminate a 'for'");
                        }

                        res.scTerminatingStr = resTemp.scTerminatingStr;
                        flowStack.pop();
                        break;
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

        // Check for END
        if (scan.currentToken.dclType == SubClassif.END) {
            ResultValue res = new ResultValue(SubClassif.END, scan.currentToken.tokenStr);
            res.scTerminatingStr = scan.currentToken.tokenStr;
            // DO NOT skip past endX;. We need this if XStmt for exception handling

            return res;
        }

        // Check for FLOW token
        while(scan.currentToken.tokenStr.equals("while")
                || scan.currentToken.tokenStr.equals("for")
                || scan.currentToken.tokenStr.equals("if")) {
            switch (scan.currentToken.tokenStr) {
                case "while":
                    flowStack.push("while");
                    whileStmt(bExec);   // Will change the token position
                    break;
                case "for":
                    flowStack.push("for");
                    ++currentForStmtDepth;

                    int iStartSourceLineNr = scan.iSourceLineNr; // Save position at the condition to loop back
                    int iStartColPos = scan.iColPos;

                    while (!scan.nextToken.tokenStr.equals(":")) {   // Examine the type of for loop
                        scan.getNext();

                        if (scan.nextToken.tokenStr.equals("to")) {

                            // Go back to start of expression for evalCond
                            scan.goTo(iStartSourceLineNr, iStartColPos);

                            forStmt(bExec);   // Will change the token position

                            break;

                        } else if (scan.nextToken.tokenStr.equals("in")) {

                            // Go back to start of expression for evalCond
                            scan.goTo(iStartSourceLineNr, iStartColPos);

                            forEachStmt(bExec);   // Will change the token position

                            break;

                        } else if (scan.nextToken.tokenStr.equals(":")) {
                            errorWithCurrent("Invalid 'for' statement syntax");
                        }
                    }
                    break;
                case "if":
                    flowStack.push("if");
                    ifStmt(bExec);   // Will change the token position
            }

            // Check for END
            if (scan.currentToken.dclType == SubClassif.END) {
                ResultValue res = new ResultValue(SubClassif.END, scan.currentToken.tokenStr);
                res.scTerminatingStr = scan.currentToken.tokenStr;
                // DO NOT skip past endX;. We need this if XStmt for exception handling

                return res;
            }
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
                scan.getNext(); // Skip past ';' to next statement
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
                        if(i > arr.highestPopulatedValue) {
                            arr.highestPopulatedValue = i;
                        }

                        if(typeStr.equals("String[") && arrElement.iDatatype != SubClassif.STRING)
                            errorWithCurrent("Expected an string for string array declaration/assignment");

                        // Convert string to numeric for numeric array
                        if(!typeStr.equals("String[") && arrElement.iDatatype == SubClassif.STRING) {
                            if(typeStr.equals("Int["))
                                arr.set(i, new ResultValue(SubClassif.INTEGER, new Numeric((String)arrElement.value, SubClassif.INTEGER)));
                            if(typeStr.equals("Float["))
                                arr.set(i, new ResultValue(SubClassif.FLOAT, new Numeric((String)arrElement.value, SubClassif.FLOAT)));
                        }

                        arr.set(i, arrElement);
                        i++;

                        if(!scan.currentToken.tokenStr.equals(","))
                            break;
                        scan.getNext();
                    } while(true);

                    //System.out.println("HIGHEST POPULATED ELEMENT" + arr.highestPopulatedValue);

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
        } else if(scan.nextToken.tokenStr.equals(";")) {
            scan.getNext(); // Skip past variable name
            scan.getNext(); // Skip past ';' to next statement
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
                    assign(variableStr, new ResultValue(SubClassif.STRING, ""));
                    break;
                case "Bool":
                    scan.symbolTable.putSymbol(variableStr, new STEntry(variableStr, Classif.OPERAND, SubClassif.BOOLEAN));
                    assign(variableStr, new ResultValue(SubClassif.BOOLEAN, "F"));
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
        // Either on '[' or assignment operator

        if(scan.currentToken.tokenStr.equals("[")) {
            int index = -1;
            scan.getNext();
            ResultValue expr = expr(true);
            if(expr.iDatatype != SubClassif.INTEGER)
                error("Array subscript expression must evaluate to an integer");
            index = ((Numeric)expr.value).intValue;

            if(!scan.currentToken.tokenStr.equals("]"))
                errorWithCurrent("Expected array subscript to end with a ']'");

            scan.getNext();

            if(!scan.currentToken.tokenStr.equals("="))
                errorWithCurrent("Expected '=' assignment to array reference assignment");

            scan.getNext();

            expr = expr(true); // Value to copy into array reference

            // If array can't hold expr()'s type
            SubClassif type = scan.symbolTable.getSymbol(variableName).dclType;
            if(type == SubClassif.STRING) {
                if(expr.iDatatype != SubClassif.STRING)
                    errorWithCurrent("Cannot assign %s to a STRING index", expr.value);
                String exprStr = ((String)expr.value);
                String str = ((String)getVariableValue(variableName).value);
                str = str.substring(0, index) + exprStr + str.substring(Math.min(str.length(), index + exprStr.length()));
                assign(variableName, new ResultValue(SubClassif.STRING, str));
            } else if(type == SubClassif.INTEGERARR || type == SubClassif.FLOATARR || type == SubClassif.STRINGARR) {
                if(!((type == SubClassif.INTEGERARR && expr.iDatatype == SubClassif.INTEGER)
                        || (type == SubClassif.FLOATARR && expr.iDatatype == SubClassif.FLOAT)
                        || (type == SubClassif.STRINGARR && expr.iDatatype == SubClassif.STRING)))
                    error("Value of array reference assignment had type %s, but array has type %s", expr.iDatatype.toString(), type.toString());
                // Set value
                PickleArray arr = ((PickleArray) getVariableValue(variableName).value);
                arr.set(index, expr);
            }

            if(!scan.currentToken.tokenStr.equals(";"))
                errorWithCurrent("Expected array reference assignment to end with a ';'");

            scan.getNext(); // Skip to next statement

            return expr;
        }
        else {
            if (scan.currentToken.primClassif != Classif.OPERATOR)
                errorWithCurrent("Expected assignment operator for assignment statement");

            String operatorStr = scan.currentToken.tokenStr;
            scan.getNext();
            ResultValue exprToAssign = expr(true);
            switch(operatorStr) {
                case "=":
                    res = assign(variableName, exprToAssign);
                    break;
                case "-=":
                    assign(variableName, getVariableValue(variableName).executeOperation(exprToAssign, "-"));
                    // TODO: Add line, col number, and parameter num in executeOperation's exception handling (like Parser.error())
                    break;
                case "+=":
                    assign(variableName, getVariableValue(variableName).executeOperation(exprToAssign, "+"));
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
            if(StorageManager.retrieveVariable(currentForStmtDepth + "tempLimit") == null) { // We must initialize the values first

                int iStartOperandColPos;

                // ITERATOR VARIABLE

                scan.getNext(); // Skip past the "for" to the iterator variable

                iteratorVariable = scan.currentToken.tokenStr;

                if (StorageManager.retrieveVariable(scan.currentToken.tokenStr) == null) {   // Store the iterator variable if it doesn't already exit
                    scan.currentToken.primClassif = Classif.IDENTIFIER; // Set the classification to an identifier
                    StorageManager.storeVariable(scan.currentToken.tokenStr, new ResultValue(Classif.IDENTIFIER, SubClassif.INTEGER, 0));   // TODO: FIX BUG THAT DELETES NON-USER DECLARED ITERATOR VARIABLES FROM StorageManager
                } else {
                    StorageManager.storeVariable(iteratorVariable, new ResultValue(Classif.IDENTIFIER, SubClassif.INTEGER, 0));
                }

                //System.out.println(scan.currentToken.tokenStr);
                //System.out.println(StorageManager.retrieveVariable(scan.currentToken.tokenStr).iPrimClassif);
                //System.out.println(StorageManager.retrieveVariable("i").iPrimClassif);


                if (StorageManager.retrieveVariable(scan.currentToken.tokenStr).iPrimClassif != Classif.IDENTIFIER) {
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
                    scan.getNext();
                }
                //System.out.println(StorageManager.retrieveVariable("i").iPrimClassif);
                //System.out.println(StorageManager.retrieveVariable("i").iPrimClassif);

                // LIMIT VALUE

                if (!scan.currentToken.tokenStr.equals("to")) {
                    errorWithCurrent("Expected 'to' for 'for' limit");
                }

                iStartOperandColPos = scan.iColPos;

                scan.getNext();

                //.out.println("TOKEN STRING " + scan.currentToken.tokenStr);
                //System.out.println(scan.currentToken.primClassif);
                //System.out.println(scan.currentToken.dclType);

                if (scan.currentToken.primClassif != Classif.OPERAND && scan.currentToken.primClassif != Classif.FUNCTION) {
                    errorWithCurrent("Expected operand after 'for' limit");
                }

                if (scan.nextToken.primClassif == Classif.OPERATOR || scan.currentToken.primClassif == Classif.FUNCTION) { // If we found another operand, it's an expression.
                    scan.iColPos = iStartOperandColPos;

                    StorageManager.storeVariable(currentForStmtDepth + "tempLimit", expr(true));    // Store the evaluated expression
                }   // expr() should land us on the "by" position
                else {
                    StorageManager.storeVariable(currentForStmtDepth + "tempLimit", new ResultValue(SubClassif.INTEGER, scan.currentToken.tokenStr));
                    scan.getNext();
                }

                // INCREMENT VALUE

                if (!scan.currentToken.tokenStr.equals("by")) {

                    StorageManager.storeVariable(currentForStmtDepth + "tempIncrement", new ResultValue(SubClassif.INTEGER, "1"));

                } else {

                    iStartOperandColPos = scan.iColPos;

                    scan.getNext();

                    if (!(scan.currentToken.primClassif == Classif.OPERAND)) {
                        errorWithCurrent("Expected operand after 'for' increment");
                    }

                    if (scan.nextToken.primClassif == Classif.OPERATOR) { // If we found another operand, it's an expression.
                        scan.iColPos = iStartOperandColPos;

                        StorageManager.storeVariable(currentForStmtDepth + "tempIncrement", expr(true));    // Store the evaluated expression
                    }   // expr() should land us on the ":" position
                    else {
                        StorageManager.storeVariable(currentForStmtDepth + "tempIncrement", new ResultValue(SubClassif.INTEGER, scan.currentToken.tokenStr));
                        scan.getNext();
                    }
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

            //System.out.println("SIZE " + Integer.parseInt(StorageManager.retrieveVariable(currentForStmtDepth + "tempLimit").toString()));

            while(Integer.parseInt(StorageManager.retrieveVariable(iteratorVariable).toString()) < Integer.parseInt(StorageManager.retrieveVariable(currentForStmtDepth + "tempLimit").toString())) {
                //System.out.println("INDEX " + StorageManager.retrieveVariable(iteratorVariable).toString());

                //System.out.println("VARIABLE: " + iteratorVariable + " " + StorageManager.retrieveVariable(iteratorVariable).iPrimClassif);
                ResultValue resTemp = executeStatements(true);

                if (!resTemp.scTerminatingStr.equals("endfor"))
                    errorWithCurrent("Expected an 'endfor' for a 'for'");
                iEndSourceLineNr = scan.iSourceLineNr;
                iEndColPos = scan.iColPos;

                // Jump back to beginning
                scan.goTo(iStartSourceLineNr, iStartColPos);

                //System.out.println("Depth " + currentForStmtDepth);
                //System.out.println("Iterator " + StorageManager.retrieveVariable(iteratorVariable).toString());
                //System.out.println("Increment " + Integer.parseInt(StorageManager.retrieveVariable(currentForStmtDepth + "tempIncrement").toString()));
                //System.out.println();

                // Add the increment to the iterator
                StorageManager.storeVariable(iteratorVariable, new ResultValue(SubClassif.INTEGER, Integer.parseInt(StorageManager.retrieveVariable(iteratorVariable).toString()) + Integer.parseInt(StorageManager.retrieveVariable(currentForStmtDepth + "tempIncrement").toString())));
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
        // Delete temporary limit and increment variables in the StorageManager
        StorageManager.deleteVariable(currentForStmtDepth + "tempLimit");
        StorageManager.deleteVariable(currentForStmtDepth + "tempIncrement");
        --currentForStmtDepth;
    }

    void forEachStmt(boolean bExec) throws Exception {
        if(bExec) {

            String iteratorVariable;

            if(StorageManager.retrieveVariable(currentForStmtDepth + "iteratorPosition") == null) {

                StorageManager.storeVariable(currentForStmtDepth + "iteratorPosition", new ResultValue(SubClassif.INTEGER, 0));

                // ITERATOR VARIABLE

                scan.getNext(); // Skip past the "for" to the iterator variable

                iteratorVariable = scan.currentToken.tokenStr;

                scan.getNext();

                if (!scan.currentToken.tokenStr.equals("in")) {
                    errorWithCurrent("Expected 'in' after 'for each' iterator variable");
                }

                // ITERATION OBJECT VALUE

                scan.getNext();

                //System.out.println(scan.currentToken.tokenStr);
                //System.out.println(StorageManager.retrieveVariable(scan.currentToken.tokenStr).iDatatype);
                //System.out.println(((PickleArray) StorageManager.retrieveVariable(scan.currentToken.tokenStr).value).type);

                //if (scan.currentToken.primClassif == Classif.OPERAND || scan.currentToken.dclType == SubClassif.STRING) {
                if (StorageManager.retrieveVariable(scan.currentToken.tokenStr).iDatatype == SubClassif.STRING) {
                    if (StorageManager.retrieveVariable(iteratorVariable) == null) {   // Store the iterator variable if it doesn't already exist
                        StorageManager.storeVariable(iteratorVariable, new ResultValue(SubClassif.STRING, ""));
                    }

                    //System.out.println(scan.currentToken.dclType);

                    if (scan.currentToken.dclType != SubClassif.IDENTIFIER && scan.currentToken.dclType != SubClassif.STRING) {
                        errorWithCurrent("Expected identifier for 'for' iterator variable");
                    }

                    if (scan.nextToken.primClassif == Classif.OPERATOR) { // If we found another operator, it's an expression

                        StorageManager.storeVariable(currentForStmtDepth + "tempIteratorObject", expr(true));    // Store the evaluated expression
                    }   // expr() should land us on the ":" position
                    else {
                        StorageManager.storeVariable(currentForStmtDepth + "tempIteratorObject", StorageManager.retrieveVariable(scan.currentToken.tokenStr));
                        scan.getNext();
                    }
                } else if (StorageManager.retrieveVariable(scan.currentToken.tokenStr).iDatatype == SubClassif.FIXED_ARRAY
                        || StorageManager.retrieveVariable(scan.currentToken.tokenStr).iDatatype == SubClassif.UNBOUNDED_ARRAY
                        || StorageManager.retrieveVariable(scan.currentToken.tokenStr).iDatatype == SubClassif.INTEGERARR
                        || StorageManager.retrieveVariable(scan.currentToken.tokenStr).iDatatype == SubClassif.FLOATARR
                        || StorageManager.retrieveVariable(scan.currentToken.tokenStr).iDatatype == SubClassif.STRINGARR
                        || StorageManager.retrieveVariable(scan.currentToken.tokenStr).iDatatype == SubClassif.BOOLEANARR
                        || StorageManager.retrieveVariable(scan.currentToken.tokenStr).iDatatype == SubClassif.DATEARR) {



                    if (StorageManager.retrieveVariable(iteratorVariable) == null) {   // Store the iterator variable if it doesn't already exist
                        StorageManager.storeVariable(iteratorVariable, new ResultValue( ((PickleArray) StorageManager.retrieveVariable(scan.currentToken.tokenStr).value).type, 0));
                    }

                    //System.out.println(scan.currentToken.dclType);
                    if (StorageManager.retrieveVariable(iteratorVariable).iDatatype != ((PickleArray) StorageManager.retrieveVariable(scan.currentToken.tokenStr).value).type) {
                        errorWithCurrent("Expected identifier for 'for' iterator variable");
                    }

                    /*if (scan.nextToken.primClassif == Classif.OPERATOR) { // If we found another operator, it's an expression

                        StorageManager.storeVariable(currentForStmtDepth + "tempIteratorObject", expr(true));    // Store the evaluated expression
                    }   // expr() should land us on the ":" position
                    else {
                        StorageManager.storeVariable(currentForStmtDepth + "tempIteratorObject", StorageManager.retrieveVariable(scan.currentToken.tokenStr));
                        scan.getNext();
                    }*/

                    StorageManager.storeVariable(currentForStmtDepth + "tempIteratorObject", StorageManager.retrieveVariable(scan.currentToken.tokenStr));
                    scan.getNext();

                } else {
                    errorWithCurrent("Incorrect type for 'for each' iterator object");
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
            //System.out.println(StorageManager.retrieveVariable(iteratorVariable).iDatatype);
            if(StorageManager.retrieveVariable(iteratorVariable).iDatatype == SubClassif.STRING) {
                //System.out.println(StorageManager.retrieveVariable(iteratorVariable).toString());
                //System.out.println(Integer.parseInt(StorageManager.retrieveVariable(currentForStmtDepth + "iteratorPosition").toString()));
                //System.out.println(iteratorVariable);
                //System.out.println(StorageManager.retrieveVariable(currentForStmtDepth + "tempIteratorObject").toString());
                while(Integer.parseInt(StorageManager.retrieveVariable(currentForStmtDepth + "iteratorPosition").toString()) < StorageManager.retrieveVariable(currentForStmtDepth + "tempIteratorObject").toString().length()) {

                    // Store the new value in the iterator variable
                    StorageManager.storeVariable(iteratorVariable, new ResultValue(SubClassif.STRING, StorageManager.retrieveVariable(currentForStmtDepth + "tempIteratorObject").toString().charAt(Integer.parseInt(StorageManager.retrieveVariable(currentForStmtDepth + "iteratorPosition").toString()))));

                    // Increment the position
                    StorageManager.storeVariable(currentForStmtDepth + "iteratorPosition", new ResultValue(SubClassif.INTEGER, Integer.parseInt(StorageManager.retrieveVariable(currentForStmtDepth + "iteratorPosition").toString()) + 1));

                    ResultValue resTemp = executeStatements(true);

                    if (!resTemp.scTerminatingStr.equals("endfor"))
                        errorWithCurrent("Expected an 'endfor' for a 'for'");
                    iEndSourceLineNr = scan.iSourceLineNr;
                    iEndColPos = scan.iColPos;

                    // Jump back to beginning
                    scan.goTo(iStartSourceLineNr, iStartColPos);
                }
            } else if ( StorageManager.retrieveVariable(currentForStmtDepth + "tempIteratorObject").iDatatype == SubClassif.FIXED_ARRAY
                    || StorageManager.retrieveVariable(currentForStmtDepth + "tempIteratorObject").iDatatype == SubClassif.UNBOUNDED_ARRAY
                    || StorageManager.retrieveVariable(currentForStmtDepth + "tempIteratorObject").iDatatype == SubClassif.INTEGERARR
                    || StorageManager.retrieveVariable(currentForStmtDepth + "tempIteratorObject").iDatatype == SubClassif.FLOATARR
                    || StorageManager.retrieveVariable(currentForStmtDepth + "tempIteratorObject").iDatatype == SubClassif.STRINGARR
                    || StorageManager.retrieveVariable(currentForStmtDepth + "tempIteratorObject").iDatatype == SubClassif.BOOLEANARR
                    || StorageManager.retrieveVariable(currentForStmtDepth + "tempIteratorObject").iDatatype == SubClassif.DATEARR) {

                //System.out.println("SIZE " + ((PickleArray) StorageManager.retrieveVariable(currentForStmtDepth + "tempIteratorObject").value).arrayList.size());

                while(Integer.parseInt(StorageManager.retrieveVariable(currentForStmtDepth + "iteratorPosition").toString()) < ( (PickleArray) StorageManager.retrieveVariable(currentForStmtDepth + "tempIteratorObject").value).arrayList.size()) {

                    //System.out.println(StorageManager.retrieveVariable(iteratorVariable));
                    //System.out.println(StorageManager.retrieveVariable(currentForStmtDepth + "tempIteratorObject").value instanceof PickleArray);
                    // Store the new value in the iterator variable
                    StorageManager.storeVariable(iteratorVariable, new ResultValue(StorageManager.retrieveVariable(iteratorVariable).iDatatype, ((PickleArray) StorageManager.retrieveVariable(currentForStmtDepth + "tempIteratorObject").value).arrayList.get(Integer.parseInt(StorageManager.retrieveVariable(currentForStmtDepth + "iteratorPosition").toString()))));

                    // Increment the position
                    StorageManager.storeVariable(currentForStmtDepth + "iteratorPosition", new ResultValue(SubClassif.INTEGER, Integer.parseInt(StorageManager.retrieveVariable(currentForStmtDepth + "iteratorPosition").toString()) + 1));

                    ResultValue resTemp = executeStatements(true);

                    if (!resTemp.scTerminatingStr.equals("endfor"))
                        errorWithCurrent("Expected an 'endfor' for a 'for'");
                    iEndSourceLineNr = scan.iSourceLineNr;
                    iEndColPos = scan.iColPos;

                    // Jump back to beginning
                    scan.goTo(iStartSourceLineNr, iStartColPos);
                }

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

        StorageManager.deleteVariable(currentForStmtDepth + "iteratorPosition");
        --currentForStmtDepth;
    }

    private void initializeTempForVariables() throws Exception {
        if(scan.currentToken.tokenStr.equals("to")) {
            StorageManager.storeVariable(currentForStmtDepth + "tempLimit", new ResultValue(SubClassif.INTEGER, 0));
        }
        else if(scan.currentToken.tokenStr.equals("by")) {
            StorageManager.storeVariable(currentForStmtDepth + "tempIncrement", new ResultValue(SubClassif.INTEGER, 0));
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
                    msg.append(msgPart.toString());
                    break;
                case INTEGERARR:
                case FLOATARR:
                case STRINGARR:
                    PickleArray arr = ((PickleArray)msgPart.value);
                    StringBuilder sb = new StringBuilder();
                    if(arr.highestPopulatedValue != 0)
                        sb.append(arr.get(0));
                    for(int i = 1; i <= arr.highestPopulatedValue; i++) {
                        sb.append(" ");
                        sb.append(arr.get(i));
                    }
                    msg.append(sb.toString());
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
            result = new ResultValue(SubClassif.INTEGER, (param.toString().length()));
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
            String scValue = param.toString();

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
     * @param bExec used for functions
     * @return The ResultValue of the expression
     */
    ResultValue expr(boolean bExec) throws Exception {
        saveLocationForRange();
        // First we'll handle if `bExec` == false
        if(!bExec) {
            while(continuesExpr(scan.currentToken))
                scan.getNext();
            return new ResultValue(SubClassif.EMPTY, "");
        }

        // Convert infix to postfix to evaluate
        Stack<Token> out = new Stack<>();
        Stack<Token> stack = new Stack<>();
        boolean foundLParen = false;
        infix_to_postfix_loop:
        while(continuesExpr(scan.currentToken)) {
            // Evaluate starting from currentToken. Converts results from things like array references or variables into a Token
            Token t = scan.currentToken;

            // If array, call expr() on it and use the ResultValue as Token t, leaving currentToken on the ']' since
            // we call scan.getNext() at the end of the while
            if(scan.nextToken.tokenStr.equals("[")) {
                ResultValue indexResult = null;
                String variableName = scan.currentToken.tokenStr;
                STEntry stEntry = scan.symbolTable.getSymbol(variableName);

                if(stEntry == null)
                    errorWithCurrent("Variable \"%s\" not found in symbol table");
                if(!isArray(stEntry) && stEntry.dclType != SubClassif.STRING)
                    errorWithCurrent("%s with type %s cannot be subscripted because it is not an array or string", variableName, stEntry.dclType);

                scan.getNext(); // Skip to '['
                scan.getNext(); // Skip to first item in index

                indexResult = expr(true);
                int index = ((Numeric) indexResult.value).intValue;

                if(indexResult.iDatatype != SubClassif.INTEGER)
                    error("Index evaluated to %s, which is not an integer");

                if(!scan.currentToken.tokenStr.equals("]"))
                    errorWithCurrent("Expected for array subscript to end with a ']'");

                if(stEntry.dclType == SubClassif.STRING) {
                    String str = ((String)getVariableValue(variableName).value);
                    char value = str.charAt(index);
                    t = new Token(String.valueOf(value));
                    t.primClassif = Classif.OPERAND;
                    t.dclType = SubClassif.STRING;
                }
                else {
                    PickleArray arr = ((PickleArray) getVariableValue(variableName).value);
                    ResultValue value = arr.get(index);

                    if(((Numeric) indexResult.value).intValue > arr.highestPopulatedValue) {
                        arr.highestPopulatedValue = ((Numeric) indexResult.value).intValue;

                        if( ( ((Numeric) indexResult.value).intValue - arr.highestPopulatedValue) > 1) {

                            // Fill array up to the highest referenced index
                            for(int i = arr.highestPopulatedValue; i < ((Numeric) indexResult.value).intValue; i++)
                                arr.arrayList.add(arr.defaultValue);
                        }
                    }

                    t = new Token(value.toString());
                    scan.setClassification(t);
                }

                // Don't skip past the ']', we'll do that at the end of the while loop
            }
            else if(scan.currentToken.dclType == SubClassif.IDENTIFIER) {
                boolean unaryMinus = false;
                if(t.tokenStr.startsWith("-")) {
                    unaryMinus = true;
                    t.tokenStr = t.tokenStr.substring(1);
                }
                ResultValue value = getVariableValue(scan.currentToken.tokenStr);
                if(value.iDatatype == SubClassif.STRING) {
                    t = new Token("\"" + value.toString() + "\"");
                    t.tokenStr = t.tokenStr.substring(1, t.tokenStr.length() - 1);
                    t.primClassif = Classif.OPERAND;
                    t.dclType = SubClassif.STRING;
                } else {
                    t = new Token(value.toString());
                    scan.setClassification(t);
                }

                if(unaryMinus)
                    t.tokenStr = '-' + t.tokenStr;
                // Arrays will be kept as identifiers until they need to be passed into something
                // Then we'll simply grab getVariableValue and pass into built in function
                // So, if setClassification does nothing, put the identifier on the stack
                if(t.primClassif == Classif.EMPTY && t.dclType == SubClassif.EMPTY)
                    t = scan.currentToken;
            }

            switch(t.primClassif) {
                case FUNCTION:
                    stack.push(t);
                    break;
                case OPERAND:
                    out.push(t);
                    break;
                case OPERATOR:
                    while(!stack.isEmpty()) {
                        if(t.preced() > stack.peek().stkPreced())
                            break;
                        Token popped = stack.pop();
                        out.push(popped);
                    }
                    stack.push(t);
                    break;
                case SEPARATOR:
                    switch(t.tokenStr) {
                        case "(":
                            stack.push(t);
                            break;
                        case ")":
                            foundLParen = false;
                            while(!stack.isEmpty()) {
                                Token popped = stack.pop();
                                if(popped.tokenStr.equals("(")) {
                                    foundLParen = true;
                                    break;
                                }
                                out.push(popped);
                            }
                            if(!stack.isEmpty() && stack.peek().primClassif == Classif.FUNCTION)
                                out.push(stack.pop());
                            if(!foundLParen)
                                break infix_to_postfix_loop;
                            break;
                        default:
                            error("Token %s, a separator, must either be a '(' or ')'", t.tokenStr);
                    }
                    break;
                default:
                    errorWithCurrent("Couldn't classify %s to add to the stack", t.tokenStr);
            }
            scan.getNext();
        }

        while(!stack.isEmpty())
            out.push(stack.pop());
        try {
            return getOperand(out);
        } catch(EmptyStackException e) {
            errorWithRange("Interpreter error: Expression ", " had an empty stack in postfix evaluation " +
                    "when trying to grab an operand");
            return null;
        }
    }

    private ResultValue getOperand(Stack<Token> out) throws Exception {
        String operation = null;
        ResultValue operand1 = null;
        ResultValue operand2 = null;
        //if(out.isEmpty())
        //    error("getOperand was called to evaluate postfix, but out was empty");
        if(out.peek().primClassif == Classif.OPERAND)
            return tokenToResultValue(out.pop());
        else if(out.peek().primClassif == Classif.OPERATOR) {
            operation = out.pop().tokenStr;
            if(operation.equals("not")) {
                operand1 = getOperand(out);
                return operand1.executeOperation(null, "not");
            }
            operand1 = getOperand(out);
            operand2 = getOperand(out);
            return operand2.executeOperation(operand1, operation);
        } else if(out.peek().primClassif == Classif.FUNCTION) {
            // TODO: This is only for built in functions right now
            Token funcToken = out.pop();
            STEntry stEntry = SymbolTable.globalSymbolTable.getSymbol(funcToken.tokenStr);
            if(stEntry == null)
                error("Function %s was not found in the symbol table", funcToken.tokenStr);
            if(!(stEntry instanceof STFunction))
                error(funcToken.tokenStr + " was found in the symbol table, but is not a function");
            STFunction stFunction = (STFunction) stEntry;
            ArrayList<ResultValue> parms = new ArrayList<>();
            for(int i = 0; i < stFunction.numArgs; i++) {
                ResultValue rv = getOperand(out);
                if(rv.value instanceof StringBuilder)
                    rv.value = ((StringBuilder) rv.value).toString(); // TODO: Quick fix. Make sure we're not getting StringBuilder from getOperand in the future
                parms.add(rv);
            }
            return callFunction(stFunction, parms);
        } else {
            error("Token %s cannot be evaluated in an expression", out.pop().tokenStr);
            return new ResultValue(SubClassif.EMPTY, "");
        }
    }

    public ResultValue callFunction(STFunction stFunction, ArrayList<ResultValue> parms) throws Exception {
        if(stFunction.numArgs != -1 && stFunction.numArgs != parms.size())
            error("Function %s requires %d arguments, but you entered %d", stFunction.tokenStr, stFunction.numArgs, parms.size());
            ResultValue arrRV;
            PickleArray arr;
            ResultValue strRV;
            String str;
                switch(stFunction.tokenStr) {
                    case "ELEM":
                        // parms will be a PickleArray
                        arrRV = parms.get(0);
                        if(!isArray(arrRV.iDatatype))
                            error("Function ELEM only takes in arrays.");
                        arr = ((PickleArray) arrRV.value);
                        return funcELEM(arr);
                    case "MAXELEM":
                        // parms will be a PickleArray
                        arrRV = parms.get(0);
                        if(!isArray(arrRV.iDatatype))
                            error("Function MAXELEM only takes in arrays.");
                        arr = ((PickleArray) arrRV.value);
                        return funcMAXELEM(arr);
                    case "LENGTH":
                        // parms will be a string
                        strRV = parms.get(0);
                        // Convert operand to string
                        if(strRV.iDatatype != SubClassif.STRING) {
                            strRV = new ResultValue(SubClassif.STRING, strRV.toString());
                        }
                        str = ((String)strRV.value);
                        return funcLENGTH(str);
                    case "SPACES":
                        // parms will be a string
                        strRV = parms.get(0);
                        if(strRV.iDatatype != SubClassif.STRING)
                            error("Function SPACES only takes in one string.");
                        str = ((String)strRV.value);
                        return funcSPACES(str);
                    default:
                        error("STFunction was passed in, but %s is not a supported function for callFunction, " +
                                "used in expr()'s eval of postfix", stFunction.tokenStr);
                        return new ResultValue(SubClassif.EMPTY, "");
        }
    }

    public ResultValue funcELEM(PickleArray arr) throws Exception {
        return arr.getElem();
    }

    public ResultValue funcMAXELEM(PickleArray arr) throws Exception {
        return arr.getMaxElem();
    }

    public ResultValue funcLENGTH(String str) throws Exception {
        return new ResultValue(SubClassif.INTEGER, new Numeric(String.valueOf(str.length()), SubClassif.INTEGER));
    }

    public ResultValue funcSPACES(String str) throws Exception {
        return new ResultValue(SubClassif.BOOLEAN, str.contains(" ") || str.isEmpty());
    }

    private boolean continuesExpr(Token t) {
        return t.primClassif == Classif.OPERATOR
                || t.primClassif == Classif.OPERAND
                || t.tokenStr.equals("(")
                || t.tokenStr.equals(")")
                || t.primClassif == Classif.FUNCTION;
    }

    private ResultValue tokenToResultValue(Token t) throws Exception {
        switch(t.dclType) {
            case INTEGER:
            case FLOAT:
                return new ResultValue(t.dclType, new Numeric(t.tokenStr, t.dclType));
            case BOOLEAN:
                return new ResultValue(SubClassif.BOOLEAN, t.tokenStr.equals("T"));
            case STRING:
                return new ResultValue(SubClassif.STRING, t.tokenStr);
            case IDENTIFIER:
                return getVariableValue(t.tokenStr);
        }
        error("Token %s cannot be converted to a ResultValue", t.tokenStr);
        return new ResultValue(SubClassif.EMPTY, "");
    }

    ResultValue evalCond(boolean bExecFunc, String flowType) throws Exception {
        ResultValue expr = expr(bExecFunc);
        if(expr.iDatatype != SubClassif.BOOLEAN)
            error("%s condition must yield a Bool", flowType);
        return expr;
    }

    private boolean isArray(STEntry stEntry) {
        return isArray(stEntry.dclType);
    }

    private boolean isArray(SubClassif dclType) {
        return dclType == SubClassif.INTEGERARR
                || dclType == SubClassif.FLOATARR
                || dclType == SubClassif.BOOLEANARR
                || dclType == SubClassif.STRINGARR;
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
                        error("Token's Classif is OPERAND and SubClassif is BOOLEAN but " +
                                "tokenStr " + scan.currentToken.tokenStr + " could not be resolved " +
                                "to a boolean value");
                    }
                    break;
                case STRING:
                    res = new ResultValue(SubClassif.STRING, scan.currentToken.tokenStr);
                    break;
                default:
                    error("operand is of unhandled type");
            }
        }

        return res;
    }

    public void saveLocationForRange() {
        savedRangeStartLine = scan.iSourceLineNr;
        savedRangeStartCol = scan.iColPos;
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
     * Prints tokens through a range
     * Use savedLocationForRange() to save, tokens before currentToken will be printed
     */
    public void errorWithRange(String before, String after) throws Exception {
        StringBuilder sb = new StringBuilder();

        int iEndLine = scan.iSourceLineNr;
        int iEndCol = scan.iColPos - 1; // To not print currentToken
        scan.goTo(savedRangeStartLine, savedRangeStartCol);
        while(scan.iSourceLineNr <= iEndLine && scan.iColPos <= iEndCol) {
            sb.append(scan.currentToken.tokenStr);
            scan.getNext();
        }
        error("%s%s%s", before, sb.toString(), after);
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
