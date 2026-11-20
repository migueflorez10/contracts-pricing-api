package co.com.mmartinez.usecase.pricecondition;

import co.com.mmartinez.model.common.BusinessException;
import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.common.ErrorCode;
import co.com.mmartinez.model.pricecondition.ConditionType;
import co.com.mmartinez.model.pricecondition.PriceCondition;
import co.com.mmartinez.model.pricecondition.gateways.PriceConditionRepository;

import java.util.List;

/**
 * Casos de uso para registrar y consultar condiciones de precio.
 */
public class PriceConditionUseCase {

    private final PriceConditionRepository priceConditionRepository;

    public PriceConditionUseCase(PriceConditionRepository priceConditionRepository) {
        this.priceConditionRepository = priceConditionRepository;
    }

    public PriceCondition create(PriceCondition newCondition) {
        boolean overlaps = priceConditionRepository
                .findByTypeCountryAndMaterial(newCondition.type(), newCondition.country(), newCondition.materialCode())
                .stream()
                .anyMatch(existing -> existing.overlapsWith(newCondition));
        if (overlaps) {
            throw new BusinessException(ErrorCode.PRICE_CONDITION_OVERLAP, newCondition.materialCode());
        }
        return priceConditionRepository.save(newCondition);
    }

    public PriceCondition getById(Long id) {
        return priceConditionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICE_CONDITION_NOT_FOUND, String.valueOf(id)));
    }

    public List<PriceCondition> list(Country country, ConditionType type, String materialCode) {
        String normalizedMaterial = (materialCode == null || materialCode.isBlank())
                ? null : materialCode.trim().toUpperCase();
        return priceConditionRepository.findAll(country, type, normalizedMaterial);
    }
}
