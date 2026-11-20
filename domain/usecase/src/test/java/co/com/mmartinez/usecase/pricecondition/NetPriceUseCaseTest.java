package co.com.mmartinez.usecase.pricecondition;

import co.com.mmartinez.model.common.BusinessException;
import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.common.ErrorCode;
import co.com.mmartinez.model.common.ValidityPeriod;
import co.com.mmartinez.model.pricecondition.ConditionType;
import co.com.mmartinez.model.pricecondition.PriceCondition;
import co.com.mmartinez.model.pricecondition.PriceQuote;
import co.com.mmartinez.model.pricecondition.gateways.PriceConditionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NetPriceUseCaseTest {

    private static final LocalDate DATE = LocalDate.of(2026, 5, 15);
    private static final ValidityPeriod YEAR_2026 =
            new ValidityPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

    private static final PriceCondition GENERAL_PRICE = new PriceCondition(1L, ConditionType.PRICE, Country.SV,
            null, "CEM-50KG", new BigDecimal("9.80"), YEAR_2026);
    private static final PriceCondition CUSTOMER_PRICE = new PriceCondition(2L, ConditionType.PRICE, Country.SV,
            "C-1001", "CEM-50KG", new BigDecimal("9.50"), YEAR_2026);
    private static final PriceCondition GENERAL_DISCOUNT = new PriceCondition(3L, ConditionType.DISCOUNT,
            Country.SV, null, "CEM-50KG", new BigDecimal("2"), YEAR_2026);
    private static final PriceCondition CUSTOMER_DISCOUNT = new PriceCondition(4L, ConditionType.DISCOUNT,
            Country.SV, "C-1001", "CEM-50KG", new BigDecimal("5"), YEAR_2026);

    @Mock
    private PriceConditionRepository repository;

    private NetPriceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new NetPriceUseCase(repository);
    }

    @Test
    void customerConditionsShouldTakePriorityOverGeneralOnes() {
        when(repository.findValidOn(Country.SV, "CEM-50KG", DATE)).thenReturn(
                List.of(GENERAL_PRICE, CUSTOMER_PRICE, GENERAL_DISCOUNT, CUSTOMER_DISCOUNT));

        PriceQuote quote = useCase.calculate(Country.SV, "C-1001", "cem-50kg", DATE);

        // 9.50 x (1 - 0.05) = 9.025 -> 9.03
        assertThat(quote.basePrice()).isEqualByComparingTo("9.50");
        assertThat(quote.discountPercent()).isEqualByComparingTo("5");
        assertThat(quote.netPrice()).isEqualByComparingTo("9.03");
        assertThat(quote.currency()).isEqualTo("USD");
        assertThat(quote.priceConditionId()).isEqualTo(2L);
        assertThat(quote.discountConditionId()).isEqualTo(4L);
    }

    @Test
    void customerWithoutOwnConditionsShouldGetGeneralOnes() {
        when(repository.findValidOn(Country.SV, "CEM-50KG", DATE)).thenReturn(
                List.of(GENERAL_PRICE, CUSTOMER_PRICE, GENERAL_DISCOUNT, CUSTOMER_DISCOUNT));

        PriceQuote quote = useCase.calculate(Country.SV, "C-2002", "CEM-50KG", DATE);

        // 9.80 x (1 - 0.02) = 9.604 -> 9.60
        assertThat(quote.basePrice()).isEqualByComparingTo("9.80");
        assertThat(quote.netPrice()).isEqualByComparingTo("9.60");
        assertThat(quote.priceConditionId()).isEqualTo(1L);
    }

    @Test
    void withoutDiscountNetPriceShouldEqualBasePrice() {
        when(repository.findValidOn(Country.SV, "CEM-50KG", DATE)).thenReturn(List.of(GENERAL_PRICE));

        PriceQuote quote = useCase.calculate(Country.SV, null, "CEM-50KG", DATE);

        assertThat(quote.discountPercent()).isEqualByComparingTo("0");
        assertThat(quote.netPrice()).isEqualByComparingTo("9.80");
        assertThat(quote.discountConditionId()).isNull();
    }

    @Test
    void shouldFailWhenThereIsNoPrice() {
        when(repository.findValidOn(Country.SV, "CEM-50KG", DATE)).thenReturn(List.of(GENERAL_DISCOUNT));

        assertThatThrownBy(() -> useCase.calculate(Country.SV, "C-1001", "CEM-50KG", DATE))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PRICE_NOT_FOUND);
    }
}
