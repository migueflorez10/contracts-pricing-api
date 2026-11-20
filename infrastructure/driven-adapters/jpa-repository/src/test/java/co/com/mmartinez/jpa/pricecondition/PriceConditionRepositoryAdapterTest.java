package co.com.mmartinez.jpa.pricecondition;

import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.common.ValidityPeriod;
import co.com.mmartinez.model.pricecondition.ConditionType;
import co.com.mmartinez.model.pricecondition.PriceCondition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(PriceConditionRepositoryAdapter.class)
class PriceConditionRepositoryAdapterTest {

    private static final ValidityPeriod FIRST_HALF =
            new ValidityPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
    private static final ValidityPeriod SECOND_HALF =
            new ValidityPeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31));

    @Autowired
    private PriceConditionRepositoryAdapter adapter;

    @Test
    void shouldSaveGeneralAndCustomerConditions() {
        PriceCondition general = adapter.save(PriceCondition.create(ConditionType.PRICE, Country.SV, null,
                "CEM-50KG", new BigDecimal("9.80"), FIRST_HALF));
        PriceCondition specific = adapter.save(PriceCondition.create(ConditionType.PRICE, Country.SV, "C-1001",
                "CEM-50KG", new BigDecimal("9.50"), FIRST_HALF));

        assertThat(adapter.findById(general.id())).get()
                .extracting(PriceCondition::customerCode).isNull();
        assertThat(adapter.findById(specific.id())).get()
                .extracting(PriceCondition::customerCode).isEqualTo("C-1001");
    }

    @Test
    void findValidOnShouldReturnOnlyConditionsValidOnThatDate() {
        adapter.save(PriceCondition.create(ConditionType.PRICE, Country.SV, null,
                "CEM-50KG", new BigDecimal("9.80"), FIRST_HALF));
        adapter.save(PriceCondition.create(ConditionType.PRICE, Country.SV, null,
                "CEM-50KG", new BigDecimal("10.10"), SECOND_HALF));

        assertThat(adapter.findValidOn(Country.SV, "CEM-50KG", LocalDate.of(2026, 8, 15)))
                .extracting(PriceCondition::amount)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("10.10"));
    }

    @Test
    void findAllShouldApplyOptionalFilters() {
        adapter.save(PriceCondition.create(ConditionType.PRICE, Country.SV, null,
                "CEM-50KG", new BigDecimal("9.80"), FIRST_HALF));
        adapter.save(PriceCondition.create(ConditionType.DISCOUNT, Country.SV, null,
                "CEM-50KG", new BigDecimal("2"), FIRST_HALF));
        adapter.save(PriceCondition.create(ConditionType.PRICE, Country.GT, null,
                "CEM-50KG", new BigDecimal("75"), FIRST_HALF));

        assertThat(adapter.findAll(Country.SV, null, null)).hasSize(2);
        assertThat(adapter.findAll(null, ConditionType.PRICE, "CEM-50KG")).hasSize(2);
        assertThat(adapter.findByTypeCountryAndMaterial(ConditionType.DISCOUNT, Country.SV, "CEM-50KG"))
                .hasSize(1);
    }
}
