package com.leo.pos.shop;

public record ShopDto(String id, String name, String type) {
    public static ShopDto from(Shop shop) {
        return new ShopDto(shop.getId(), shop.getName(), shop.getType());
    }
}