package main.java.pickle.exception;

public class SyntaxExceptionHandler {

    /**
     * Scans token for syntax exceptions
     *
     * @param tokenStr Token to be scanned for exceptions
     */
    public static void tokenException(String tokenStr, int iLineNumber, int iColNumber) throws Exception {
        iLineNumber++;

        int decimal = 0;

        for (int i = 0; i < tokenStr.length(); i++) {
            char c = tokenStr.charAt(i);

            // Decimal FloatingPointException
            if (c == '.') {
                decimal++;
                if (decimal > 1)
                    throw new NumberFormatException(errorMsg(iLineNumber, iColNumber, "Float contains more than one decimal"));
            }

            char begin = tokenStr.charAt(0);
            char end = tokenStr.charAt(tokenStr.length() - 1);
            if ((begin == '"' || begin == '\'')) {
                // Illegal line ending in string literal
                if (tokenStr.contains("\n"))
                    throw new Exception(errorMsg(iLineNumber, iColNumber, "Line " + tokenStr + " Illegal line end in string literal"));

                // String literal not closed
                if (begin != end)
                    throw new Exception(errorMsg(iLineNumber, iColNumber, "String literal " + tokenStr + " was not closed"));
            }

        }
    }

    private static String errorMsg(int iLineNumber, int iColNumber, String msg) {
        return String.format("Line %d Col %d: %s", iLineNumber, iColNumber, msg);
    }

}
