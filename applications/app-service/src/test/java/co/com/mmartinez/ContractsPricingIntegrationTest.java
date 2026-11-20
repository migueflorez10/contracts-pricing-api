package co.com.mmartinez;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de punta a punta: API REST, casos de uso, JPA y migraciones de Flyway sobre H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContractsPricingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private long createContract(String customerCode, String validFrom, String validTo) throws Exception {
        String body = """
                {
                  "customerCode": "%s",
                  "country": "SV",
                  "validFrom": "%s",
                  "validTo": "%s",
                  "items": [
                    {"materialCode": "CEM-50KG", "quantity": 10000, "unitPrice": 9.50},
                    {"materialCode": "CEM-GRANEL", "quantity": 300, "unitPrice": 185}
                  ]
                }
                """.formatted(customerCode, validFrom, validTo);
        String response = mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    @Test
    void fullContractLifecycle() throws Exception {
        long id = createContract("INT-1001", "2026-01-01", "2026-06-30");

        mockMvc.perform(post("/api/v1/contracts/{id}/approve", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(patch("/api/v1/contracts/{id}/items/{material}", id, "CEM-50KG")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"additionalQuantity\": 10000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].quantity").value(20000));

        mockMvc.perform(post("/api/v1/contracts/{id}/validity-change", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newValidTo\": \"2026-12-31\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.validTo").value("2026-06-30"))
                .andExpect(jsonPath("$.pendingValidTo").value("2026-12-31"));

        mockMvc.perform(post("/api/v1/contracts/{id}/approve", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validTo").value("2026-12-31"));

        mockMvc.perform(get("/api/v1/contracts").param("country", "SV").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.customerCode == 'INT-1001')]").exists());
    }

    @Test
    void shouldRejectOverlappingContractForTheSameCustomer() throws Exception {
        createContract("INT-2002", "2026-01-01", "2026-06-30");

        mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerCode": "INT-2002",
                                  "country": "SV",
                                  "validFrom": "2026-03-01",
                                  "validTo": "2026-09-30",
                                  "items": [{"materialCode": "CEM-50KG", "quantity": 100, "unitPrice": 9.5}]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONTRACT_OVERLAP"));
    }

    @Test
    void shouldCalculateNetPriceWithCustomerDiscount() throws Exception {
        String general = """
                {"type": "PRICE", "country": "GT", "materialCode": "CEM-42KG", "amount": 78.50,
                 "validFrom": "2026-01-01", "validTo": "2026-12-31"}
                """;
        String discount = """
                {"type": "DISCOUNT", "country": "GT", "customerCode": "INT-3003", "materialCode": "CEM-42KG",
                 "amount": 10, "validFrom": "2026-01-01", "validTo": "2026-12-31"}
                """;
        mockMvc.perform(post("/api/v1/price-conditions").contentType(MediaType.APPLICATION_JSON).content(general))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/price-conditions").contentType(MediaType.APPLICATION_JSON).content(discount))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/prices/net")
                        .param("country", "GT")
                        .param("customerCode", "INT-3003")
                        .param("materialCode", "CEM-42KG")
                        .param("date", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("GTQ"))
                .andExpect(jsonPath("$.netPrice").value(70.65));

        mockMvc.perform(post("/api/v1/price-conditions").contentType(MediaType.APPLICATION_JSON).content(general))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRICE_CONDITION_OVERLAP"));
    }

    @Test
    void unknownContractShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/contracts/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONTRACT_NOT_FOUND"));
    }

    @Test
    void healthEndpointShouldBeUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
