package pickle;

import pickle.exception.ParserException;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

/**
 * Micheal: executeStatements, print, testing
 * JCGV: while
 * Alex: debug
 *
 */
public class Parser {

    public Scanner scan;

    public Parser(Scanner scanner) {
        scan = scanner;
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
        ResultValue res = new ResultValue(SubClassif.EMPTY, "");
        Queue<String> flowQueue = new LinkedList<>();
        while(true) {
            if(scan.currentToken.tokenStr.equals("if") || scan.currentToken.tokenStr.equals("while"))
                flowQueue.add(scan.currentToken.tokenStr);

            ResultValue resTemp = executeStmt(bExec);
            if(resTemp.scTerminatingStr.equals("endif") || resTemp.scTerminatingStr.equals("endwhile")) {
                if(flowQueue.isEmpty()) {
                    // Either flow is in another executeStmt's flowQueue or this is an invalid/non-matching termination
                    res.scTerminatingStr = resTemp.scTerminatingStr;
                    scan.getNext(); // Skip past END
                    if(!scan.getNext().tokenStr.equals(";"))
                        errorWithCurrent("Expected a ';' after an %s", resTemp.scTerminatingStr);
                    scan.getNext(); // Skip past ';'
                    return res;
                }

                // scTerminatingStr's END token has a matching FLOW at the front of the queue
                if(("end" + flowQueue.peek()).equals(resTemp.scTerminatingStr)) {
                    if(!scan.getNext().tokenStr.equals(";"))
                        errorWithCurrent("Expected a ';' after an %s", resTemp.scTerminatingStr);

                    res.scTerminatingStr = resTemp.scTerminatingStr;
                    flowQueue.remove();
                    break;
                } else {
                    errorWithCurrent("Expected an flow END (e.g. endif) for %s", flowQueue.peek());
                }

            }
        }

        return res;
    }

    ResultValue executeStmt(boolean bExec) throws Exception {
        // Check for END
        if(scan.currentToken.dclType == SubClassif.END) {
            ResultValue res = new ResultValue(SubClassif.EMPTY, "");
            res.scTerminatingStr = scan.currentToken.tokenStr;
            return res;
        }

        // Check for FLOW token
        switch(scan.currentToken.tokenStr) {
            case "while":
                whileStmt(bExec);
                break;
            case "if":
                ifStmt(bExec);
                break;
        }
        if(bExec) {
            if (scan.currentToken.dclType == SubClassif.DECLARE) {
                declareStmt();
            }
            if (scan.currentToken.dclType == SubClassif.IDENTIFIER) {
                return assignmentStmt();
            }
        }

        if(!bExec)
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
        ResultValue res = null;
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
            res = expr();
        }
        // Error
        else {
            errorWithCurrent("Expected an assignment (ex: Float f = 1.0;) or only declaration (ex: Float f;)");
        }
        return res;
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

        if(scan.currentToken.primClassif != Classif.SEPARATOR)
            errorWithCurrent("Expected a SEPARATOR to terminate assignment");

        return res;
    }

    void ifStmt(boolean bExec) throws Exception {
        if(bExec) {
            scan.getNext(); // Skip past the "if" to the opening parenthesis of the condition expression
            ResultValue resCond = evalCond("if");
            if(Boolean.parseBoolean(String.valueOf(resCond.value))) {
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
            skipAfter(":");
            ResultValue resTemp = executeStatements(false);
            if(resTemp.scTerminatingStr.equals("else")) {
                if(!scan.getNext().tokenStr.equals(":"))
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
    ResultValue expr() throws Exception {
        // Only supports one operator until program 4

        /*
        // Skip ( for expressions wrapped in parenthesis like (1 + 5) or while (T) <--
        // TODO: An edge case may be a = ("hello"). Is it okay to support that?
        if(scan.currentToken.tokenStr.equals("(")) {
            scan.getNext();
            ResultValue innerExprValue = expr(); // TODO: Make sure that expr() will stop when it hits a separator
        }
         */

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
            default:
                errorWithCurrent("Expected a token that can be evaluated in an expression");
        }

        scan.getNext(); // Now we're on the operator

        // Expression is only one operand
        if(scan.currentToken.primClassif == Classif.SEPARATOR) {
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
                break;
            default:
                // We'll catch the error when we switch the operator
                // We need this ResultValue classification for the unary minus (separator follows minus)
                resOperand2 = new ResultValue(scan.nextToken.dclType, scan.nextToken.tokenStr);
        }

        String operator = scan.currentToken.tokenStr;
        ResultValue expr = resOperand1.executeOperation(resOperand2, operator); // Note: IDE lies, resOperand1 won't be
            // null (-> NPE) because default case in switch (where resOperand1 would be null) results in an Exception

        scan.getNext(); // On either 2nd operand or separator since max operands is 2
        if(!scan.isSeparator(scan.currentToken.tokenStr) || !scan.isSeparator(scan.nextToken.tokenStr))
            errorWithCurrent("Expected expression to end with a SEPARATOR (e.g. ';', ',')");
        skipAfter(Classif.SEPARATOR); // We'll change this to keep parsing until we hit a separator in program 4
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
