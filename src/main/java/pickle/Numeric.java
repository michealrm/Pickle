package pickle;

import pickle.exception.InvalidNumberException;

public class Numeric {

    public SubClassif type;
    public Integer intValue;
    public Float floatValue;
    public String stringValue;

    private static class NotNumericException extends Exception {
        public NotNumericException(String tokenStr) {
            super(tokenStr);
        }
    }

    public Numeric(String value, SubClassif iDataType) throws NotNumericException {
        stringValue = value;
        type = iDataType;
        convertToNumber(value);
    }

    /*
     * Used in the Parser for  with the Parser, operation, and paramNum passed in for error handling
     * @param parser The parser used for token position in error handling
     * @param value The ResultValue that will convert to Numeric
     * @param operation The operation (ex: +=) used in error handling
     * @param paramNum The parameter index number (ex: 1st parameter) used in error handling
     */
    /*
    public Numeric(Parser parser, ResultValue value, String operation, String paramNum) throws Exception {
        if(value == null) {

        }
        switch(value.iDatatype) {
            case INTEGER:

                break;
            case FLOAT:
                break;
            default:
                throw new InvalidNumberException("Expected a number in the " + paramNum + " of the " + operation +
                        " operation, but read type " + value.iDatatype + " for token " + value.value);
        }
    }
    */

    /**
     * Receives an object and sets type, intValue or floatValue or stringValue
     * @param obj The object to set the instance variables
     * @throws Exception If
     */
    private void setNumericFromObject(Object obj) throws Exception {
    }

    // The type returned is based on the type of the left operand

    public Object add(Numeric value) {
        if(type == SubClassif.INTEGER)
            return intValue + value.intValue;
        else
            return floatValue + value.floatValue;
    }

    public Object subtract(Numeric value) {
        if(type == SubClassif.INTEGER)
            return intValue - value.intValue;
        else
            return floatValue - value.floatValue;
    }

    public Object multiply(Numeric value) {
        if(type == SubClassif.INTEGER)
            return intValue * value.intValue;
        else
            return floatValue * value.floatValue;
    }

    public Object divide(Numeric value) {
        if(type == SubClassif.INTEGER)
            return intValue / value.intValue;
        else
            return floatValue / value.floatValue;
    }

    public Object power(Numeric value) {
        if(type == SubClassif.INTEGER)
            return (int)Math.pow(intValue, value.intValue);
        else
            return Math.pow(floatValue, value.floatValue);
    }

    public Object unaryMinus() {
        if(type == SubClassif.INTEGER)
            return -intValue;
        else
            return -floatValue;
    }

    public boolean equal(Numeric value) {
        if (type == SubClassif.INTEGER) {
            return intValue.equals(value.intValue);
        } else {
            return floatValue.equals(value.floatValue);
        }
    }

    public boolean notEqual(Numeric value) {
        if (type == SubClassif.INTEGER) {
            return !intValue.equals(value.intValue);
        } else {
            return !floatValue.equals(value.floatValue);
        }
    }

    public boolean lessThan(Numeric value) {
        if(type == SubClassif.INTEGER) {
            return intValue < value.intValue;
        } else {
            return floatValue < value.floatValue;
        }
    }

    public boolean lessThanEqualTo(Numeric value) {
        if(type == SubClassif.INTEGER) {
            return intValue <= value.intValue;
        } else {
            return floatValue <= value.floatValue;
        }
    }

    public boolean greaterThan(Numeric value) {
        if(type == SubClassif.INTEGER) {
            return intValue > value.intValue;
        } else {
            return floatValue > value.floatValue;
        }
    }

    public boolean greaterThanEqualTo(Numeric value) {
        if(type == SubClassif.INTEGER) {
            return intValue >= value.intValue;
        } else {
            return floatValue >= value.floatValue;
        }
    }

    private void convertToNumber(String value) throws NotNumericException {
        if(type == SubClassif.INTEGER) {
            intValue = Integer.parseInt(value);
            floatValue = intValue.floatValue();
        }
        else if(type == SubClassif.FLOAT) {
            floatValue = Float.parseFloat(value);   // Have both float and int values, since we don't know what type the left operand is
            intValue = floatValue.intValue();
        } else {
            throw new NotNumericException(value);
        }
    }

    public String toString() {
        String str;
        if(type == SubClassif.INTEGER)
            str = String.valueOf(intValue);
        else
            str = String.valueOf(floatValue);
        return str;
    }
}
