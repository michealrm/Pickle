package pickle.exception;

public class ParserException extends Exception
{
    public int iLineNr;
    public int iColPos;
    public String diagnostic;
    public String sourceFileName;
    // constructor
    public ParserException(int iLineNr, int iColPos, String diagnostic, String sourceFileName)
    {
        this.iLineNr = iLineNr + 1;
        this.iColPos = iColPos;
        this.diagnostic = diagnostic;
        this.sourceFileName = sourceFileName;
    }
    // Exceptions are required to provide tosString()
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("Line ");
        sb.append(iLineNr);
        sb.append(" ");
        sb.append("Col ");
        sb.append(iColPos);
        sb.append(" ");
        sb.append(diagnostic);
        sb.append(", File: ");
        sb.append(sourceFileName);
        return sb.toString();
    }
}

