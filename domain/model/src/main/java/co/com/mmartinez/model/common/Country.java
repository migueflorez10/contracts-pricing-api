package co.com.mmartinez.model.common;

/**
 * Países atendidos por la operación, con la moneda local en la que se expresan los precios.
 */
public enum Country {
    AR("Argentina", "ARS"),
    CO("Colombia", "COP"),
    CR("Costa Rica", "CRC"),
    EC("Ecuador", "USD"),
    MX("México", "MXN"),
    SV("El Salvador", "USD"),
    NI("Nicaragua", "NIO"),
    GT("Guatemala", "GTQ"),
    PE("Perú", "PEN");

    private final String displayName;
    private final String currency;

    Country(String displayName, String currency) {
        this.displayName = displayName;
        this.currency = currency;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCurrency() {
        return currency;
    }
}
