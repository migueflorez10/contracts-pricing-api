package co.com.mmartinez.model.common;

/**
 * Catálogo de errores de negocio. La categoría permite que cada punto de entrada
 * traduzca el error a su protocolo (por ejemplo, un código HTTP) sin que el dominio lo conozca.
 */
public enum ErrorCode {
    CONTRACT_NOT_FOUND(Category.NOT_FOUND, "El contrato no existe"),
    PRICE_CONDITION_NOT_FOUND(Category.NOT_FOUND, "La condición de precio no existe"),
    PRICE_NOT_FOUND(Category.NOT_FOUND, "No hay un precio vigente para el material en la fecha indicada"),
    ITEM_NOT_FOUND(Category.NOT_FOUND, "El material no hace parte del contrato"),

    INVALID_VALIDITY(Category.BUSINESS_RULE, "La fecha de inicio debe ser anterior o igual a la fecha de fin"),
    INVALID_STATUS_TRANSITION(Category.BUSINESS_RULE, "La operación no está permitida en el estado actual del contrato"),
    INVALID_QUANTITY(Category.BUSINESS_RULE, "La cantidad debe ser mayor que cero"),
    INVALID_AMOUNT(Category.BUSINESS_RULE, "El valor de la condición no es válido"),
    EMPTY_CONTRACT(Category.BUSINESS_RULE, "El contrato debe tener al menos una posición"),
    DUPLICATED_MATERIAL(Category.BUSINESS_RULE, "Un material no puede repetirse dentro del mismo contrato"),
    REQUIRED_FIELD(Category.BUSINESS_RULE, "Falta un campo obligatorio"),
    SAME_VALIDITY(Category.BUSINESS_RULE, "La nueva fecha de fin es igual a la vigente"),

    CONTRACT_OVERLAP(Category.CONFLICT, "El cliente ya tiene un contrato vigente o pendiente en ese periodo"),
    PRICE_CONDITION_OVERLAP(Category.CONFLICT, "Ya existe una condición del mismo tipo para ese periodo");

    public enum Category {
        NOT_FOUND,
        BUSINESS_RULE,
        CONFLICT
    }

    private final Category category;
    private final String message;

    ErrorCode(Category category, String message) {
        this.category = category;
        this.message = message;
    }

    public Category getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }
}
