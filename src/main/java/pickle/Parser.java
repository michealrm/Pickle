package pickle;

import pickle.exception.ParserException;

public class Parser {

    public Scanner scan;

    ////////////////
    // Statements //
    ////////////////

    ResultValue executeStatements(boolean bExec) {
        return null;
    }

    ResultValue executeStmt(boolean bExec) throws Exception {
        if(!bExec) {
            skipTo(";");
            scan.getNext();
            return new ResultValue(SubClassif.EMPTY, "");
        }
    }

    /**
     * Reads a declare statement starting at a DECLARE token, ending at the character after the semicolon
     * @return The ResultValue wrapper
     * @throws Exception
     */
    private ResultValue declareStmt() throws Exception {
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
        String variableStr = scan.currentToken.tokenStr;

        scan.getNext();
        if(scan.currentToken.primClassif != Classif.OPERATOR)
            error("Expected assignment operator for assignment statement");

        String operatorStr = scan.currentToken.tokenStr;
        ResultValue resO2 = null;
        ResultValue resO1 = null;
        Numeric nOp2;
        Numeric nOp1;
        switch(operatorStr) {
            case "=":
                resO2 = expr();
                res = assign(variableStr, resO2);
                break;
            case "-=":
                resO2 = expr();
                nOp2 = new Numeric(this, resO2, "-=", "2nd operand");
                //resO2 = getVariableValue(variableStr); // not used. Do we need to get the value? Can't we just assign?
                nOp1 = new Numeric(this, resO1, "-=", "1st operand");
                res = assign(variableStr, Utility.subtractToResult(this, nOp1, nOp2)); // parser for diagnostics like line number
                break;
            case "+=":
                resO2 = expr();

                nOp2 = new Numeric(this, resO2, "+=", "2nd operand"); // -= and 2nd Operand added for errors
                // parser for diagnostics like line number

                resO2 = getVariableValue(variableStr);

                nOp1 = new Numeric(this, resO1, "+=", "1st operand");

                res = assign(variableStr, Utility.addToResult(this, nOp1, nOp2)); // parser for diagnostics like line number
                break;
            default:
                error("Expected assignment operator");
        }
        return res;
    }

    void ifStmt(boolean bExec) throws Exception {
        if(bExec) {
            ResultValue resCond = evalCond("if");
            if(Boolean.parseBoolean(String.valueOf(resCond.value))) { // TODO: Another way to get resCond.value -> bool?
                ResultValue resTemp = executeStatements(true);
                if(resTemp.scTerminatingStr.equals("else")) {
                    if(!scan.getNext().tokenStr.equals(":"))
                        errorWithCurrent("Expected ':' after else");
                    resTemp = executeStatements(false);
                }
                if(!resTemp.scTerminatingStr.equals("endif"))
                    errorWithCurrent("Expected an 'endif' for an 'if'");
                if(!scan.getNext().tokenStr.equals(";"))
                    errorWithCurrent("Expected';' after and'endif'");
            }
            else {
                ResultValue resTemp = executeStatements(false);
                if(resTemp.scTerminatingStr.equals("else")) {
                    if(!scan.getNext().tokenStr.equals(":"))
                        errorWithCurrent("Expected ':' after 'else'");
                    resTemp = executeStatements(true);
                }
                if(!scan.getNext().tokenStr.equals(";"))
                    errorWithCurrent("Expected ';' after 'endif'");
            }
        } else {
            skipTo(":");
            ResultValue resTemp = executeStatements(false);
            if(resTemp.scTerminatingStr.equals("else")) {
                if(scan.getNext().tokenStr.equals(":"))
                    errorWithCurrent("Expected ':' after else");
                resTemp = executeStatements(false);
            }
            if(!scan.getNext().tokenStr.equals("endif"))
                errorWithCurrent("Expected an 'endif' for an 'if'");
            if(!scan.getNext().tokenStr.equals(";"))
                errorWithCurrent("Expected ';' after 'endif'");
        }
    }

