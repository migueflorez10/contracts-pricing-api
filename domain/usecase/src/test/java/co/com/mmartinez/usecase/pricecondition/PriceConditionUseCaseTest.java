package co.com.mmartinez.usecase.pricecondition;

import co.com.mmartinez.model.common.BusinessException;
import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.common.ErrorCode;
import co.com.mmartinez.model.common.ValidityPeriod;
import co.com.mmartinez.model.pricecondition.ConditionType;
import co.com.mmartinez.model.pricecondition.PriceCondition;
import co.com.mmartinez.model.pricecondition.gateways.PriceConditionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceConditionUseCaseTest {

    private static final ValidityPeriod YEAR_2026 =
            new ValidityPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

    @Mock
    private PriceConditionRepository repository;

    private PriceConditionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PriceConditionUseCase(repository);
    }

    @Test
    void createShouldSaveWhenThereIsNoOverlap() {
        PriceCondition general = new PriceCondition(1L, ConditionType.PRICE, Country.CO, null,
                "CEM-50KG", new BigDecimal("32000"), YEAR_2026);
        PriceCondition newCondition = PriceCondition.create(ConditionType.PRICE, Country.CO, "C-1001",
                "CEM-50KG", new BigDecimal("30000"), YEAR_2026);
        when(repository.findByTypeCountryAndMaterial(ConditionType.PRICE, Country.CO, "CEM-50KG"))
                .thenReturn(List.of(general));
        when(repository.save(newCondition)).thenReturn(newCondition);

        useCase.create(newCondition);

        verify(repository).save(newCondition);
    }

    @Test
    void createShouldFailWhenConditionOverlaps() {
        PriceCondition existing = new PriceCondition(1L, ConditionType.PRICE, Country.CO, null,
                "CEM-50KG", new BigDecimal("32000"), YEAR_2026);
        PriceCondition newCondition = PriceCondition.create(ConditionType.PRICE, Country.CO, null,
                "CEM-50KG", new BigDecimal("33000"),
                new ValidityPeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31)));
        when(repository.findByTypeCountryAndMaterial(ConditionType.PRICE, Country.CO, "CEM-50KG"))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> useCase.create(newCondition))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PRICE_CONDITION_OVERLAP);
        verify(repository, never()).save(any());
    }
}
