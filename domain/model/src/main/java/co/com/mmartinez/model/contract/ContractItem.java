package co.com.mmartinez.model.contract;

import co.com.mmartinez.model.common.BusinessException;
import co.com.mmartinez.model.common.ErrorCode;

import java.math.BigDecimal;

/**
 * Posición de un contrato: un material con su cantidad pactada y precio unitario.
 */
public record ContractItem(String materialCode, BigDecimal quantity, BigDecimal unitPrice) {

    public ContractItem {
        if (materialCode == null || materialCode.isBlank()) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD, "materialCode");
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, "unitPrice");
        }
        materialCode = materialCode.trim().toUpperCase();
    }

    /**
     * La cantidad adicional se suma a la que ya tiene la posición; no la reemplaza.
     */
    public ContractItem addQuantity(BigDecimal additionalQuantity) {
        if (additionalQuantity == null || additionalQuantity.signum() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }
        return new ContractItem(materialCode, quantity.add(additionalQuantity), unitPrice);
    }

    public BigDecimal total() {
        return quantity.multiply(unitPrice);
    }
}
