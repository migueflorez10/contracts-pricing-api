package co.com.mmartinez.api.pricecondition;

import co.com.mmartinez.model.common.BusinessException;
import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.common.ErrorCode;
import co.com.mmartinez.model.common.ValidityPeriod;
import co.com.mmartinez.model.pricecondition.ConditionType;
import co.com.mmartinez.model.pricecondition.PriceCondition;
import co.com.mmartinez.model.pricecondition.PriceQuote;
import co.com.mmartinez.usecase.pricecondition.NetPriceUseCase;
import co.com.mmartinez.usecase.pricecondition.PriceConditionUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PriceConditionController.class)
class PriceConditionControllerTest {

    private static final ValidityPeriod YEAR_2026 =
            new ValidityPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PriceConditionUseCase priceConditionUseCase;

    @MockitoBean
    private NetPriceUseCase netPriceUseCase;

    @Test
    void createShouldReturn201() throws Exception {
        when(priceConditionUseCase.create(any())).thenReturn(new PriceCondition(5L, ConditionType.DISCOUNT,
                Country.SV, "C-1001", "CEM-50KG", new BigDecimal("5"), YEAR_2026));

        mockMvc.perform(post("/api/v1/price-conditions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "DISCOUNT",
                                  "country": "SV",
                                  "customerCode": "C-1001",
                                  "materialCode": "CEM-50KG",
                                  "amount": 5,
                                  "validFrom": "2026-01-01",
                                  "validTo": "2026-12-31"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void createShouldReturn409WhenConditionOverlaps() throws Exception {
        when(priceConditionUseCase.create(any()))
                .thenThrow(new BusinessException(ErrorCode.PRICE_CONDITION_OVERLAP, "CEM-50KG"));

        mockMvc.perform(post("/api/v1/price-conditions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PRICE",
                                  "country": "SV",
                                  "materialCode": "CEM-50KG",
                                  "amount": 9.8,
                                  "validFrom": "2026-01-01",
                                  "validTo": "2026-12-31"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRICE_CONDITION_OVERLAP"));
    }

    @Test
    void netPriceShouldReturnQuote() throws Exception {
        LocalDate date = LocalDate.of(2026, 5, 15);
        when(netPriceUseCase.calculate(Country.SV, "C-1001", "CEM-50KG", date)).thenReturn(new PriceQuote(
                Country.SV, "C-1001", "CEM-50KG", date, "USD", new BigDecimal("9.50"), new BigDecimal("5"),
                new BigDecimal("9.03"), 2L, 4L));

        mockMvc.perform(get("/api/v1/prices/net")
                        .param("country", "SV")
                        .param("customerCode", "C-1001")
                        .param("materialCode", "CEM-50KG")
                        .param("date", "2026-05-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netPrice").value(9.03))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void netPriceShouldReturn400WhenCountryIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/prices/net").param("materialCode", "CEM-50KG"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
