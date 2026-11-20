package co.com.mmartinez.api.contract;

import co.com.mmartinez.model.common.BusinessException;
import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.common.ErrorCode;
import co.com.mmartinez.model.common.ValidityPeriod;
import co.com.mmartinez.model.contract.Contract;
import co.com.mmartinez.model.contract.ContractItem;
import co.com.mmartinez.model.contract.ContractStatus;
import co.com.mmartinez.usecase.contract.ContractUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContractController.class)
class ContractControllerTest {

    private static final ValidityPeriod FIRST_HALF =
            new ValidityPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContractUseCase contractUseCase;

    private static Contract stored(ContractStatus status) {
        return Contract.restore(1L, "C-1001", Country.CO, FIRST_HALF, null, status, null,
                List.of(new ContractItem("CEM-50KG", new BigDecimal("10000"), new BigDecimal("32000"))));
    }

    @Test
    void createShouldReturn201WithLocation() throws Exception {
        when(contractUseCase.create(any())).thenReturn(stored(ContractStatus.PENDING_APPROVAL));

        mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerCode": "C-1001",
                                  "country": "CO",
                                  "validFrom": "2026-01-01",
                                  "validTo": "2026-06-30",
                                  "items": [{"materialCode": "CEM-50KG", "quantity": 10000, "unitPrice": 32000}]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/contracts/1"))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.currency").value("COP"))
                .andExpect(jsonPath("$.items[0].total").value(320000000));
    }

    @Test
    void createShouldReturn400WhenFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"country": "CO", "validFrom": "2026-01-01", "validTo": "2026-06-30", "items": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.length()").value(2));
    }

    @Test
    void createShouldReturn422WhenDatesAreInverted() throws Exception {
        mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerCode": "C-1001",
                                  "country": "CO",
                                  "validFrom": "2026-06-30",
                                  "validTo": "2026-01-01",
                                  "items": [{"materialCode": "CEM-50KG", "quantity": 1, "unitPrice": 1}]
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_VALIDITY"));
    }

    @Test
    void createShouldReturn400WhenCountryIsNotSupported() throws Exception {
        mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerCode": "C-1001",
                                  "country": "BR",
                                  "validFrom": "2026-01-01",
                                  "validTo": "2026-06-30",
                                  "items": [{"materialCode": "CEM-50KG", "quantity": 1, "unitPrice": 1}]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void getByIdShouldReturn404WhenContractDoesNotExist() throws Exception {
        when(contractUseCase.getById(99L)).thenThrow(new BusinessException(ErrorCode.CONTRACT_NOT_FOUND, "99"));

        mockMvc.perform(get("/api/v1/contracts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONTRACT_NOT_FOUND"));
    }

    @Test
    void addQuantityShouldReturnUpdatedContract() throws Exception {
        Contract updated = Contract.restore(1L, "C-1001", Country.CO, FIRST_HALF, null, ContractStatus.ACTIVE, null,
                List.of(new ContractItem("CEM-50KG", new BigDecimal("12500"), new BigDecimal("32000"))));
        when(contractUseCase.addQuantity(eq(1L), eq("CEM-50KG"), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/contracts/1/items/CEM-50KG")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"additionalQuantity\": 2500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(12500));
    }

    @Test
    void approveShouldReturn422WhenTransitionIsNotAllowed() throws Exception {
        when(contractUseCase.approve(1L)).thenThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION));

        mockMvc.perform(post("/api/v1/contracts/1/approve"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }
}
