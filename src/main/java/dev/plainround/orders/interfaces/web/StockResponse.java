package dev.plainround.orders.interfaces.web;

/**
 * The units on hand for a SKU.
 *
 * @param sku the product code
 * @param available the units
 */
public record StockResponse(String sku, int available) {}
