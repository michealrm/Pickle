package pickle.exception;

public class DateFormatException extends Exception {
    public DateFormatException(String date, String msg) {
        super(String.format("Error while trying to parse date \"%s\": %s", date, msg));
    }
}
