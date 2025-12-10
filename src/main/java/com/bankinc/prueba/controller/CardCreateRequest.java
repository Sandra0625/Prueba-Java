package com.bankinc.prueba.controller;

public class CardCreateRequest {
    private String productId;
    private String holderName;

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }
}
