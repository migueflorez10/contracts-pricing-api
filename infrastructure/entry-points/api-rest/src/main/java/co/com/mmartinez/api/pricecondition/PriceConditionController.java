package co.com.mmartinez.api.pricecondition;

import co.com.mmartinez.api.pricecondition.dto.PriceDtos.CreatePriceConditionRequest;
import co.com.mmartinez.api.pricecondition.dto.PriceDtos.PriceConditionResponse;
import co.com.mmartinez.api.pricecondition.dto.PriceDtos.PriceQuoteResponse;
import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.common.ValidityPeriod;
import co.com.mmartinez.model.pricecondition.ConditionType;
import co.com.mmartinez.model.pricecondition.PriceCondition;
import co.com.mmartinez.model.pricecondition.PriceQuote;
import co.com.mmartinez.usecase.pricecondition.NetPriceUseCase;
import co.com.mmartinez.usecase.pricecondition.PriceConditionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Precios", description = "Condiciones de precio, descuentos y cálculo del precio neto")
public class PriceConditionController {

    private final PriceConditionUseCase priceConditionUseCase;
    private final NetPriceUseCase netPriceUseCase;

    public PriceConditionController(PriceConditionUseCase priceConditionUseCase, NetPriceUseCase netPriceUseCase) {
        this.priceConditionUseCase = priceConditionUseCase;
        this.netPriceUseCase = netPriceUseCase;
    }

    @PostMapping("/price-conditions")
    @Operation(summary = "Registra un precio o un descuento, general del país o propio de un cliente")
    public ResponseEntity<PriceConditionResponse> create(@Valid @RequestBody CreatePriceConditionRequest request) {
        PriceCondition created = priceConditionUseCase.create(PriceCondition.create(
                request.type(), request.country(), request.customerCode(), request.materialCode(),
                request.amount(), new ValidityPeriod(request.validFrom(), request.validTo())));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(toResponse(created));
    }

    @GetMapping("/price-conditions")
    @Operation(summary = "Lista condiciones, con filtros opcionales")
    public List<PriceConditionResponse> list(@RequestParam(required = false) Country country,
                                             @RequestParam(required = false) ConditionType type,
                                             @RequestParam(required = false) String materialCode) {
        return priceConditionUseCase.list(country, type, materialCode).stream()
                .map(PriceConditionController::toResponse)
                .toList();
    }

    @GetMapping("/price-conditions/{id}")
    @Operation(summary = "Consulta una condición por su identificador")
    public PriceConditionResponse getById(@PathVariable Long id) {
        return toResponse(priceConditionUseCase.getById(id));
    }

    @GetMapping("/prices/net")
    @Operation(summary = "Calcula el precio neto de un material para un cliente en una fecha")
    public PriceQuoteResponse netPrice(@RequestParam Country country,
                                       @RequestParam String materialCode,
                                       @RequestParam(required = false) String customerCode,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate effectiveDate = date == null ? LocalDate.now() : date;
        PriceQuote quote = netPriceUseCase.calculate(country, customerCode, materialCode, effectiveDate);
        return new PriceQuoteResponse(quote.country(), quote.customerCode(), quote.materialCode(), quote.date(),
                quote.currency(), quote.basePrice(), quote.discountPercent(), quote.netPrice(),
                quote.priceConditionId(), quote.discountConditionId());
    }

    private static PriceConditionResponse toResponse(PriceCondition condition) {
        return new PriceConditionResponse(condition.id(), condition.type(), condition.country(),
                condition.customerCode(), condition.materialCode(), condition.amount(), condition.currency(),
                condition.validity().validFrom(), condition.validity().validTo());
    }
}
