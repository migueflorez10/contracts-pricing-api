package co.com.mmartinez.model.pricecondition;

import co.com.mmartinez.model.common.BusinessException;
import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.common.ErrorCode;
import co.com.mmartinez.model.common.ValidityPeriod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Condición de precio de un material en un país. Si tiene cliente, aplica solo a ese cliente;
 * si no, es la condición general del país y la usan todos los clientes sin una condición propia.
 */
public record PriceCondition(Long id, ConditionType type, Country country, String customerCode,
                             String materialCode, BigDecimal amount, ValidityPeriod validity) {

    private static final BigDecimal MAX_DISCOUNT = BigDecimal.valueOf(100);

    public PriceCondition {
        if (type == null) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD, "type");
        }
        if (country == null) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD, "country");
        }
        if (materialCode == null || materialCode.isBlank()) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD, "materialCode");
        }
        if (validity == null) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD, "validity");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, "debe ser mayor que cero");
        }
        if (type == ConditionType.DISCOUNT && amount.compareTo(MAX_DISCOUNT) > 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, "un descuento no puede superar el 100 %");
        }
        materialCode = materialCode.trim().toUpperCase();
        customerCode = (customerCode == null || customerCode.isBlank()) ? null : customerCode.trim();
    }

    public static PriceCondition create(ConditionType type, Country country, String customerCode,
                                        String materialCode, BigDecimal amount, ValidityPeriod validity) {
        return new PriceCondition(null, type, country, customerCode, materialCode, amount, validity);
    }

    public boolean isCustomerSpecific() {
        return customerCode != null;
    }

    public String currency() {
        return country.getCurrency();
    }

    /**
     * Indica si la condición aplica a un cliente en una fecha. Una condición general aplica a cualquier cliente.
     */
    public boolean appliesTo(String customer, LocalDate date) {
        boolean customerMatches = !isCustomerSpecific() || customerCode.equals(customer);
        return customerMatches && validity.contains(date);
    }

    /**
     * Dos condiciones chocan si son del mismo tipo, para el mismo país, material y cliente,
     * y sus vigencias se cruzan.
     */
    public boolean overlapsWith(PriceCondition other) {
        return type == other.type
                && country == other.country
                && materialCode.equals(other.materialCode)
                && Objects.equals(customerCode, other.customerCode)
                && validity.overlaps(other.validity);
    }
}
