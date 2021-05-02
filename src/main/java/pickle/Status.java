package pickle;

public enum Status {
    IGNORE_EXEC("IGNORE_EXEC"),
    EXECUTE("EXECUTE"),
    BREAK("BREAK"),
    CONTINUE("CONTINUE"),
    RETURN("RETURN");

    private String value;

    Status(String value) {
        this.value = value;
    }

    public String toString() {
        return value;
    }
}
