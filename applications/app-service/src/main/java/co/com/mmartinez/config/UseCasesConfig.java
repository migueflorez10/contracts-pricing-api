package co.com.mmartinez.config;

import co.com.mmartinez.model.contract.gateways.ContractRepository;
import co.com.mmartinez.model.pricecondition.gateways.PriceConditionRepository;
import co.com.mmartinez.usecase.contract.ContractUseCase;
import co.com.mmartinez.usecase.pricecondition.NetPriceUseCase;
import co.com.mmartinez.usecase.pricecondition.PriceConditionUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Los casos de uso no dependen de Spring. Aquí se registran como beans y se les inyecta
 * la implementación de cada puerto (gateway) que aporta la capa de infraestructura.
 */
@Configuration
public class UseCasesConfig {

    @Bean
    public ContractUseCase contractUseCase(ContractRepository contractRepository) {
        return new ContractUseCase(contractRepository);
    }

    @Bean
    public PriceConditionUseCase priceConditionUseCase(PriceConditionRepository priceConditionRepository) {
        return new PriceConditionUseCase(priceConditionRepository);
    }

    @Bean
    public NetPriceUseCase netPriceUseCase(PriceConditionRepository priceConditionRepository) {
        return new NetPriceUseCase(priceConditionRepository);
    }
}
