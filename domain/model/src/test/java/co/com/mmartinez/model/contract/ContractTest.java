package co.com.mmartinez.model.contract;

import co.com.mmartinez.model.common.BusinessException;
import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.common.ErrorCode;
import co.com.mmartinez.model.common.ValidityPeriod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractTest {

    private static final ValidityPeriod FIRST_HALF =
            new ValidityPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

    private static Contract newContract() {
        return Contract.create("C-1001", Country.CO, FIRST_HALF, List.of(
                new ContractItem("cem-50kg", new BigDecimal("10000"), new BigDecimal("32000")),
                new ContractItem("CEM-GRANEL", new BigDecimal("500"), new BigDecimal("610000"))));
    }

    private static Contract activeContract() {
        Contract contract = newContract();
        contract.approve();
        return contract;
    }

    @Test
    void newContractShouldBePendingApprovalWithNormalizedMaterials() {
        Contract contract = newContract();

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.PENDING_APPROVAL);
        assertThat(contract.getItems()).extracting(ContractItem::materialCode)
                .containsExactly("CEM-50KG", "CEM-GRANEL");
    }

    @Test
    void shouldRejectContractWithoutItems() {
        assertThatThrownBy(() -> Contract.create("C-1001", Country.CO, FIRST_HALF, List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMPTY_CONTRACT);
    }

    @Test
    void shouldRejectDuplicatedMaterials() {
        List<ContractItem> items = List.of(
                new ContractItem("CEM-50KG", BigDecimal.ONE, BigDecimal.TEN),
                new ContractItem("cem-50kg", BigDecimal.ONE, BigDecimal.TEN));

        assertThatThrownBy(() -> Contract.create("C-1001", Country.CO, FIRST_HALF, items))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATED_MATERIAL);
    }

    @Test
    void approveShouldActivateContract() {
        Contract contract = activeContract();

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
    }

    @Test
    void addQuantityShouldSumToExistingQuantityWithoutApproval() {
        Contract contract = activeContract();

        contract.addQuantity("CEM-50KG", new BigDecimal("10000"));

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(contract.getItems().get(0).quantity()).isEqualByComparingTo("20000");
    }

    @Test
    void addQuantityShouldFailForUnknownMaterial() {
        Contract contract = activeContract();

        assertThatThrownBy(() -> contract.addQuantity("ARENA", BigDecimal.ONE))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_NOT_FOUND);
    }

    @Test
    void addQuantityShouldFailWhenContractIsNotActive() {
        Contract contract = newContract();

        assertThatThrownBy(() -> contract.addQuantity("CEM-50KG", BigDecimal.ONE))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void validityChangeShouldWaitForApprovalAndThenApply() {
        Contract contract = activeContract();
        LocalDate newEnd = LocalDate.of(2026, 12, 31);

        contract.requestValidityChange(newEnd);

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.PENDING_APPROVAL);
        assertThat(contract.getValidity().validTo()).isEqualTo(FIRST_HALF.validTo());

        contract.approve();

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(contract.getValidity().validTo()).isEqualTo(newEnd);
        assertThat(contract.getPendingValidTo()).isNull();
    }

    @Test
    void rejectingValidityChangeShouldKeepContractActiveWithPreviousValidity() {
        Contract contract = activeContract();
        contract.requestValidityChange(LocalDate.of(2026, 12, 31));

        contract.reject("Sin soporte del área comercial");

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(contract.getValidity()).isEqualTo(FIRST_HALF);
        assertThat(contract.getRejectionReason()).isEqualTo("Sin soporte del área comercial");
    }

    @Test
    void rejectingNewContractShouldMarkItAsRejected() {
        Contract contract = newContract();

        contract.reject("Cliente bloqueado");

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.REJECTED);
    }

    @Test
    void validityChangeShouldFailWhenNewEndIsBeforeStart() {
        Contract contract = activeContract();

        assertThatThrownBy(() -> contract.requestValidityChange(LocalDate.of(2025, 12, 31)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_VALIDITY);
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
    }

    @Test
    void cancelledContractCannotBeCancelledAgain() {
        Contract contract = activeContract();
        contract.cancel();

        assertThatThrownBy(contract::cancel)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void totalShouldSumAllItems() {
        Contract contract = newContract();

        // 10.000 x 32.000 + 500 x 610.000
        assertThat(contract.total()).isEqualByComparingTo("625000000");
    }
}
