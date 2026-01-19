package cod.model.enums;

public enum Priority {
    LOW, MEDIUM, HIGH, CRITICAL;

    /**
     Returns the next priority level or CRITICAL if already at the highest level.
     */
    public Priority next() {
        int nextOrdinal = this.ordinal() + 1;
        if (nextOrdinal >= values().length) {
            return CRITICAL;
        }
        return values()[nextOrdinal];
    }
}
