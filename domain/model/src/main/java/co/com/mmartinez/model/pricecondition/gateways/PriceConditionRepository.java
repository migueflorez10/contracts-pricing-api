package co.com.mmartinez.model.pricecondition.gateways;

import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.pricecondition.ConditionType;
import co.com.mmartinez.model.pricecondition.PriceCondition;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceConditionRepository {

    PriceCondition save(PriceCondition condition);

    Optional<PriceCondition> findById(Long id);

    /**
     * Filtros opcionales: un valor nulo significa "sin filtrar por ese campo".
     */
    List<PriceCondition> findAll(Country country, ConditionType type, String materialCode);

    /**
     * Condiciones del mismo tipo, país y material, sin importar cliente ni vigencia.
     */
    List<PriceCondition> findByTypeCountryAndMaterial(ConditionType type, Country country, String materialCode);

    /**
     * Condiciones de un material en un país que están vigentes en la fecha indicada.
     */
    List<PriceCondition> findValidOn(Country country, String materialCode, LocalDate date);
}
