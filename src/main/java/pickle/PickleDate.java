package pickle;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import pickle.exception.DateFormatException;

public class PickleDate {

    public LocalDate date;
    public String strDate;

    public static int diff(PickleDate pd1, PickleDate pd2) {
        return Days.daysBetween(pd1.date, pd2.date).getDays();
    }

    public static PickleDate adj(PickleDate pd, int by) throws Exception {
        PickleDate copy = pd.copy();
        copy.date = copy.date.plusDays(by);
        copy.strDate = String.format("%04d-%02d-%02d", copy.date.getYear(),  copy.date.getMonthOfYear(), copy.date.getDayOfMonth());
        return copy;
    }

    public PickleDate(String date) throws Exception {
        strDate = date;

        String[] split = date.split("-");
        int year, month, day;
        if(split.length != 3)
            throw new DateFormatException(date, "Format for a date must be YYYY-MM-DD");

        year = parseInt(split[0], "year");
        month = parseInt(split[1], "month");
        day = parseInt(split[2], "day");

        // TODO We could have our own exception handling, not LocalDate's
        this.date = new LocalDate(year, month, day);

    }

    public PickleDate(int year, int month, int day) {
        strDate = String.format("%d-%d-%d", year, month, day);
        date = new LocalDate(year, month, day);
    }

    public int parseInt(String partOfDate, String partName) throws Exception {
        int result;
        try {
            result = Integer.parseInt(partOfDate);
        } catch(NumberFormatException nfe) {
            throw new Exception(String.format("Error while parsing %s of a date: %s must evaluate to an integer"));
        }
        return result;
    }

    public PickleDate copy() throws Exception {
        return new PickleDate(this.strDate);
    }

    public String toString() {
        return strDate;
    }

}
