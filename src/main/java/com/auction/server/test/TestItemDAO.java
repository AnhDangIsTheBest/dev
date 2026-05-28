package com.auction.server.test;

import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;
import com.auction.shared.model.Item.Art;
import com.auction.shared.model.Item.Electronics;
import com.auction.shared.model.Item.Item;
import com.auction.shared.model.Item.OtherItem;
import com.auction.shared.model.Item.Vehicle;
import com.auction.shared.model.User.Seller;
import com.auction.shared.model.User.User;

import java.util.List;

public class TestItemDAO {
    public static void main(String[] args) {
        ItemDAO itemDAO = new ItemDAO();
        UserDAO userDAO = new UserDAO();
        String suffix = String.valueOf(System.currentTimeMillis());

        String sellerId = "SEL" + suffix.substring(suffix.length() - 8);
        String otherId = "ITM_OTHER_" + suffix.substring(suffix.length() - 6);
        String electronicsId = "ITM_ELEC_" + suffix.substring(suffix.length() - 6);
        String vehicleId = "ITM_VEH_" + suffix.substring(suffix.length() - 6);
        String artId = "ITM_ART_" + suffix.substring(suffix.length() - 6);

        System.out.println("===== TEST ITEM DAO =====");

        System.out.println("\n--- 0. CREATE TEST SELLER ---");
        User seller = new Seller(
                sellerId,
                "seller_item_" + suffix,
                "seller_item_" + suffix + "@test.com",
                "123456",
                "Seller Item Test",
                0,
                0
        );
        System.out.println("Insert seller: " + userDAO.insert(seller));

        Item other = new OtherItem(
                otherId,
                "Sach co test",
                90_000,
                90_000,
                "AVAILABLE",
                "Sach xuat ban nam 2029",
                "Book"
        );

        Item electronics = new Electronics(
                electronicsId,
                "Laptop test",
                "Laptop gaming test",
                15_000_000,
                "AVAILABLE",
                15_000_000,
                "Lenovo",
                24,
                "Legion"
        );

        Item vehicle = new Vehicle(
                vehicleId,
                "Xe test",
                50_000_000,
                50_000_000,
                "AVAILABLE",
                "Xe may test",
                "Honda",
                "Wave Alpha",
                2024,
                1000,
                "Motorbike"
        );

        Item art = new Art(
                artId,
                "Tranh test",
                5_000_000,
                5_000_000,
                "AVAILABLE",
                "Tranh son dau test",
                "Dang Artist",
                2026,
                "Oil"
        );

        System.out.println("\n--- 1. INSERT ITEMS ---");
        System.out.println("Insert other      : " + itemDAO.insert(other, sellerId));
        System.out.println("Insert electronics: " + itemDAO.insert(electronics, sellerId));
        System.out.println("Insert vehicle    : " + itemDAO.insert(vehicle, sellerId));
        System.out.println("Insert art        : " + itemDAO.insert(art, sellerId));

        System.out.println("\n--- 2. FIND BY ID ---");
        Item found = itemDAO.findById(otherId);
        System.out.println(found != null ? found.display() : "Not found");

        System.out.println("\n--- 3. GET ITEMS BY SELLER ---");
        List<Item> sellerItems = itemDAO.getItemsBySeller(sellerId);
        System.out.println("Seller items: " + sellerItems.size());
        for (Item i : sellerItems) {
            System.out.println("  " + i.display() + " | ID = " + i.getId());
        }

        System.out.println("\n--- 4. UPDATE ITEM ---");
        Item updatedOther = new OtherItem(
                otherId,
                "Sach co test updated",
                120_000,
                120_000,
                "AVAILABLE",
                "Mo ta da update",
                "Book"
        );
        System.out.println("Update other: " + itemDAO.update(updatedOther));
        Item afterUpdate = itemDAO.findById(otherId);
        System.out.println(afterUpdate != null ? afterUpdate.display() : "Not found after update");

        System.out.println("\n--- 5. GET ALL ITEMS ---");
        List<Item> all = itemDAO.getAllItems();
        System.out.println("Total items: " + all.size());

        System.out.println("\n--- 6. DELETE TEST ITEMS ---");
        System.out.println("Delete other      : " + itemDAO.delete(otherId));
        System.out.println("Delete electronics: " + itemDAO.delete(electronicsId));
        System.out.println("Delete vehicle    : " + itemDAO.delete(vehicleId));
        System.out.println("Delete art        : " + itemDAO.delete(artId));

        System.out.println("\n--- 7. DELETE TEST SELLER ---");
        System.out.println("Delete seller: " + userDAO.delete(sellerId));

        System.out.println("\n===== DONE =====");
    }
}
