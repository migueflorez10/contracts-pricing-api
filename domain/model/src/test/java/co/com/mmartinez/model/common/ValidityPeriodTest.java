package co.com.mmartinez.model.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidityPeriodTest {

    private static final LocalDate JAN_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate JUN_30 = LocalDate.of(2026, 6, 30);

    @Test
    void shouldRejectStartAfterEnd() {
        assertThatThrownBy(() -> new ValidityPeriod(JUN_30, JAN_1))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_VALIDITY);
    }

    @Test
    void shouldIncludeBothEdges() {
        ValidityPeriod period = new ValidityPeriod(JAN_1, JUN_30);

        assertThat(period.contains(JAN_1)).isTrue();
        assertThat(period.contains(JUN_30)).isTrue();
        assertThat(period.contains(JUN_30.plusDays(1))).isFalse();
    }

    @Test
    void shouldDetectOverlapWhenPeriodsShareOneDay() {
        ValidityPeriod first = new ValidityPeriod(JAN_1, JUN_30);
        ValidityPeriod second = new ValidityPeriod(JUN_30, LocalDate.of(2026, 12, 31));

        assertThat(first.overlaps(second)).isTrue();
    }

    @Test
    void shouldNotOverlapWhenPeriodsAreConsecutive() {
        ValidityPeriod first = new ValidityPeriod(JAN_1, JUN_30);
        ValidityPeriod second = new ValidityPeriod(JUN_30.plusDays(1), LocalDate.of(2026, 12, 31));

        assertThat(first.overlaps(second)).isFalse();
    }
}
