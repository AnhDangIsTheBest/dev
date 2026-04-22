package com.auction.test;

import com.auction.dao.ItemDAO;
import com.auction.model.Item.Art;
import com.auction.model.Item.Electronics;
import com.auction.model.Item.Item;
import com.auction.model.Item.OtherItem;
import com.auction.model.Item.Vehicle;

public class TestItemDAO {
    public static void main(String[] args) {
        ItemDAO dao = new ItemDAO();

        Item item = new OtherItem(
                null,
                "Sách cổ",
                90000,
                100000,
                "OLD",
                "Sách xuất bản năm 2026",
                "Book"
        );

        boolean ok = dao.insert(item, 1);
        System.out.println("Insert: " + ok);

        for (Item i : dao.getAllItems()) {
            System.out.println(i.display());
        }
    }
}