package co.com.mmartinez.model.pricecondition;

import co.com.mmartinez.model.common.Country;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resultado del cálculo del precio neto de un material para un cliente en una fecha.
 */
public record PriceQuote(Country country, String customerCode, String materialCode, LocalDate date,
                         String currency, BigDecimal basePrice, BigDecimal discountPercent,
                         BigDecimal netPrice, Long priceConditionId, Long discountConditionId) {
}
