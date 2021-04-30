package pickle;

public class PickleUtil {
    public static boolean isInt(String tokenStr) {
        boolean numeric = true;
        for(int i = 0; i < tokenStr.length(); i++) {
            char c = tokenStr.charAt(i);

            if(i == 0 && c == '-')
                continue;

            if (!Character.isDigit(c))
                numeric = false;
        }
        return numeric;
    }

    public static boolean isFloat(String tokenStr) {
        boolean numeric = true;
        for(int i = 0; i < tokenStr.length(); i++) {
            char c = tokenStr.charAt(i);

            if(i == 0 && c == '-')
                continue;

            if (!Character.isDigit(c) && c != '.')
                numeric = false;
        }

        return numeric;
    }
}
