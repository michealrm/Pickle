package pickle;

public class Numeric {

    public SubClassif type;
    public int intValue;
    public float floatValue;
    public String stringValue;

    private static class NotNumericException extends Exception {
        public NotNumericException(String tokenStr) {
            super(tokenStr);
        }
    }

    public Numeric(String tokenStr) throws NotNumericException {
        if(PickleUtil.isFloat(tokenStr)) {
            type = SubClassif.FLOAT;
            floatValue = Float.parseFloat(tokenStr);
        } else if(PickleUtil.isInt(tokenStr)) {
            type = SubClassif.INTEGER;
            intValue = Integer.parseInt(tokenStr);
        } else {
            throw new NotNumericException(tokenStr);
        }
    }
    public Numeric(Parser parser, ResultValue value, String operation, String paramNum) {

    }

    public Numeric add(Numeric num) {
        return null;
    }

    public Numeric subtract(Numeric num) {
        return null;
    }

    public Numeric multiply(Numeric num) {
        return null;
    }

    public Numeric divide(Numeric num) {
        return null;
    }



}
