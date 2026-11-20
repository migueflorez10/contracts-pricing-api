package co.com.mmartinez.usecase.pricecondition;

import co.com.mmartinez.model.common.BusinessException;
import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.common.ErrorCode;
import co.com.mmartinez.model.pricecondition.ConditionType;
import co.com.mmartinez.model.pricecondition.PriceCondition;
import co.com.mmartinez.model.pricecondition.PriceQuote;
import co.com.mmartinez.model.pricecondition.gateways.PriceConditionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Calcula el precio neto de un material para un cliente en una fecha.
 *
 * <p>Precio neto = precio base x (1 - descuento / 100). Para el precio y para el descuento,
 * la condición propia del cliente tiene prioridad sobre la condición general del país.
 */
public class NetPriceUseCase {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final PriceConditionRepository priceConditionRepository;

    public NetPriceUseCase(PriceConditionRepository priceConditionRepository) {
        this.priceConditionRepository = priceConditionRepository;
    }

    public PriceQuote calculate(Country country, String customerCode, String materialCode, LocalDate date) {
        if (country == null || materialCode == null || materialCode.isBlank() || date == null) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD, "country, materialCode y date");
        }
        String material = materialCode.trim().toUpperCase();
        String customer = (customerCode == null || customerCode.isBlank()) ? null : customerCode.trim();

        List<PriceCondition> applicable = priceConditionRepository.findValidOn(country, material, date)
                .stream()
                .filter(condition -> condition.appliesTo(customer, date))
                .toList();

        PriceCondition price = mostSpecific(applicable, ConditionType.PRICE)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICE_NOT_FOUND, material + " en " + country));
        Optional<PriceCondition> discount = mostSpecific(applicable, ConditionType.DISCOUNT);

        BigDecimal discountPercent = discount.map(PriceCondition::amount).orElse(BigDecimal.ZERO);
        BigDecimal factor = BigDecimal.ONE.subtract(discountPercent.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP));
        BigDecimal netPrice = price.amount().multiply(factor).setScale(2, RoundingMode.HALF_UP);

        return new PriceQuote(country, customer, material, date, country.getCurrency(),
                price.amount(), discountPercent, netPrice,
                price.id(), discount.map(PriceCondition::id).orElse(null));
    }

    private Optional<PriceCondition> mostSpecific(List<PriceCondition> conditions, ConditionType type) {
        return conditions.stream()
                .filter(condition -> condition.type() == type)
                .max(Comparator.comparing(PriceCondition::isCustomerSpecific));
    }
}