    // TODO: NOT DONE, THIS IS FOR IF
    void whileStmt(boolean bExec) throws Exception {
        if(bExec) {
            ResultValue resCond = evalCond("while");
            if(Boolean.parseBoolean(String.valueOf(resCond.value))) { // TODO: Another way to get resCond.value -> bool?
                ResultValue resTemp = executeStatements(true);
                if(resTemp.scTerminatingStr.equals("else")) {
                    if(!scan.getNext().tokenStr.equals(":"))
                        errorWithCurrent("Expected ':' after else");
                    resTemp = executeStatements(false);
                }
                if(!resTemp.scTerminatingStr.equals("endif"))
                    errorWithCurrent("Expected an 'endif' for an 'if'");
                if(!scan.getNext().tokenStr.equals(";"))
                    errorWithCurrent("Expected';' after and'endif'");
            }
            else {
                ResultValue resTemp = executeStatements(false);
                if(resTemp.scTerminatingStr.equals("else")) {
                    if(!scan.getNext().tokenStr.equals(":"))
                        errorWithCurrent("Expected ':' after 'else'");
                    resTemp = executeStatements(true);
                }
                if(!scan.getNext().tokenStr.equals(";"))
                    errorWithCurrent("Expected ';' after 'endif'");
            }
        } else {
            skipTo(":");
            ResultValue resTemp = executeStatements(false);
            if(resTemp.scTerminatingStr.equals("else")) {
                if(scan.getNext().tokenStr.equals(":"))
                    errorWithCurrent("Expected ':' after else");
                resTemp = executeStatements(false);
            }
            if(!scan.getNext().tokenStr.equals("endif"))
                errorWithCurrent("Expected an 'endif' for an 'if'");
            if(!scan.getNext().tokenStr.equals(";"))
                errorWithCurrent("Expected ';' after 'endif'");
        }
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
     * @return The ResultValue of the expression
     */
    // TODO: Maybe later we could have expr() call assignmentStmt since that's an expression & so we could just
    //  call expr() if we hit an IDENTIFIER at the start of a statement. And I guess assignmentStmt would take
    //  in the variableName and equal, and then pass back to expr()
    ResultValue expr() throws Exception {
        // Only supports one operator until program 4

        // Skip ( for expressions wrapped in parenthesis like (1 + 5) or while (T) <--
        // TODO: An edge case may be a = ("hello"). Is it okay to support that?
        if(scan.currentToken.tokenStr.equals("("))
            scan.getNext();

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
                resOperand1 = new ResultValue(SubClassif.BOOLEAN, scan.currentToken.tokenStr);
                break;
            case IDENTIFIER:
                resOperand1 = StorageManager.retrieveVariable(scan.currentToken.tokenStr);
            default:
                errorWithCurrent("Expected a token that can be evaluated in an expression");
        }

        scan.getNext(); // Now we're on the operator

        // Expression is only one operand
        if(scan.currentToken.tokenStr.equals(";")) {
            scan.getNext();
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
            default:
                // We'll catch the error when we switch the operator
                // We need this ResultValue classification for the unary minus (separator follows minus)
                resOperand2 = new ResultValue(scan.nextToken.dclType, scan.nextToken.tokenStr);
        }

        // Operator, using lookahead above for the second operand to apply the operation
        ResultValue expr = null;
        Object resultObj;
        Numeric nOperand1;
        Numeric nOperand2;
        boolean bResult;
        switch(scan.currentToken.tokenStr) {
            // Numeric arithmetic

            // Add. Second operand is required
            case "+":
                nOperand1 = new Numeric(this, resOperand1, "+", "1st operand");
                nOperand2 = new Numeric(this, resOperand2, "+", "2st operand");
                resultObj = nOperand1.add(nOperand2);

                if(resultObj instanceof Integer) {
                    expr = new ResultValue(SubClassif.INTEGER, resultObj);
                } else if(resultObj instanceof Float) {
                    expr = new ResultValue(SubClassif.FLOAT, resultObj);
                }
                break;

            // Subtraction. Either unary minus or second operand is required
            case "-":
                nOperand1 = new Numeric(this, resOperand1, "-", "1st operand");

                // If we have a separator that follows a minus, then apply unary minus
                char c = String.valueOf(resOperand2.value).charAt(0);
                if(c == ',' || c == ';') {
                    resultObj = nOperand1.unaryMinus();
                }
                // We have 2 operands
                else {
                    nOperand2 = new Numeric(this, resOperand2, "-", "2nd operand");
                    resultObj = nOperand1.add(nOperand2);
                }

                if(resultObj instanceof Integer) {
                    expr = new ResultValue(SubClassif.INTEGER, resultObj);
                } else if(resultObj instanceof Float) {
                    expr = new ResultValue(SubClassif.FLOAT, resultObj);
                }
                break;

            // Multiplication
            case "*":
                nOperand1 = new Numeric(this, resOperand1, "*", "1st operand");
                nOperand2 = new Numeric(this, resOperand2, "*", "2nd operand");
                resultObj = nOperand1.multiply(nOperand2);

                if(resultObj instanceof Integer) {
                    expr = new ResultValue(SubClassif.INTEGER, resultObj);
                } else if(resultObj instanceof Float) {
                    expr = new ResultValue(SubClassif.FLOAT, resultObj);
                }
                break;

            // Division
            case "/":
                nOperand1 = new Numeric(this, resOperand1, "/", "1st operand");
                nOperand2 = new Numeric(this, resOperand2, "/", "2nd operand");
                resultObj = nOperand1.divide(nOperand2);

                if(resultObj instanceof Integer) {
                    expr = new ResultValue(SubClassif.INTEGER, resultObj);
                } else if(resultObj instanceof Float) {
                    expr = new ResultValue(SubClassif.FLOAT, resultObj);
                }
                break;

            // Boolean equality operators
            case "==":
                expr = new ResultValue(SubClassif.BOOLEAN, resOperand1.equals(resOperand2));
                break;
            case "!=":
                expr = new ResultValue(SubClassif.BOOLEAN, !resOperand1.equals(resOperand2));
                break;

            // Boolean comparison operators (Numeric operands) TODO: Numeric operands for now (String, Date?)
            case "<=":
                nOperand1 = new Numeric(this, resOperand1, "<=", "1st operand");
                nOperand2 = new Numeric(this, resOperand2, "<=", "2nd operand");
                bResult = nOperand1.lessThanEqualTo(nOperand2);

                expr = new ResultValue(SubClassif.BOOLEAN, bResult);
                break;
            case ">=":
                nOperand1 = new Numeric(this, resOperand1, ">=", "1st operand");
                nOperand2 = new Numeric(this, resOperand2, ">=", "2nd operand");
                bResult = nOperand1.greaterThanEqualTo(nOperand2);

                expr = new ResultValue(SubClassif.BOOLEAN, bResult);
                break;
            case "<":
                nOperand1 = new Numeric(this, resOperand1, "<", "1st operand");
                nOperand2 = new Numeric(this, resOperand2, "<", "2nd operand");
                bResult = nOperand1.lessThan(nOperand2);

                expr = new ResultValue(SubClassif.BOOLEAN, bResult);
                break;
            case ">":
                nOperand1 = new Numeric(this, resOperand1, ">", "1st operand");
                nOperand2 = new Numeric(this, resOperand2, ">", "2nd operand");
                bResult = nOperand1.greaterThan(nOperand2);

                expr = new ResultValue(SubClassif.BOOLEAN, bResult);
                break;

            // I don't know what this does
            case "^":
                break;

            // Operator not found
            default:
                errorWithCurrent("Expected an operator for the expression");
        }

        skipTo(";");
        scan.getNext();
        return expr;
    }

    ResultValue evalCond(String flowType) throws Exception {
        ResultValue expr = expr();
        if(expr.iDatatype != SubClassif.BOOLEAN)
            error("%s condition must yield a Bool", flowType);
        return expr;
    }

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

    private ResultValue assign(String variableName, ResultValue value) {
        StorageManager.storeVariable(variableName, value);
        return value;
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
        error("Read %s, " + fmt, scan.currentToken.tokenStr);
    }

    public void errorWithCurrent(String fmt, Object... varArgs) throws Exception {
        error("Read %s, " + fmt, scan.currentToken.tokenStr, varArgs);
    }


}
