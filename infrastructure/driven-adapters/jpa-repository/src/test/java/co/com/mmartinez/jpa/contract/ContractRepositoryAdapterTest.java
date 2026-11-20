package co.com.mmartinez.jpa.contract;

import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.common.ValidityPeriod;
import co.com.mmartinez.model.contract.Contract;
import co.com.mmartinez.model.contract.ContractItem;
import co.com.mmartinez.model.contract.ContractStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ContractRepositoryAdapter.class)
class ContractRepositoryAdapterTest {

    private static final ValidityPeriod FIRST_HALF =
            new ValidityPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

    @Autowired
    private ContractRepositoryAdapter adapter;

    private Contract newContract(String customer, Country country) {
        return Contract.create(customer, country, FIRST_HALF, List.of(
                new ContractItem("CEM-50KG", new BigDecimal("10000"), new BigDecimal("32000")),
                new ContractItem("CEM-GRANEL", new BigDecimal("500"), new BigDecimal("610000"))));
    }

    @Test
    void shouldSaveAndReadContractWithItems() {
        Contract saved = adapter.save(newContract("C-1001", Country.CO));

        Contract found = adapter.findById(saved.getId()).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getStatus()).isEqualTo(ContractStatus.PENDING_APPROVAL);
        assertThat(found.getValidity()).isEqualTo(FIRST_HALF);
        assertThat(found.getItems()).extracting(ContractItem::materialCode)
                .containsExactly("CEM-50KG", "CEM-GRANEL");
    }

    @Test
    void shouldPersistChangesOfAnExistingContract() {
        Contract saved = adapter.save(newContract("C-1001", Country.CO));
        saved.approve();
        saved.addQuantity("CEM-50KG", new BigDecimal("2500"));

        adapter.save(saved);
        Contract found = adapter.findById(saved.getId()).orElseThrow();

        assertThat(found.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(found.getItems()).hasSize(2);
        assertThat(found.getItems().get(0).quantity()).isEqualByComparingTo("12500");
    }

    @Test
    void shouldFilterByCountryAndStatus() {
        adapter.save(newContract("C-1001", Country.CO));
        Contract salvador = adapter.save(newContract("C-2002", Country.SV));
        salvador.approve();
        adapter.save(salvador);

        assertThat(adapter.findAll(Country.SV, null)).hasSize(1);
        assertThat(adapter.findAll(null, ContractStatus.ACTIVE))
                .extracting(Contract::getCustomerCode).containsExactly("C-2002");
        assertThat(adapter.findAll(null, null)).hasSize(2);
    }

    @Test
    void shouldFindByCustomerAndCountry() {
        adapter.save(newContract("C-1001", Country.CO));
        adapter.save(newContract("C-1001", Country.PE));

        assertThat(adapter.findByCustomerAndCountry("C-1001", Country.CO)).hasSize(1);
    }
}
