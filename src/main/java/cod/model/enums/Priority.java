package cod.model.enums;

public enum Priority {
    LOW, MEDIUM, HIGH, CRITICAL;

    public Priority next() {
        int nextOrdinal = this.ordinal() + 1;
        if (nextOrdinal >= values().length) return CRITICAL;
        return values()[nextOrdinal];
    }
}