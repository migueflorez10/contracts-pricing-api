package co.com.mmartinez.api.pricecondition.dto;

import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.pricecondition.ConditionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Objetos de entrada y salida de la API de condiciones de precio.
 */
public final class PriceDtos {

    private PriceDtos() {
    }

    public record CreatePriceConditionRequest(
            @Schema(example = "PRICE") @NotNull ConditionType type,
            @Schema(example = "SV") @NotNull Country country,
            @Schema(description = "Vacío para una condición general del país", example = "C-1001")
            @Size(max = 20) String customerCode,
            @Schema(example = "CEM-50KG") @NotBlank @Size(max = 20) String materialCode,
            @Schema(description = "Precio por unidad, o porcentaje si es descuento", example = "9.50")
            @NotNull @Positive BigDecimal amount,
            @Schema(example = "2026-01-01") @NotNull LocalDate validFrom,
            @Schema(example = "2026-12-31") @NotNull LocalDate validTo) {
    }

    public record PriceConditionResponse(Long id, ConditionType type, Country country, String customerCode,
                                         String materialCode, BigDecimal amount, String currency,
                                         LocalDate validFrom, LocalDate validTo) {
    }

    public record PriceQuoteResponse(Country country, String customerCode, String materialCode, LocalDate date,
                                     String currency, BigDecimal basePrice, BigDecimal discountPercent,
                                     BigDecimal netPrice, Long priceConditionId, Long discountConditionId) {
    }
}
