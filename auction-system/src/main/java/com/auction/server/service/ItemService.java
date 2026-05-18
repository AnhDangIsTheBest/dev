package com.auction.server.service;

import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.Item.Item;

import java.util.List;

public class ItemService {

    private final ItemDAO itemDAO = new ItemDAO();

    public boolean createItem(Item item, String sellerId) {
        validateItem(item);

        if (sellerId == null || sellerId.isBlank()) {
            throw new IllegalArgumentException("sellerId không được trống");
        }

        return itemDAO.insert(item, sellerId);
    }

    public Item getItemById(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId không được trống");
        }

        return itemDAO.findById(itemId);
    }

    public List<Item> getAllItems() {
        return itemDAO.getAllItems();
    }

    public List<Item> getItemsBySeller(String sellerId) {
        if (sellerId == null || sellerId.isBlank()) {
            throw new IllegalArgumentException("sellerId không được trống");
        }

        return itemDAO.getItemsBySeller(sellerId);
    }

    public boolean updateItem(Item item) {
        validateItem(item);

        if (item.getId() == null || item.getId().isBlank()) {
            throw new IllegalArgumentException("itemId không được trống");
        }

        return itemDAO.update(item);
    }

    public boolean deleteItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId không được trống");
        }

        return itemDAO.delete(itemId);
    }

    private void validateItem(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Item không được null");
        }

        if (item.getName() == null || item.getName().isBlank()) {
            throw new IllegalArgumentException("Tên item không được trống");
        }

        if (item.getStartingPrice() < 0) {
            throw new IllegalArgumentException("Giá khởi điểm không được âm");
        }

        if (item.getCurrentPrice() < 0) {
            throw new IllegalArgumentException("Giá hiện tại không được âm");
        }

        if (item.getStatus() == null || item.getStatus().isBlank()) {
            throw new IllegalArgumentException("Trạng thái item không được trống");
        }
    }
}