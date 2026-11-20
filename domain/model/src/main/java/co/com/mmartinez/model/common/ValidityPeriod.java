package co.com.mmartinez.model.common;

import java.time.LocalDate;

/**
 * Objeto de valor que representa una vigencia con fecha de inicio y fin, ambas inclusivas.
 */
public record ValidityPeriod(LocalDate validFrom, LocalDate validTo) {

    public ValidityPeriod {
        if (validFrom == null || validTo == null) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD, "validFrom y validTo");
        }
        if (validFrom.isAfter(validTo)) {
            throw new BusinessException(ErrorCode.INVALID_VALIDITY);
        }
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(validFrom) && !date.isAfter(validTo);
    }

    public boolean overlaps(ValidityPeriod other) {
        return !validFrom.isAfter(other.validTo) && !other.validFrom.isAfter(validTo);
    }

    public ValidityPeriod withValidTo(LocalDate newValidTo) {
        return new ValidityPeriod(validFrom, newValidTo);
    }
}
