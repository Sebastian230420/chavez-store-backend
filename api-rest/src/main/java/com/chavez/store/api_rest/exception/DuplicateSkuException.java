package com.chavez.store.api_rest.exception;

/** SKU duplicado. HTTP 409 · PROD_001 */
public class DuplicateSkuException extends BusinessException {

    public DuplicateSkuException(String sku) {
        super("PROD_001", 409, "El SKU ya existe: " + sku);
    }
}