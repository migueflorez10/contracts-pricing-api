package co.com.mmartinez.model.pricecondition;

import co.com.mmartinez.model.common.BusinessException;
import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.common.ErrorCode;
import co.com.mmartinez.model.common.ValidityPeriod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceConditionTest {

    private static final ValidityPeriod YEAR_2026 =
            new ValidityPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

    @Test
    void discountCannotExceedOneHundredPercent() {
        assertThatThrownBy(() -> PriceCondition.create(ConditionType.DISCOUNT, Country.SV, null,
                "CEM-50KG", new BigDecimal("120"), YEAR_2026))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_AMOUNT);
    }

    @Test
    void amountMustBePositive() {
        assertThatThrownBy(() -> PriceCondition.create(ConditionType.PRICE, Country.SV, null,
                "CEM-50KG", BigDecimal.ZERO, YEAR_2026))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_AMOUNT);
    }

    @Test
    void currencyShouldComeFromCountry() {
        PriceCondition condition = PriceCondition.create(ConditionType.PRICE, Country.MX, null,
                "CEM-50KG", new BigDecimal("180"), YEAR_2026);

        assertThat(condition.currency()).isEqualTo("MXN");
    }

    @Test
    void generalConditionShouldApplyToAnyCustomer() {
        PriceCondition general = PriceCondition.create(ConditionType.PRICE, Country.CO, " ",
                "CEM-50KG", new BigDecimal("32000"), YEAR_2026);

        assertThat(general.isCustomerSpecific()).isFalse();
        assertThat(general.appliesTo("C-1001", LocalDate.of(2026, 5, 10))).isTrue();
    }

    @Test
    void customerConditionShouldOnlyApplyToThatCustomer() {
        PriceCondition specific = PriceCondition.create(ConditionType.PRICE, Country.CO, "C-1001",
                "CEM-50KG", new BigDecimal("30000"), YEAR_2026);

        assertThat(specific.appliesTo("C-1001", LocalDate.of(2026, 5, 10))).isTrue();
        assertThat(specific.appliesTo("C-2002", LocalDate.of(2026, 5, 10))).isFalse();
    }

    @Test
    void conditionsForDifferentCustomersShouldNotOverlap() {
        PriceCondition general = PriceCondition.create(ConditionType.PRICE, Country.CO, null,
                "CEM-50KG", new BigDecimal("32000"), YEAR_2026);
        PriceCondition specific = PriceCondition.create(ConditionType.PRICE, Country.CO, "C-1001",
                "CEM-50KG", new BigDecimal("30000"), YEAR_2026);

        assertThat(general.overlapsWith(specific)).isFalse();
    }

    @Test
    void sameKeyAndCrossingDatesShouldOverlap() {
        PriceCondition first = PriceCondition.create(ConditionType.DISCOUNT, Country.CO, "C-1001",
                "CEM-50KG", new BigDecimal("5"), YEAR_2026);
        PriceCondition second = PriceCondition.create(ConditionType.DISCOUNT, Country.CO, "C-1001",
                "cem-50kg", new BigDecimal("8"),
                new ValidityPeriod(LocalDate.of(2026, 12, 1), LocalDate.of(2027, 3, 31)));

        assertThat(first.overlapsWith(second)).isTrue();
    }
}
