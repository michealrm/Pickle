package pickle;

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

    public Numeric(String value) throws NotNumericException {
        convertToNumber(value);
    }

    public Numeric(String value, SubClassif iDataType) throws NotNumericException {
        stringValue = value;
        type = iDataType;

        convertToNumber(value);
    }


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
            return Math.pow(intValue, value.intValue);
        else
            return Math.pow(floatValue, value.floatValue);
    }

    public Object unaryMinus() {
        if(type == SubClassif.INTEGER)
            return -intValue;
        else
            return -floatValue;
    }

    public boolean notEqual(Numeric value) {
        if(type == SubClassif.INTEGER) {
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
        if(PickleUtil.isFloat(value)) {
            type = SubClassif.FLOAT;
            floatValue = Float.parseFloat(value);
        } else if(PickleUtil.isInt(value)) {
            type = SubClassif.INTEGER;
            intValue = Integer.parseInt(value);
        } else {
            throw new NotNumericException(value);
        }
    }
}
