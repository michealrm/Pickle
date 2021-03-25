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

    ResultValue assignmentStmt() throws Exception {
        ResultValue res = null;
        if(scan.currentToken.dclType != SubClassif.IDENTIFIER)
            error("Expected a variable for the target of an assignment");
        String variableStr = scan.currentToken.tokenStr;

        scan.getNext();
        if(scan.currentToken.primClassif != Classif.OPERATOR)
            error("Expected assignment operator");

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
                nOp2 = new Numeric(this, resO2, "-=", "2nd operand"); // -= and 2nd Operand added for errors
                                                                            // parser for diagnostics like line number
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
            ResultValue resCond = evalCond();
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
            ResultValue resCond = evalCond();
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

    // Eval

    /**
     * Evaluates an expression and returns the ResultValue
     *
     * Note: currentToken must be at the start of an expression.
     * An expression can also be within parenthesis, like while () <--
     * @return The ResultValue of the expression
     */
    ResultValue expr() throws Exception {
        // Only supports one operator until program 4
        scan.getNext();


        return null;
    }

    ResultValue evalCond() {
        return null;
    }

    // Util
    private ResultValue getVariableValue(String variableStr) throws Exception {
        return new ResultValue(1, StorageMgr.getValue(variableStr)); // TODO: Temp, are we changing iType?
        // TODO: Also, should StorageMgr hold ResultValues or Objects?
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
        StorageMgr.putValue(variableName, value);
        return value;
    }

    // Exceptions

    private void error(String fmt) throws Exception {
        throw new ParserException(scan.iSourceLineNr, fmt, scan.sourceFileNm);
    }

    private void error(String fmt, Object... varArgs) throws Exception
    {
        String diagnosticTxt = String.format(fmt, varArgs);
        throw new ParserException(scan.iSourceLineNr
                , diagnosticTxt
                , scan.sourceFileNm);
    }

    private void errorWithCurrent(String fmt) {

    }




}
