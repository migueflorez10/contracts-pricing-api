package co.com.mmartinez.jpa.contract;

import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.common.ValidityPeriod;
import co.com.mmartinez.model.contract.Contract;
import co.com.mmartinez.model.contract.ContractItem;
import co.com.mmartinez.model.contract.ContractStatus;
import co.com.mmartinez.model.contract.gateways.ContractRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementación del puerto {@link ContractRepository} con Spring Data JPA.
 * Traduce entre el modelo de dominio y las entidades de base de datos.
 */
@Repository
public class ContractRepositoryAdapter implements ContractRepository {

    private final ContractDataRepository dataRepository;

    public ContractRepositoryAdapter(ContractDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public Contract save(Contract contract) {
        ContractEntity entity = contract.getId() == null
                ? new ContractEntity()
                : dataRepository.findById(contract.getId()).orElseGet(ContractEntity::new);
        copyToEntity(contract, entity);
        return toDomain(dataRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Contract> findById(Long id) {
        return dataRepository.findById(id).map(ContractRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Contract> findAll(Country country, ContractStatus status) {
        Specification<ContractEntity> spec = (root, query, cb) -> cb.conjunction();
        if (country != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("country"), country));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return dataRepository.findAll(spec, Sort.by("id")).stream()
                .map(ContractRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Contract> findByCustomerAndCountry(String customerCode, Country country) {
        return dataRepository.findByCustomerCodeAndCountry(customerCode, country).stream()
                .map(ContractRepositoryAdapter::toDomain)
                .toList();
    }

    /**
     * Actualiza las posiciones en sitio (por código de material) en lugar de borrarlas y crearlas de nuevo,
     * para conservar sus identificadores.
     */
    private static void copyToEntity(Contract contract, ContractEntity entity) {
        entity.setCustomerCode(contract.getCustomerCode());
        entity.setCountry(contract.getCountry());
        entity.setValidFrom(contract.getValidity().validFrom());
        entity.setValidTo(contract.getValidity().validTo());
        entity.setPendingValidTo(contract.getPendingValidTo());
        entity.setStatus(contract.getStatus());
        entity.setRejectionReason(contract.getRejectionReason());

        Map<String, ContractItemEntity> current = entity.getItems().stream()
                .collect(Collectors.toMap(ContractItemEntity::getMaterialCode, Function.identity()));
        Set<String> materials = contract.getItems().stream()
                .map(ContractItem::materialCode)
                .collect(Collectors.toSet());
        entity.getItems().removeIf(item -> !materials.contains(item.getMaterialCode()));

        for (ContractItem item : contract.getItems()) {
            ContractItemEntity itemEntity = current.get(item.materialCode());
            if (itemEntity == null) {
                itemEntity = new ContractItemEntity();
                itemEntity.setMaterialCode(item.materialCode());
                entity.addItem(itemEntity);
            }
            itemEntity.setQuantity(item.quantity());
            itemEntity.setUnitPrice(item.unitPrice());
        }
    }

    static Contract toDomain(ContractEntity entity) {
        List<ContractItem> items = entity.getItems().stream()
                .map(item -> new ContractItem(item.getMaterialCode(), item.getQuantity(), item.getUnitPrice()))
                .toList();
        return Contract.restore(
                entity.getId(),
                entity.getCustomerCode(),
                entity.getCountry(),
                new ValidityPeriod(entity.getValidFrom(), entity.getValidTo()),
                entity.getPendingValidTo(),
                entity.getStatus(),
                entity.getRejectionReason(),
                items);
    }
}
