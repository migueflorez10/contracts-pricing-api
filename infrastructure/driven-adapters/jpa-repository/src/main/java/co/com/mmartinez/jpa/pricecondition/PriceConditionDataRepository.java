package co.com.mmartinez.jpa.pricecondition;

import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.pricecondition.ConditionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PriceConditionDataRepository
        extends JpaRepository<PriceConditionEntity, Long>, JpaSpecificationExecutor<PriceConditionEntity> {

    List<PriceConditionEntity> findByTypeAndCountryAndMaterialCode(ConditionType type, Country country,
                                                                   String materialCode);

    @Query("""
            select p from PriceConditionEntity p
            where p.country = :country
              and p.materialCode = :materialCode
              and p.validFrom <= :date
              and p.validTo >= :date
            """)
    List<PriceConditionEntity> findValidOn(@Param("country") Country country,
                                           @Param("materialCode") String materialCode,
                                           @Param("date") LocalDate date);
}
