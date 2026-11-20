package co.com.mmartinez.jpa.contract;

import co.com.mmartinez.model.common.Country;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ContractDataRepository
        extends JpaRepository<ContractEntity, Long>, JpaSpecificationExecutor<ContractEntity> {

    @Override
    @EntityGraph(attributePaths = "items")
    List<ContractEntity> findAll(Specification<ContractEntity> spec, Sort sort);

    @EntityGraph(attributePaths = "items")
    List<ContractEntity> findByCustomerCodeAndCountry(String customerCode, Country country);
}
