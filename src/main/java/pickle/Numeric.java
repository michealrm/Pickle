package main.java.pickle;

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



}
