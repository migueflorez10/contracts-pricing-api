package co.com.mmartinez.usecase.contract;

import co.com.mmartinez.model.common.BusinessException;
import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.common.ErrorCode;
import co.com.mmartinez.model.common.ValidityPeriod;
import co.com.mmartinez.model.contract.Contract;
import co.com.mmartinez.model.contract.ContractItem;
import co.com.mmartinez.model.contract.ContractStatus;
import co.com.mmartinez.model.contract.gateways.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractUseCaseTest {

    private static final ValidityPeriod FIRST_HALF =
            new ValidityPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

    @Mock
    private ContractRepository contractRepository;

    private ContractUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ContractUseCase(contractRepository);
    }

    private static List<ContractItem> items() {
        return List.of(new ContractItem("CEM-50KG", new BigDecimal("10000"), new BigDecimal("32000")));
    }

    private static Contract stored(Long id, ContractStatus status, ValidityPeriod validity) {
        return Contract.restore(id, "C-1001", Country.CO, validity, null, status, null, items());
    }

    @Test
    void createShouldSaveWhenThereIsNoOverlap() {
        Contract newContract = Contract.create("C-1001", Country.CO, FIRST_HALF, items());
        ValidityPeriod secondHalf = new ValidityPeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31));
        when(contractRepository.findByCustomerAndCountry("C-1001", Country.CO))
                .thenReturn(List.of(stored(1L, ContractStatus.ACTIVE, secondHalf)));
        when(contractRepository.save(newContract)).thenReturn(newContract);

        Contract result = useCase.create(newContract);

        assertThat(result.getStatus()).isEqualTo(ContractStatus.PENDING_APPROVAL);
        verify(contractRepository).save(newContract);
    }

    @Test
    void createShouldFailWhenCustomerHasAnOpenContractInTheSamePeriod() {
        Contract newContract = Contract.create("C-1001", Country.CO, FIRST_HALF, items());
        when(contractRepository.findByCustomerAndCountry("C-1001", Country.CO))
                .thenReturn(List.of(stored(1L, ContractStatus.ACTIVE, FIRST_HALF)));

        assertThatThrownBy(() -> useCase.create(newContract))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONTRACT_OVERLAP);
        verify(contractRepository, never()).save(any());
    }

    @Test
    void createShouldIgnoreCancelledContracts() {
        Contract newContract = Contract.create("C-1001", Country.CO, FIRST_HALF, items());
        when(contractRepository.findByCustomerAndCountry("C-1001", Country.CO))
                .thenReturn(List.of(stored(1L, ContractStatus.CANCELLED, FIRST_HALF)));
        when(contractRepository.save(newContract)).thenReturn(newContract);

        useCase.create(newContract);

        verify(contractRepository).save(newContract);
    }

    @Test
    void getByIdShouldFailWhenContractDoesNotExist() {
        when(contractRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getById(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONTRACT_NOT_FOUND);
    }

    @Test
    void approveShouldActivateAndSave() {
        Contract pending = stored(1L, ContractStatus.PENDING_APPROVAL, FIRST_HALF);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(contractRepository.save(pending)).thenReturn(pending);

        Contract result = useCase.approve(1L);

        assertThat(result.getStatus()).isEqualTo(ContractStatus.ACTIVE);
    }

    @Test
    void addQuantityShouldSumAndSave() {
        Contract active = stored(1L, ContractStatus.ACTIVE, FIRST_HALF);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(active));
        when(contractRepository.save(active)).thenReturn(active);

        Contract result = useCase.addQuantity(1L, "cem-50kg", new BigDecimal("2500"));

        assertThat(result.getItems().get(0).quantity()).isEqualByComparingTo("12500");
    }

    @Test
    void requestValidityChangeShouldLeaveContractPending() {
        Contract active = stored(1L, ContractStatus.ACTIVE, FIRST_HALF);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(active));
        when(contractRepository.save(active)).thenReturn(active);

        Contract result = useCase.requestValidityChange(1L, LocalDate.of(2026, 9, 30));

        assertThat(result.getStatus()).isEqualTo(ContractStatus.PENDING_APPROVAL);
        assertThat(result.getPendingValidTo()).isEqualTo(LocalDate.of(2026, 9, 30));
    }
}
