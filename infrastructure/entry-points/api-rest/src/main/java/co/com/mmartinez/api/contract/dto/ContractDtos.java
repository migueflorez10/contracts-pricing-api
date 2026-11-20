package co.com.mmartinez.api.contract.dto;

import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.contract.ContractStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Objetos de entrada y salida de la API de contratos.
 */
public final class ContractDtos {

    private ContractDtos() {
    }

    public record CreateContractRequest(
            @Schema(example = "C-1001") @NotBlank @Size(max = 20) String customerCode,
            @Schema(example = "CO") @NotNull Country country,
            @Schema(example = "2026-01-01") @NotNull LocalDate validFrom,
            @Schema(example = "2026-06-30") @NotNull LocalDate validTo,
            @NotEmpty @Valid List<ContractItemRequest> items) {
    }

    public record ContractItemRequest(
            @Schema(example = "CEM-50KG") @NotBlank @Size(max = 20) String materialCode,
            @Schema(example = "10000") @NotNull @Positive BigDecimal quantity,
            @Schema(example = "32000") @NotNull @PositiveOrZero BigDecimal unitPrice) {
    }

    public record AddQuantityRequest(
            @Schema(example = "2500") @NotNull @Positive BigDecimal additionalQuantity) {
    }

    public record ValidityChangeRequest(
            @Schema(example = "2026-12-31") @NotNull LocalDate newValidTo) {
    }

    public record RejectRequest(
            @Schema(example = "Falta el soporte del área comercial") @NotBlank @Size(max = 255) String reason) {
    }

    public record ContractItemResponse(String materialCode, BigDecimal quantity, BigDecimal unitPrice,
                                       BigDecimal total) {
    }

    public record ContractResponse(Long id, String customerCode, Country country, String currency,
                                   LocalDate validFrom, LocalDate validTo, LocalDate pendingValidTo,
                                   ContractStatus status, String rejectionReason, BigDecimal total,
                                   List<ContractItemResponse> items) {
    }
}
