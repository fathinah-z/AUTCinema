package cinemaapp.service;

import cinemaapp.model.AttendeeType;

public class PricingService {

    public double calculateItemPrice(AttendeeType attendeeType, double showPrice) {
        return showPrice * attendeeType.getPriceModifier();
    }
}
