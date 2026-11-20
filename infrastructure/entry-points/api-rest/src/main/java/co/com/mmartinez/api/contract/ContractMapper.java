package co.com.mmartinez.api.contract;

import co.com.mmartinez.api.contract.dto.ContractDtos.ContractItemResponse;
import co.com.mmartinez.api.contract.dto.ContractDtos.ContractResponse;
import co.com.mmartinez.api.contract.dto.ContractDtos.CreateContractRequest;
import co.com.mmartinez.model.common.ValidityPeriod;
import co.com.mmartinez.model.contract.Contract;
import co.com.mmartinez.model.contract.ContractItem;

import java.util.List;

final class ContractMapper {

    private ContractMapper() {
    }

    static Contract toDomain(CreateContractRequest request) {
        List<ContractItem> items = request.items().stream()
                .map(item -> new ContractItem(item.materialCode(), item.quantity(), item.unitPrice()))
                .toList();
        return Contract.create(request.customerCode(), request.country(),
                new ValidityPeriod(request.validFrom(), request.validTo()), items);
    }

    static ContractResponse toResponse(Contract contract) {
        List<ContractItemResponse> items = contract.getItems().stream()
                .map(item -> new ContractItemResponse(item.materialCode(), item.quantity(), item.unitPrice(),
                        item.total()))
                .toList();
        return new ContractResponse(
                contract.getId(),
                contract.getCustomerCode(),
                contract.getCountry(),
                contract.getCountry().getCurrency(),
                contract.getValidity().validFrom(),
                contract.getValidity().validTo(),
                contract.getPendingValidTo(),
                contract.getStatus(),
                contract.getRejectionReason(),
                contract.total(),
                items);
    }
}
