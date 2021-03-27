package pickle;

import pickle.exception.ParserException;
import pickle.exception.ScannerTokenFormatException;

import java.util.Stack;

public class Parser {

    public Scanner scan;
    private Stack<String> flowStack = new Stack<>();

    public Parser(Scanner scanner) throws Exception {
        scan = scanner;
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
            else if(scan.currentToken.tokenStr.equals("print")) {
                printStmt();
            }
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
     * @return The ResultValue wrapper
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

        scan.getNext();
        // Only instantiation, no assignment
        if(scan.currentToken.tokenStr.equals(";")) {
            switch(typeStr) {
                case "Int":
                    assign(variableStr, new ResultValue(SubClassif.INTEGER, 0));
                    break;
                case "Float":
                    assign(variableStr, new ResultValue(SubClassif.FLOAT, 0.0));
                    break;
                case "Bool":
                    assign(variableStr, new ResultValue(SubClassif.BOOLEAN, false));
                    break;
                case "String":
                    assign(variableStr, new ResultValue(SubClassif.STRING, ""));
                    break;
                default:
                    error("Unsupported type %s", typeStr);
            }
        }
        // Instantiation and assignment
        else if(scan.currentToken.tokenStr.equals("=")) {
            scan.getNext();
            assign(variableStr, expr());
        }
        // Error
        else {
            errorWithCurrent("Expected an assignment (ex: Float f = 1.0;) or only declaration (ex: Float f;)");
        }
        scan.getNext(); // Skip past ';' to next statement - This is where the next statement is obtained
        return null;
    }

    /**
     * Reads a assignment statement starting at an IDENTIFIER token
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
        ResultValue exprToAssign = expr();
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
            System.out.println(res.toString() + " assigned to variable " + scan.currentToken.tokenStr);
        }

        return res;
    }

    void ifStmt(boolean bExec) throws Exception {
        if(bExec) {
            scan.getNext(); // Skip past the "if" to the opening parenthesis of the condition expression
            ResultValue resCond = evalCond("if");
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
                resTemp = executeStatements(true);
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

            ResultValue resCond = evalCond("while");
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
                resCond = evalCond("while");
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

    private void printStmt() throws Exception {
        StringBuffer msg = new StringBuffer();
        if(!scan.getNext().tokenStr.equals("("))
            errorWithCurrent("Expected '(' for builtin function 'print'");
        do {
            scan.getNext();
            ResultValue msgPart = expr();
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
        } while(scan.currentToken.tokenStr.equals(","));

        if(!scan.currentToken.tokenStr.equals(")"))
            errorWithCurrent("Expected ')' closing after print parameter");
        if(!scan.getNext().tokenStr.equals(";"))
            errorWithCurrent("Expected ';' after print statement");
        scan.getNext(); // Skip past ';'
        System.out.println(msg.toString());
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
     *
     * @return The ResultValue of the expression
     */
    ResultValue expr() throws Exception {
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
                    unaryMinusOn = new Numeric(scan.currentToken.tokenStr);
                    expr = new ResultValue(SubClassif.INTEGER, unaryMinusOn.unaryMinus());
                    break;
                case FLOAT:
                    unaryMinusOn = new Numeric(scan.currentToken.tokenStr);
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
                resOperand1 = new ResultValue(SubClassif.INTEGER, new Numeric(scan.currentToken.tokenStr));
                break;
            case FLOAT:
                resOperand1 = new ResultValue(SubClassif.FLOAT, new Numeric(scan.currentToken.tokenStr));
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

        // Get second operand if there is one using nextToken lookahead
        ResultValue resOperand2 = null;
        switch(scan.nextToken.dclType) {
            case INTEGER:
                resOperand2 = new ResultValue(SubClassif.INTEGER, new Numeric(scan.nextToken.tokenStr));
                break;
            case FLOAT:
                resOperand2 = new ResultValue(SubClassif.FLOAT, new Numeric(scan.nextToken.tokenStr));
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
            System.out.println("\n>" + expr);
        }

        return expr;
    }



    ResultValue evalCond(String flowType) throws Exception {
        ResultValue expr = expr();
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

    private ResultValue assign(String variableName, ResultValue value) {
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
    }

    // Exceptions

    public void error(String fmt) throws Exception {
        throw new ParserException(scan.iSourceLineNr, fmt, scan.sourceFileNm);
    }

    public void error(String fmt, Object... varArgs) throws Exception
    {
        String diagnosticTxt = String.format(fmt, varArgs);
        throw new ParserException(scan.iSourceLineNr
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
