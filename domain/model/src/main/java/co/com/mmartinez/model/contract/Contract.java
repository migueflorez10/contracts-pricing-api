package co.com.mmartinez.model.contract;

import co.com.mmartinez.model.common.BusinessException;
import co.com.mmartinez.model.common.Country;
import co.com.mmartinez.model.common.ErrorCode;
import co.com.mmartinez.model.common.ValidityPeriod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contrato de venta de un cliente en un país.
 *
 * <p>Reglas principales:
 * <ul>
 *     <li>Todo contrato nuevo queda pendiente de aprobación.</li>
 *     <li>Aumentar la cantidad de una posición no requiere aprobación: la cantidad se suma a la existente.</li>
 *     <li>Cambiar la vigencia sí requiere aprobación: el cambio queda pendiente hasta que se aprueba o rechaza.</li>
 * </ul>
 */
public class Contract {

    private final Long id;
    private final String customerCode;
    private final Country country;
    private ValidityPeriod validity;
    private LocalDate pendingValidTo;
    private ContractStatus status;
    private String rejectionReason;
    private final List<ContractItem> items;

    private Contract(Long id, String customerCode, Country country, ValidityPeriod validity,
                     LocalDate pendingValidTo, ContractStatus status, String rejectionReason,
                     List<ContractItem> items) {
        this.id = id;
        this.customerCode = customerCode;
        this.country = country;
        this.validity = validity;
        this.pendingValidTo = pendingValidTo;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.items = new ArrayList<>(items);
    }

    /**
     * Crea un contrato nuevo aplicando las validaciones de negocio.
     */
    public static Contract create(String customerCode, Country country, ValidityPeriod validity,
                                  List<ContractItem> items) {
        if (customerCode == null || customerCode.isBlank()) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD, "customerCode");
        }
        if (country == null) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD, "country");
        }
        if (validity == null) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD, "validity");
        }
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_CONTRACT);
        }
        Set<String> materials = new HashSet<>();
        for (ContractItem item : items) {
            if (!materials.add(item.materialCode())) {
                throw new BusinessException(ErrorCode.DUPLICATED_MATERIAL, item.materialCode());
            }
        }
        return new Contract(null, customerCode.trim(), country, validity, null,
                ContractStatus.PENDING_APPROVAL, null, items);
    }

    /**
     * Reconstruye un contrato ya existente, por ejemplo al leerlo de la base de datos.
     */
    public static Contract restore(Long id, String customerCode, Country country, ValidityPeriod validity,
                                   LocalDate pendingValidTo, ContractStatus status, String rejectionReason,
                                   List<ContractItem> items) {
        return new Contract(id, customerCode, country, validity, pendingValidTo, status, rejectionReason, items);
    }

    public void approve() {
        requireStatus(ContractStatus.PENDING_APPROVAL);
        if (pendingValidTo != null) {
            validity = validity.withValidTo(pendingValidTo);
            pendingValidTo = null;
        }
        rejectionReason = null;
        status = ContractStatus.ACTIVE;
    }

    /**
     * Si se rechaza un contrato nuevo, queda rechazado. Si se rechaza un cambio de vigencia,
     * el contrato sigue activo con la vigencia anterior.
     */
    public void reject(String reason) {
        requireStatus(ContractStatus.PENDING_APPROVAL);
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD, "reason");
        }
        rejectionReason = reason.trim();
        if (pendingValidTo != null) {
            pendingValidTo = null;
            status = ContractStatus.ACTIVE;
        } else {
            status = ContractStatus.REJECTED;
        }
    }

    public void addQuantity(String materialCode, BigDecimal additionalQuantity) {
        requireStatus(ContractStatus.ACTIVE);
        String normalized = materialCode == null ? "" : materialCode.trim().toUpperCase();
        for (int i = 0; i < items.size(); i++) {
            ContractItem item = items.get(i);
            if (item.materialCode().equals(normalized)) {
                items.set(i, item.addQuantity(additionalQuantity));
                return;
            }
        }
        throw new BusinessException(ErrorCode.ITEM_NOT_FOUND, normalized);
    }

    public void requestValidityChange(LocalDate newValidTo) {
        requireStatus(ContractStatus.ACTIVE);
        if (newValidTo == null) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD, "newValidTo");
        }
        if (newValidTo.equals(validity.validTo())) {
            throw new BusinessException(ErrorCode.SAME_VALIDITY);
        }
        validity.withValidTo(newValidTo); // valida que la nueva fecha no sea anterior al inicio
        pendingValidTo = newValidTo;
        status = ContractStatus.PENDING_APPROVAL;
    }

    public void cancel() {
        if (status == ContractStatus.CANCELLED || status == ContractStatus.REJECTED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, status.name());
        }
        pendingValidTo = null;
        status = ContractStatus.CANCELLED;
    }

    /**
     * Un contrato "abierto" (activo o pendiente) bloquea que el cliente tenga otro en el mismo periodo.
     */
    public boolean isOpen() {
        return status == ContractStatus.ACTIVE || status == ContractStatus.PENDING_APPROVAL;
    }

    public BigDecimal total() {
        return items.stream().map(ContractItem::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void requireStatus(ContractStatus expected) {
        if (status != expected) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "se esperaba " + expected + " y el contrato está en " + status);
        }
    }

    public Long getId() {
        return id;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public Country getCountry() {
        return country;
    }

    public ValidityPeriod getValidity() {
        return validity;
    }

    public LocalDate getPendingValidTo() {
        return pendingValidTo;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public List<ContractItem> getItems() {
        return List.copyOf(items);
    }
}
