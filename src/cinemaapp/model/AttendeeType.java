package cinemaapp.model;

public enum AttendeeType {
    ADULT(1.0),
    CHILD(0.6),
    STUDENT(0.75),
    SENIOR(0.7);

    private final double priceModifier;

    AttendeeType(double priceModifier) {
        this.priceModifier = priceModifier;
    }

    public double getPriceModifier() {
        return priceModifier;
    }
}
