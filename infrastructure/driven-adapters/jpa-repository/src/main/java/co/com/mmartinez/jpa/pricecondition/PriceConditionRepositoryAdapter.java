package co.com.mmartinez.jpa.pricecondition;

import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.common.ValidityPeriod;
import co.com.mmartinez.model.pricecondition.ConditionType;
import co.com.mmartinez.model.pricecondition.PriceCondition;
import co.com.mmartinez.model.pricecondition.gateways.PriceConditionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class PriceConditionRepositoryAdapter implements PriceConditionRepository {

    private final PriceConditionDataRepository dataRepository;

    public PriceConditionRepositoryAdapter(PriceConditionDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public PriceCondition save(PriceCondition condition) {
        PriceConditionEntity entity = new PriceConditionEntity();
        entity.setType(condition.type());
        entity.setCountry(condition.country());
        entity.setCustomerCode(condition.customerCode());
        entity.setMaterialCode(condition.materialCode());
        entity.setAmount(condition.amount());
        entity.setValidFrom(condition.validity().validFrom());
        entity.setValidTo(condition.validity().validTo());
        return toDomain(dataRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PriceCondition> findById(Long id) {
        return dataRepository.findById(id).map(PriceConditionRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceCondition> findAll(Country country, ConditionType type, String materialCode) {
        Specification<PriceConditionEntity> spec = (root, query, cb) -> cb.conjunction();
        if (country != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("country"), country));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (materialCode != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("materialCode"), materialCode));
        }
        return dataRepository.findAll(spec, Sort.by("id")).stream()
                .map(PriceConditionRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceCondition> findByTypeCountryAndMaterial(ConditionType type, Country country,
                                                             String materialCode) {
        return dataRepository.findByTypeAndCountryAndMaterialCode(type, country, materialCode).stream()
                .map(PriceConditionRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceCondition> findValidOn(Country country, String materialCode, LocalDate date) {
        return dataRepository.findValidOn(country, materialCode, date).stream()
                .map(PriceConditionRepositoryAdapter::toDomain)
                .toList();
    }

    static PriceCondition toDomain(PriceConditionEntity entity) {
        return new PriceCondition(
                entity.getId(),
                entity.getType(),
                entity.getCountry(),
                entity.getCustomerCode(),
                entity.getMaterialCode(),
                entity.getAmount(),
                new ValidityPeriod(entity.getValidFrom(), entity.getValidTo()));
    }
}
