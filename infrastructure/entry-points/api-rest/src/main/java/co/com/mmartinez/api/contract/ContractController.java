package co.com.mmartinez.api.contract;

import co.com.mmartinez.api.contract.dto.ContractDtos.AddQuantityRequest;
import co.com.mmartinez.api.contract.dto.ContractDtos.ContractResponse;
import co.com.mmartinez.api.contract.dto.ContractDtos.CreateContractRequest;
import co.com.mmartinez.api.contract.dto.ContractDtos.RejectRequest;
import co.com.mmartinez.api.contract.dto.ContractDtos.ValidityChangeRequest;
import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.contract.Contract;
import co.com.mmartinez.model.contract.ContractStatus;
import co.com.mmartinez.usecase.contract.ContractUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/contracts")
@Tag(name = "Contratos", description = "Creación, consulta y modificación de contratos")
public class ContractController {

    private final ContractUseCase contractUseCase;

    public ContractController(ContractUseCase contractUseCase) {
        this.contractUseCase = contractUseCase;
    }

    @PostMapping
    @Operation(summary = "Crea un contrato. Queda pendiente de aprobación.")
    public ResponseEntity<ContractResponse> create(@Valid @RequestBody CreateContractRequest request) {
        Contract created = contractUseCase.create(ContractMapper.toDomain(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(ContractMapper.toResponse(created));
    }

    @GetMapping
    @Operation(summary = "Lista contratos, con filtros opcionales por país y estado")
    public List<ContractResponse> list(@RequestParam(required = false) Country country,
                                       @RequestParam(required = false) ContractStatus status) {
        return contractUseCase.list(country, status).stream()
                .map(ContractMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta un contrato por su identificador")
    public ContractResponse getById(@PathVariable Long id) {
        return ContractMapper.toResponse(contractUseCase.getById(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Aprueba un contrato nuevo o un cambio de vigencia pendiente")
    public ContractResponse approve(@PathVariable Long id) {
        return ContractMapper.toResponse(contractUseCase.approve(id));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Rechaza un contrato nuevo o un cambio de vigencia pendiente")
    public ContractResponse reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request) {
        return ContractMapper.toResponse(contractUseCase.reject(id, request.reason()));
    }

    @PatchMapping("/{id}/items/{materialCode}")
    @Operation(summary = "Suma cantidad a una posición. No requiere aprobación.")
    public ContractResponse addQuantity(@PathVariable Long id, @PathVariable String materialCode,
                                        @Valid @RequestBody AddQuantityRequest request) {
        return ContractMapper.toResponse(
                contractUseCase.addQuantity(id, materialCode, request.additionalQuantity()));
    }

    @PostMapping("/{id}/validity-change")
    @Operation(summary = "Solicita cambiar la fecha de fin. Queda pendiente de aprobación.")
    public ContractResponse requestValidityChange(@PathVariable Long id,
                                                  @Valid @RequestBody ValidityChangeRequest request) {
        return ContractMapper.toResponse(contractUseCase.requestValidityChange(id, request.newValidTo()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancela un contrato")
    public ContractResponse cancel(@PathVariable Long id) {
        return ContractMapper.toResponse(contractUseCase.cancel(id));
    }
}
