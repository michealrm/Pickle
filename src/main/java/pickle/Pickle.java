/*
  This is a simple driver for the first programming assignment.
  Command Arguments:
      java Pickle arg1
             arg1 is the pickle source file name.
  Output:
      Prints each token in a table.
  Notes:
      1. This creates a SymbolTable object which doesn't do anything
         for this first programming assignment.
      2. This uses the student's Scanner class to get each token from
         the input file.  It uses the getNext method until it returns
         an empty string.
      3. If the Scanner raises an exception, this driver prints 
         information about the exception and terminates.
      4. The token is printed using the Token::printToken() method.
 */
package pickle;

import pickle.st.SymbolTable;
import pickle.Scanner;

import java.util.ArrayList;
import java.util.Stack;

public class Pickle
{
    public static void main(String[] args) throws Exception {
        //System.out.println("run:");

        // Create the Global SymbolTable
        SymbolTable.initGlobal();

        SymbolTable firstSymbolTable = new SymbolTable();

        // Create the SymbolTable
        final Stack<SymbolTable> symbolTable = new Stack<SymbolTable>();

        symbolTable.push(firstSymbolTable);

        // Create the Scanner
        Scanner scan = new Scanner(args[0], symbolTable);

        try {
            // Create the Parser
            Parser parser = new Parser(scan);

            // Execute statements in the source file
            parser.executeStatements(Status.EXECUTE);
        } catch(Exception e) {
            e.printStackTrace();
        }

        /* Scanner printing. TODO: Maybe have this print for a debug flag?
        try
        {
            // Print a column heading 
            System.out.printf("%-11s %-12s %s\n"
                    , "primClassif"
                    , "subClassif"
                    , "tokenStr");
            
            Scanner scan = new Scanner(args[0], symbolTable);
            while (! scan.getNext().isEmpty())
            {
                scan.currentToken.printToken();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        */
    }
}
