package co.com.mmartinez.model.contract.gateways;

import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.contract.Contract;
import co.com.mmartinez.model.contract.ContractStatus;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para la persistencia de contratos. El dominio define el contrato;
 * la infraestructura decide cómo implementarlo.
 */
public interface ContractRepository {

    Contract save(Contract contract);

    Optional<Contract> findById(Long id);

    /**
     * Filtros opcionales: un valor nulo significa "sin filtrar por ese campo".
     */
    List<Contract> findAll(Country country, ContractStatus status);

    List<Contract> findByCustomerAndCountry(String customerCode, Country country);
}
