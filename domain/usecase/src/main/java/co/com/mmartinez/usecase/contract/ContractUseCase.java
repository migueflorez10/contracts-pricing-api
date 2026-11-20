package co.com.mmartinez.usecase.contract;

import co.com.mmartinez.model.common.BusinessException;
import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.common.ErrorCode;
import co.com.mmartinez.model.contract.Contract;
import co.com.mmartinez.model.contract.ContractStatus;
import co.com.mmartinez.model.contract.gateways.ContractRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Casos de uso del ciclo de vida de un contrato.
 */
public class ContractUseCase {

    private final ContractRepository contractRepository;

    public ContractUseCase(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    public Contract create(Contract newContract) {
        boolean overlaps = contractRepository
                .findByCustomerAndCountry(newContract.getCustomerCode(), newContract.getCountry())
                .stream()
                .filter(Contract::isOpen)
                .anyMatch(existing -> existing.getValidity().overlaps(newContract.getValidity()));
        if (overlaps) {
            throw new BusinessException(ErrorCode.CONTRACT_OVERLAP, newContract.getCustomerCode());
        }
        return contractRepository.save(newContract);
    }

    public Contract getById(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTRACT_NOT_FOUND, String.valueOf(id)));
    }

    public List<Contract> list(Country country, ContractStatus status) {
        return contractRepository.findAll(country, status);
    }

    public Contract approve(Long id) {
        Contract contract = getById(id);
        contract.approve();
        return contractRepository.save(contract);
    }

    public Contract reject(Long id, String reason) {
        Contract contract = getById(id);
        contract.reject(reason);
        return contractRepository.save(contract);
    }

    public Contract addQuantity(Long id, String materialCode, BigDecimal additionalQuantity) {
        Contract contract = getById(id);
        contract.addQuantity(materialCode, additionalQuantity);
        return contractRepository.save(contract);
    }

    public Contract requestValidityChange(Long id, LocalDate newValidTo) {
        Contract contract = getById(id);
        contract.requestValidityChange(newValidTo);
        return contractRepository.save(contract);
    }

    public Contract cancel(Long id) {
        Contract contract = getById(id);
        contract.cancel();
        return contractRepository.save(contract);
    }
}
