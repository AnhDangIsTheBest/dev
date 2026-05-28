package com.auction.client.controller;

import com.auction.client.ClientContext;
import com.auction.client.network.AuctionClient;
import com.auction.shared.model.Auction;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Item.Electronics;
import com.auction.shared.model.Item.Item;
import com.auction.shared.model.User.Bidder;
import com.auction.shared.model.User.User;
import com.auction.shared.network.protocol.SocketMessage;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerSmokeTest {
    private static boolean platformStarted;

    @BeforeAll
    static void startJavaFx() throws Exception {
        if (!platformStarted) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> {
                platformStarted = true;
                latch.countDown();
            });
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        Thread.sleep(300);
        ClientContext.getInstance().setCurrentUser(null);
        setClient(null);
    }

    @Test
    void auctionsControllerRendersDashboardDetailsAndValidationBranches() throws Exception {
        runFxAndWait(() -> {
            try {
                ClientContext.getInstance().setCurrentUser(user("BIDDER"));
                setClient(new FakeAuctionClient());
                AuctionsController controller = new AuctionsController();
                injectAuctionFields(controller);

                Auction running = auction("A1", Auction.AuctionStatus.RUNNING, "SELLER", "BIDDER");
                Auction own = auction("A2", Auction.AuctionStatus.OPEN, "BIDDER", null);
                Auction finished = auction("A3", Auction.AuctionStatus.FINISHED, "SELLER", "BIDDER");
                Auction upcoming = auction("A4", Auction.AuctionStatus.OPEN, "SELLER", null);
                upcoming.setEndTime(LocalDateTime.now().plusHours(2));
                Auction pending = new Auction("A5", running.getItem(), LocalDateTime.now().plusMinutes(10),
                        LocalDateTime.now().plusHours(1), null, null, false, 0, 0);
                pending.setStatus(Auction.AuctionStatus.PENDING);

                invoke(controller, "setupInitialViews");
                invoke(controller, "setupTable");
                invoke(controller, "renderDashboardCards", new Class<?>[] {List.class},
                        List.of(running, finished, upcoming, pending));
                invoke(controller, "renderDashboardCards", new Class<?>[] {List.class}, List.of());
                invoke(controller, "updateStatCards", new Class<?>[] {List.class},
                        List.of(running, finished, upcoming, pending));

                @SuppressWarnings("unchecked")
                TableColumn<Auction, String> statusColumn =
                        (TableColumn<Auction, String>) get(controller, "colStatus");
                TableCell<Auction, String> statusCell = statusColumn.getCellFactory().call(statusColumn);
                invokeUpdateItem(statusCell, null, true);
                for (Auction.AuctionStatus status : Auction.AuctionStatus.values()) {
                    invokeUpdateItem(statusCell, invoke(controller, "statusDisplay",
                            new Class<?>[] {Auction.AuctionStatus.class}, status), false);
                }

                invoke(controller, "renderDetail", new Class<?>[] {Auction.class}, running);
                invoke(controller, "renderDetail", new Class<?>[] {Auction.class}, own);
                invoke(controller, "renderDetail", new Class<?>[] {Auction.class}, finished);
                invoke(controller, "renderDetail", new Class<?>[] {Auction.class}, pending);
                invoke(controller, "switchAuctionView", new Class<?>[] {boolean.class}, false);
                invoke(controller, "switchAuctionView", new Class<?>[] {boolean.class}, true);
                invoke(controller, "switchAuctionView", new Class<?>[] {boolean.class}, true);
                invoke(controller, "switchAuctionView", new Class<?>[] {boolean.class}, false);
                invoke(controller, "animateDetailSections");
                invoke(controller, "setAnimatedCache", new Class<?>[] {javafx.scene.Node.class, boolean.class},
                        (javafx.scene.Node) null, true);
                invoke(controller, "prepareSeeAllButton", new Class<?>[] {Button.class}, (Button) null);
                Button motionButton = new Button();
                invoke(controller, "installFocusMotion", new Class<?>[] {javafx.scene.Node.class}, (javafx.scene.Node) null);
                invoke(controller, "installFocusMotion", new Class<?>[] {javafx.scene.Node.class}, motionButton);
                invoke(controller, "installFocusMotion", new Class<?>[] {javafx.scene.Node.class}, motionButton);
                VBox card = new VBox();
                invoke(controller, "installCardMotion", new Class<?>[] {VBox.class}, card);
                card.getOnMouseEntered().handle(null);
                card.getOnMousePressed().handle(null);
                card.getOnMouseReleased().handle(null);
                card.getOnMouseExited().handle(null);
                invoke(controller, "animateSelectionFlash", new Class<?>[] {javafx.scene.Node.class}, card);
                invoke(controller, "animateSelectionFlash", new Class<?>[] {javafx.scene.Node.class}, (javafx.scene.Node) null);
                invoke(controller, "handleBroadcast", new Class<?>[] {SocketMessage.class},
                        SocketMessage.ok(SocketMessage.Action.BROADCAST_AUCTION_END, "done").put("auction", finished));
                invoke(controller, "handleBroadcast", new Class<?>[] {SocketMessage.class},
                        SocketMessage.ok(SocketMessage.Action.BROADCAST_BID_UPDATE, "empty"));

                TextField bidAmount = (TextField) get(controller, "bidAmountField");
                bidAmount.setText("abc");
                set(controller, "selectedAuction", running);
                invoke(controller, "handlePlaceBid", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                bidAmount.setText("160000");
                invoke(controller, "handlePlaceBid", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                bidAmount.setText("170000");
                invoke(controller, "handlePlaceBid", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                bidAmount.setText("180000");
                invoke(controller, "handlePlaceBid", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());

                TextField autoMax = (TextField) get(controller, "autoMaxBidField");
                TextField autoInc = (TextField) get(controller, "autoIncrementField");
                autoMax.setText("120000");
                autoInc.setText("10000");
                invoke(controller, "handleSetAutoBid", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                autoMax.setText("250000");
                autoInc.setText("0");
                invoke(controller, "handleSetAutoBid", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                set(controller, "selectedAuction", finished);
                autoMax.setText("250000");
                autoInc.setText("10000");
                invoke(controller, "handleSetAutoBid", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                set(controller, "selectedAuction", running);
                autoMax.setText("250000");
                autoInc.setText("10000");
                invoke(controller, "handleSetAutoBid", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                invoke(controller, "handleCancelAutoBid", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());

                @SuppressWarnings("unchecked")
                ObservableList<Auction> auctions = (ObservableList<Auction>) get(controller, "auctions");
                auctions.setAll(running);
                controller.openAuctionById(null);
                controller.openAuctionById(" ");
                controller.openAuctionById("A1");
                controller.openAuctionById("MISSING");
                invoke(controller, "openAuction", new Class<?>[] {Auction.class}, (Auction) null);

                Item imageItem = running.getItem();
                imageItem.setImageData(new byte[] {1, 2, 3});
                invoke(controller, "loadItemImage", new Class<?>[] {Item.class}, imageItem);
                imageItem.setImageData(new byte[0]);
                invoke(controller, "loadItemImage", new Class<?>[] {Item.class}, imageItem);
                invoke(controller, "loadItemImage", new Class<?>[] {Item.class}, (Item) null);
                Auction noStart = new Auction("A6", running.getItem(), null, null, null, null, true, 30, 60);
                invoke(controller, "formatAuctionTime", new Class<?>[] {Auction.class}, noStart);

                assertTrue(((String) invoke(controller, "fmtTimer", new Class<?>[] {long.class}, 0L)).contains("t"));
                assertEquals("1m 5s", invoke(controller, "fmtTimer", new Class<?>[] {long.class}, 65L));
                assertEquals("1h 1m", invoke(controller, "fmtTimer", new Class<?>[] {long.class}, 3661L));
                assertFalse((Boolean) invoke(controller, "isPastAuction", new Class<?>[] {Auction.class}, (Auction) null));
                assertFalse((Boolean) invoke(controller, "isPastAuction", new Class<?>[] {Auction.class}, running));
                assertTrue((Boolean) invoke(controller, "isPastAuction", new Class<?>[] {Auction.class}, finished));
                assertTrue((Boolean) invoke(controller, "isPastAuction", new Class<?>[] {Auction.class},
                        auction("A7", Auction.AuctionStatus.CANCELED, "SELLER", null)));
                assertFalse((Boolean) invoke(controller, "isRunningAuction", new Class<?>[] {Auction.class}, (Auction) null));
                assertFalse((Boolean) invoke(controller, "isRunningAuction", new Class<?>[] {Auction.class}, pending));
                assertFalse((Boolean) invoke(controller, "canAcceptBid", new Class<?>[] {Auction.class}, (Auction) null));
                assertFalse((Boolean) invoke(controller, "canAcceptBid", new Class<?>[] {Auction.class}, own));
                assertTrue((Boolean) invoke(controller, "canAcceptBid", new Class<?>[] {Auction.class}, running));
                assertFalse((Boolean) invoke(controller, "isOwnAuction", new Class<?>[] {Auction.class}, (Auction) null));
                assertTrue(((String) invoke(controller, "shortText", new Class<?>[] {String.class, int.class},
                        "abcdefghijklmnopqrstuvwxyz", 8)).endsWith("..."));
                assertEquals("", invoke(controller, "shortText", new Class<?>[] {String.class, int.class}, null, 8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void simpleControllersHandleValidationAndSuccessBranches() throws Exception {
        runFxAndWait(() -> {
            try {
                ClientContext.getInstance().setCurrentUser(user("BIDDER"));
                setClient(new FakeAuctionClient());

                DepositController deposit = new DepositController();
                set(deposit, "balanceLabel", new Label());
                set(deposit, "amountField", new TextField());
                set(deposit, "btnDeposit", new Button());
                set(deposit, "resultLabel", new Label());
                deposit.initialize();
                invoke(deposit, "handleDeposit");
                ((TextField) get(deposit, "amountField")).setText("500000");
                invoke(deposit, "handleDeposit");

                AutoBidController autoBid = new AutoBidController();
                set(autoBid, "auctionCombo", new ComboBox<Auction>());
                set(autoBid, "maxBidField", new TextField());
                set(autoBid, "incrementField", new TextField());
                set(autoBid, "resultLabel", new Label());
                ((ComboBox<Auction>) get(autoBid, "auctionCombo")).getItems().add(auction("A1", Auction.AuctionStatus.RUNNING, "SELLER", null));
                ((ComboBox<Auction>) get(autoBid, "auctionCombo")).getSelectionModel().selectFirst();
                invoke(autoBid, "handleSetAutoBid", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                ((TextField) get(autoBid, "maxBidField")).setText("250000");
                ((TextField) get(autoBid, "incrementField")).setText("10000");
                invoke(autoBid, "handleSetAutoBid", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());

                LoginController login = new LoginController();
                set(login, "usernameField", new TextField());
                set(login, "passwordField", new PasswordField());
                set(login, "errorLabel", new Label());
                set(login, "loadingIndicator", new ProgressIndicator());
                set(login, "loginButton", new Button());
                set(login, "registerButton", new Button());
                login.initialize();
                invoke(login, "handleLogin", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());

                RegisterController register = new RegisterController();
                set(register, "fullNameField", new TextField());
                set(register, "usernameField", new TextField());
                set(register, "emailField", new TextField());
                set(register, "passwordField", new PasswordField());
                set(register, "messageLabel", new Label());
                set(register, "registerButton", new Button());
                register.initialize();
                invoke(register, "handleRegister", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                ((TextField) get(register, "fullNameField")).setText("Ann");
                ((TextField) get(register, "usernameField")).setText("ann");
                ((TextField) get(register, "emailField")).setText("ann@example.test");
                ((PasswordField) get(register, "passwordField")).setText("secret");
                invoke(register, "handleRegister", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void tableAndHistoryControllersInitializeWithFakeClient() throws Exception {
        runFxAndWait(() -> {
            try {
                ClientContext.getInstance().setCurrentUser(user("BIDDER"));
                setClient(new FakeAuctionClient());

                MyBidsController myBids = new MyBidsController();
                set(myBids, "table", new TableView<Auction>());
                set(myBids, "colName", new TableColumn<Auction, String>());
                set(myBids, "colMyBid", new TableColumn<Auction, String>());
                set(myBids, "colCurrent", new TableColumn<Auction, String>());
                set(myBids, "colStatus", new TableColumn<Auction, String>());
                set(myBids, "colResult", new TableColumn<Auction, String>());
                set(myBids, "summaryLabel", new Label());
                invoke(myBids, "setupTable");

                HistoryController history = new HistoryController();
                set(history, "allBidsList", new ListView<String>());
                set(history, "auctionFilter", new ComboBox<Auction>());
                NumberAxis xAxis = new NumberAxis();
                NumberAxis yAxis = new NumberAxis();
                set(history, "xAxis", xAxis);
                set(history, "priceLineChart", new LineChart<Number, Number>(xAxis, yAxis));
                set(history, "statsLabel", new Label());
                invoke(history, "renderChart", new Class<?>[] {Auction.class}, auction("A1", Auction.AuctionStatus.RUNNING, "SELLER", "BIDDER"));
                invoke(history, "animateHistoryList");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void mainAndSellerControllersExerciseNavigationAnimationAndSellerValidation() throws Exception {
        runFxAndWait(() -> {
            try {
                ClientContext.getInstance().setCurrentUser(user("BIDDER"));
                setClient(new FakeAuctionClient());

                MainController main = new MainController();
                set(main, "userNameLabel", new Label());
                set(main, "userRoleLabel", new Label());
                set(main, "statusLabel", new Label());
                set(main, "rootPane", new javafx.scene.layout.StackPane(new Button("child")));
                set(main, "mainPane", new javafx.scene.layout.BorderPane());
                set(main, "sidebar", new VBox());
                set(main, "chatPanel", new VBox());
                set(main, "chatToggleButton", new Button());
                set(main, "btnDashboard", new Button());
                set(main, "btnMyBids", new Button());
                set(main, "btnSeller", new Button());
                set(main, "btnAdmin", new Button());
                set(main, "btnHistory", new Button());
                set(main, "btnMySell", new Button());
                set(main, "btnDeposit", new Button());
                invoke(main, "setupChatbotAnimationState");
                invoke(main, "setChatbotVisible", new Class<?>[] {boolean.class}, true);
                invoke(main, "setChatbotVisible", new Class<?>[] {boolean.class}, false);
                invoke(main, "installButtonFeedback", new Class<?>[] {javafx.scene.Node.class}, get(main, "rootPane"));
                invoke(main, "setActiveButton", new Class<?>[] {Button.class}, get(main, "btnDashboard"));
                invoke(main, "animateContentIn", new Class<?>[] {javafx.scene.Node.class}, new VBox());
                assertTrue(((String) invoke(main, "formatRole", new Class<?>[] {User.class}, user("BIDDER"))).contains("GI"));
                main.setStatus("OK");

                SellerController seller = new SellerController();
                injectSellerFields(seller);
                seller.initialize();
                invoke(seller, "setupItemTable");
                invoke(seller, "setupAuctionTable");
                invoke(seller, "handleSetStartNow", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                invoke(seller, "handleAddOneHour", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                invoke(seller, "handleAddItem", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                ((TextField) get(seller, "itemNameField")).setText("Phone");
                ((TextField) get(seller, "itemDescField")).setText("Desc");
                ((TextField) get(seller, "itemPriceField")).setText("100000");
                invoke(seller, "handleAddItem", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                invoke(seller, "handleAddItemAndCreateAuction", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                @SuppressWarnings("unchecked")
                TableView<Item> itemTable = (TableView<Item>) get(seller, "itemTable");
                itemTable.getItems().add(auction("A1", Auction.AuctionStatus.OPEN, "BIDDER", null).getItem());
                itemTable.getSelectionModel().selectFirst();
                invoke(seller, "handleCreateAuction", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                @SuppressWarnings("unchecked")
                TableView<Auction> auctionTable = (TableView<Auction>) get(seller, "auctionTable");
                auctionTable.getItems().add(auction("A1", Auction.AuctionStatus.RUNNING, "BIDDER", null));
                auctionTable.getSelectionModel().selectFirst();
                invoke(seller, "handleEndAuction", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                invoke(seller, "clearItemForm");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void adminControllerCoversTabsTablesAndGuardBranches() throws Exception {
        runFxAndWait(() -> {
            try {
                ClientContext.getInstance().setCurrentUser(adminUser());
                setClient(new FakeAuctionClient());

                AdminController admin = new AdminController();
                injectAdminFields(admin);
                admin.initialize();

                invoke(admin, "handleTabAuction", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                invoke(admin, "handleTabUser", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                invoke(admin, "handleRefresh", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());

                @SuppressWarnings("unchecked")
                TableColumn<User, String> roleColumn = (TableColumn<User, String>) get(admin, "colUserRole");
                TableCell<User, String> roleCell = roleColumn.getCellFactory().call(roleColumn);
                invokeUpdateItem(roleCell, null, true);
                invokeUpdateItem(roleCell, "ADMIN", false);
                invokeUpdateItem(roleCell, "BIDDER", false);

                @SuppressWarnings("unchecked")
                TableColumn<Auction, String> statusColumn =
                        (TableColumn<Auction, String>) get(admin, "colAdminAucStatus");
                TableCell<Auction, String> statusCell = statusColumn.getCellFactory().call(statusColumn);
                invokeUpdateItem(statusCell, null, true);
                for (String status : List.of("RUNNING", "OPEN", "FINISHED", "PAID", "CANCELED", "PENDING")) {
                    invokeUpdateItem(statusCell, status, false);
                }

                assertEquals("", invoke(admin, "formatRole", new Class<?>[] {User.class}, (User) null));
                assertEquals("ADMIN", invoke(admin, "formatRole", new Class<?>[] {User.class}, adminUser()));
                assertTrue(((String) invoke(admin, "formatRole", new Class<?>[] {User.class}, user("BIDDER")))
                        .length() > 0);
                invoke(admin, "showStatus", new Class<?>[] {String.class, boolean.class}, "ok", true);
                invoke(admin, "showStatus", new Class<?>[] {String.class, boolean.class}, "bad", false);
                invoke(admin, "hideStatus");
                Label statusLabel = (Label) get(admin, "adminStatusLabel");
                set(admin, "adminStatusLabel", null);
                invoke(admin, "hideStatus");
                set(admin, "adminStatusLabel", statusLabel);

                @SuppressWarnings("unchecked")
                TableView<User> userTable = (TableView<User>) get(admin, "userTable");
                userTable.getSelectionModel().clearSelection();
                invoke(admin, "handleDeleteUser", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                userTable.getItems().setAll(adminUser(), user("BIDDER"));
                userTable.getSelectionModel().select(0);
                invoke(admin, "handleDeleteUser", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());

                @SuppressWarnings("unchecked")
                TableView<Auction> auctionTable = (TableView<Auction>) get(admin, "adminAucTable");
                auctionTable.getSelectionModel().clearSelection();
                invoke(admin, "handleEndSelectedAuction", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                invoke(admin, "handleDeleteAuction", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                auctionTable.getItems().setAll(
                        auction("A-FIN", Auction.AuctionStatus.FINISHED, "SELLER", "BIDDER"),
                        auction("A-CAN", Auction.AuctionStatus.CANCELED, "SELLER", null),
                        auction("A-PAID", Auction.AuctionStatus.PAID, "SELLER", "BIDDER"),
                        auction("A-RUN", Auction.AuctionStatus.RUNNING, "SELLER", "BIDDER"));
                for (int i = 0; i < 3; i++) {
                    auctionTable.getSelectionModel().select(i);
                    invoke(admin, "handleEndSelectedAuction", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
                }
                auctionTable.getSelectionModel().select(3);
                invoke(admin, "handleEndSelectedAuction", new Class<?>[] {javafx.event.ActionEvent.class}, new javafx.event.ActionEvent());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(300);
    }

    @Test
    void mySellControllerCoversLoadingLookupAndAnimationBranches() throws Exception {
        MySellController[] holder = new MySellController[1];
        runFxAndWait(() -> {
            try {
                ClientContext.getInstance().setCurrentUser(user("BIDDER"));
                setClient(new FakeAuctionClient());

                MySellController mySell = new MySellController();
                holder[0] = mySell;
                injectMySellFields(mySell);
                mySell.initialize();

                assertTrue(invoke(mySell, "getStartTime") instanceof LocalDateTime);
                ((DatePicker) get(mySell, "startDatePicker")).setValue(java.time.LocalDate.of(2026, 1, 2));
                assertEquals(2026, ((LocalDateTime) invoke(mySell, "getStartTime")).getYear());

                assertTrue(invoke(mySell, "getAuctionForItem", new Class<?>[] {Item.class}, (Item) null) == null);
                Item missingId = new Electronics(null, "No id", "Desc", 100000.0, "NEW", 100000.0,
                        "Dell", 12, "X");
                assertTrue(invoke(mySell, "getAuctionForItem", new Class<?>[] {Item.class}, missingId) == null);

                Button source = new Button("open");
                Item item = auction("A1", Auction.AuctionStatus.RUNNING, "BIDDER", null).getItem();
                invoke(mySell, "openAuctionForItem", new Class<?>[] {Item.class, Button.class}, item, source);

                @SuppressWarnings("unchecked")
                Map<String, Auction> auctionByItemId = (Map<String, Auction>) get(mySell, "auctionByItemId");
                auctionByItemId.put(item.getId(), auction("A1", Auction.AuctionStatus.RUNNING, "BIDDER", null));
                invoke(mySell, "openAuctionForItem", new Class<?>[] {Item.class, Button.class}, item, source);

                invoke(mySell, "animateTableRefresh");
                TableView<Item> table = (TableView<Item>) get(mySell, "itemTable");
                set(mySell, "itemTable", null);
                invoke(mySell, "setupRowMotion");
                invoke(mySell, "animateTableRefresh");
                set(mySell, "itemTable", table);
                invoke(mySell, "animateLaunch", new Class<?>[] {javafx.scene.Node.class}, new Button());
                invoke(mySell, "animateLaunch", new Class<?>[] {javafx.scene.Node.class}, (javafx.scene.Node) null);
                invoke(mySell, "animateRow", new Class<?>[] {javafx.scene.Node.class, double.class, double.class},
                        new Button(), 1.01, -2.0);
                invoke(mySell, "animateRow", new Class<?>[] {javafx.scene.Node.class, double.class, double.class},
                        (javafx.scene.Node) null, 1.0, 0.0);
                invoke(mySell, "shakeNode", new Class<?>[] {javafx.scene.Node.class}, new Button());
                invoke(mySell, "shakeNode", new Class<?>[] {javafx.scene.Node.class}, (javafx.scene.Node) null);
                invoke(mySell, "fmtVND", new Class<?>[] {double.class}, 125000.0);
                set(mySell, "colAction", null);
                invoke(mySell, "setupActionColumn");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(500);
        runFxAndWait(() -> assertTrue(((Label) get(holder[0], "summaryLabel")).getText().length() > 0));
    }

    @Test
    void chatbotControllerCoversStreamingStatusAndErrorBranches() throws Exception {
        ChatbotController[] successHolder = new ChatbotController[1];
        runFxAndWait(() -> {
            try {
                ClientContext.getInstance().setCurrentUser(user("BIDDER"));
                setClient(new FakeAuctionClient());

                ChatbotController chatbot = new ChatbotController();
                successHolder[0] = chatbot;
                injectChatbotFields(chatbot);
                set(chatbot, "chatClient", new StubGeminiChatClient(false));
                chatbot.initialize();

                ((TextField) get(chatbot, "messageField")).setText("   ");
                invoke(chatbot, "handleSend");
                ((TextField) get(chatbot, "messageField")).setText("gia hien tai?");
                invoke(chatbot, "handleSend");

                Label directBubble = (Label) invoke(chatbot, "addMessage",
                        new Class<?>[] {String.class, boolean.class}, "manual", true);
                invoke(chatbot, "appendToBubble", new Class<?>[] {Label.class, String.class}, directBubble, " text");
                invoke(chatbot, "queueAssistantChunk", new Class<?>[] {String.class}, (String) null);
                invoke(chatbot, "queueAssistantChunk", new Class<?>[] {String.class}, "");
                invoke(chatbot, "queueAssistantChunk", new Class<?>[] {String.class}, "chunk");
                invoke(chatbot, "flushPendingAssistantText", new Class<?>[] {Label.class}, directBubble);
                invoke(chatbot, "flushPendingAssistantText", new Class<?>[] {Label.class}, directBubble);
                invoke(chatbot, "setLoading", new Class<?>[] {boolean.class}, true);
                invoke(chatbot, "setLoading", new Class<?>[] {boolean.class}, false);
                invoke(chatbot, "stopStreamFlush");
                invoke(chatbot, "startStreamFlush", new Class<?>[] {Label.class}, directBubble);
                invoke(chatbot, "stopStreamFlush");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(400);
        runFxAndWait(() -> assertTrue(((VBox) get(successHolder[0], "messageList")).getChildren().size() >= 3));

        runFxAndWait(() -> {
            try {
                ChatbotController chatbot = new ChatbotController();
                injectChatbotFields(chatbot);
                set(chatbot, "chatClient", new StubGeminiChatClient(true));
                chatbot.initialize();
                ((TextField) get(chatbot, "messageField")).setText("fail");
                invoke(chatbot, "handleSend");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(400);
    }

    private static void injectAuctionFields(AuctionsController controller) throws Exception {
        set(controller, "listView", new VBox());
        set(controller, "detailPanel", new Region());
        set(controller, "statRunning", new Label());
        set(controller, "statTotal", new Label());
        set(controller, "statBids", new Label());
        set(controller, "auctionTable", new TableView<Auction>());
        set(controller, "colName", new TableColumn<Auction, String>());
        set(controller, "colCategory", new TableColumn<Auction, String>());
        set(controller, "colPrice", new TableColumn<Auction, String>());
        set(controller, "colStatus", new TableColumn<Auction, String>());
        set(controller, "colTimer", new TableColumn<Auction, String>());
        set(controller, "colLeader", new TableColumn<Auction, String>());
        set(controller, "upcomingCards", new TilePane());
        set(controller, "runningCards", new TilePane());
        set(controller, "pastCards", new TilePane());
        set(controller, "upcomingAllButton", new Button());
        set(controller, "runningAllButton", new Button());
        set(controller, "pastAllButton", new Button());
        set(controller, "statusLabel", new Label());
        set(controller, "detailName", new Label());
        set(controller, "detailTitleLarge", new Label());
        set(controller, "detailAssetCode", new Label());
        set(controller, "detailStartingPrice", new Label());
        set(controller, "detailStepPrice", new Label());
        set(controller, "detailDeposit", new Label());
        set(controller, "detailFee", new Label());
        set(controller, "detailStartTime", new Label());
        set(controller, "detailEndTime", new Label());
        set(controller, "detailAddress", new Label());
        set(controller, "detailType", new Label());
        set(controller, "countdownDays", new Label());
        set(controller, "countdownHours", new Label());
        set(controller, "countdownMinutes", new Label());
        set(controller, "countdownSeconds", new Label());
        ImageView detailImage = new ImageView();
        new VBox(detailImage);
        set(controller, "detailImage", detailImage);
        set(controller, "detailPrice", new Label());
        set(controller, "detailTimer", new Label());
        set(controller, "detailStatus", new Label());
        set(controller, "detailLeader", new Label());
        set(controller, "detailDesc", new Label());
        set(controller, "detailAntiSnipe", new Label());
        set(controller, "countdownTitle", new Label());
        set(controller, "bidResultLabel", new Label());
        set(controller, "auctionLiveBox", new VBox());
        set(controller, "auctionSuccessBox", new VBox());
        set(controller, "currentInfoBox", new VBox());
        set(controller, "successStartingPrice", new Label());
        set(controller, "successWinningPrice", new Label());
        set(controller, "successWinnerCode", new Label());
        set(controller, "bidAmountField", new TextField());
        set(controller, "placeBidButton", new Button());
        set(controller, "autoMaxBidField", new TextField());
        set(controller, "autoIncrementField", new TextField());
        set(controller, "autoBidButton", new Button());
        set(controller, "cancelAutoBidButton", new Button());
        set(controller, "autoBidResultLabel", new Label());
        VBox chartBox = new VBox();
        new VBox(chartBox);
        set(controller, "chartBox", chartBox);
        set(controller, "bidHistoryList", new ListView<String>());
    }

    private static void injectSellerFields(SellerController seller) throws Exception {
        set(seller, "itemTypeCombo", new ComboBox<String>());
        set(seller, "itemCondCombo", new ComboBox<String>());
        set(seller, "itemNameField", new TextField());
        set(seller, "itemDescField", new TextField());
        set(seller, "itemPriceField", new TextField());
        set(seller, "itemTable", new TableView<Item>());
        set(seller, "colItemName", new TableColumn<Item, String>());
        set(seller, "colItemCat", new TableColumn<Item, String>());
        set(seller, "colItemPrice", new TableColumn<Item, String>());
        set(seller, "colItemCond", new TableColumn<Item, String>());
        set(seller, "selectedImageLabel", new Label());
        set(seller, "selectedImagePreview", new ImageView());
        set(seller, "startDatePicker", new DatePicker());
        set(seller, "startHourSpinner", new Spinner<Integer>());
        set(seller, "startMinSpinner", new Spinner<Integer>());
        set(seller, "endDatePicker", new DatePicker());
        set(seller, "endHourSpinner", new Spinner<Integer>());
        set(seller, "endMinSpinner", new Spinner<Integer>());
        set(seller, "antiSnipeCheck", new CheckBox());
        set(seller, "auctionTable", new TableView<Auction>());
        set(seller, "colAucName", new TableColumn<Auction, String>());
        set(seller, "colAucStatus", new TableColumn<Auction, String>());
        set(seller, "colAucPrice", new TableColumn<Auction, String>());
        set(seller, "sellerResultLabel", new Label());
    }

    private static void injectAdminFields(AdminController admin) throws Exception {
        set(admin, "totalUsersLabel", new Label());
        set(admin, "totalAuctionsLabel", new Label());
        set(admin, "totalBidsLabel", new Label());
        set(admin, "adminStatusLabel", new Label());
        set(admin, "tabUserBtn", new Button());
        set(admin, "tabAucBtn", new Button());
        set(admin, "panelUsers", new VBox());
        set(admin, "panelAuctions", new VBox());
        set(admin, "userTable", new TableView<User>());
        set(admin, "colUserName", new TableColumn<User, String>());
        set(admin, "colUserRole", new TableColumn<User, String>());
        set(admin, "colUserEmail", new TableColumn<User, String>());
        set(admin, "adminAucTable", new TableView<Auction>());
        set(admin, "colAdminAucName", new TableColumn<Auction, String>());
        set(admin, "colAdminAucStatus", new TableColumn<Auction, String>());
        set(admin, "colAdminAucPrice", new TableColumn<Auction, String>());
        set(admin, "colAdminAucBids", new TableColumn<Auction, String>());
    }

    private static void injectMySellFields(MySellController mySell) throws Exception {
        set(mySell, "startDatePicker", new DatePicker());
        set(mySell, "startHourSpinner", new Spinner<Integer>(0, 23, 9));
        set(mySell, "startMinSpinner", new Spinner<Integer>(0, 59, 30));
        set(mySell, "endDatePicker", new DatePicker());
        set(mySell, "endHourSpinner", new Spinner<Integer>(0, 23, 10));
        set(mySell, "endMinSpinner", new Spinner<Integer>(0, 59, 30));
        set(mySell, "antiSnipeCheck", new CheckBox());
        set(mySell, "itemTable", new TableView<Item>());
        set(mySell, "colItemName", new TableColumn<Item, String>());
        set(mySell, "colItemType", new TableColumn<Item, String>());
        set(mySell, "colItemPrice", new TableColumn<Item, String>());
        set(mySell, "colItemCPrice", new TableColumn<Item, String>());
        set(mySell, "colItemStatus", new TableColumn<Item, String>());
        set(mySell, "colAction", new TableColumn<Item, Void>());
        set(mySell, "summaryLabel", new Label());
    }

    private static void injectChatbotFields(ChatbotController chatbot) throws Exception {
        set(chatbot, "chatScrollPane", new ScrollPane());
        set(chatbot, "messageList", new VBox());
        set(chatbot, "messageField", new TextField());
        set(chatbot, "sendButton", new Button());
        set(chatbot, "statusLabel", new Label());
    }

    private static Auction auction(String id, Auction.AuctionStatus status, String sellerId, String leadBidderId) {
        Electronics item = new Electronics("ITEM-" + id, "Laptop " + id, "Desc", 100000.0, "NEW", 100000.0,
                "Dell", 24, "G15");
        item.setSellerId(sellerId);
        Auction auction = new Auction(id, item, LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(30), leadBidderId == null ? null : "Ann",
                leadBidderId, true, 30, 60);
        auction.setStatus(status);
        auction.setBidHistory(List.of(
                new BidTransaction("B1", id, "BIDDER", "Ann", 150000.0, LocalDateTime.now().minusMinutes(2), false),
                new BidTransaction("B2", id, "OTHER", "Binh", 175000.0, LocalDateTime.now().minusMinutes(1), true)
        ));
        auction.setCurrentPrice(175000.0);
        return auction;
    }

    private static User user(String id) {
        return new Bidder(id, "ann", "ann@example.test", "Ann", "secret", 1000000.0, 2, 0);
    }

    private static User adminUser() {
        return new com.auction.shared.model.User.Admin("ADMIN", "root", "root@example.test", "secret", "Root");
    }

    private static void setClient(AuctionClient client) throws Exception {
        Field field = ClientContext.class.getDeclaredField("client");
        field.setAccessible(true);
        field.set(ClientContext.getInstance(), client);
    }

    private static void runFxAndWait(ThrowingRunnable runnable) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] failure = new Throwable[1];
        Platform.runLater(() -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                failure[0] = throwable;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        if (failure[0] instanceof Exception exception) throw exception;
        if (failure[0] != null) throw new AssertionError(failure[0]);
    }

    private static Object invoke(Object target, String name) throws Exception {
        return invoke(target, name, new Class<?>[0]);
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static void set(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object get(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void invokeUpdateItem(Object cell, Object value, boolean empty) throws Exception {
        Class<?> type = cell.getClass();
        while (type != null) {
            for (Method candidate : type.getDeclaredMethods()) {
                if ("updateItem".equals(candidate.getName()) && candidate.getParameterCount() == 2) {
                    candidate.setAccessible(true);
                    candidate.invoke(cell, value, empty);
                    return;
                }
            }
            type = type.getSuperclass();
        }
        throw new NoSuchMethodException("updateItem");
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class StubGeminiChatClient extends com.auction.client.controller.ai.GeminiChatClient {
        private final boolean fail;

        private StubGeminiChatClient(boolean fail) {
            super("unit-test-key", "unit-test-model");
            this.fail = fail;
        }

        @Override
        public boolean hasApiKey() {
            return true;
        }

        @Override
        public void sendMessage(String userText, String requestContext, Consumer<String> onChunk) {
            if (fail) {
                throw new IllegalStateException("forced failure");
            }
            onChunk.accept("xin ");
            onChunk.accept("chao");
        }
    }

    private static final class FakeAuctionClient extends AuctionClient {
        private FakeAuctionClient() {
            super("test", 1);
        }

        @Override public boolean isConnected() { return true; }
        @Override public boolean connect() { return true; }
        @Override public void setBroadcastListener(java.util.function.Consumer<SocketMessage> listener) {}

        @Override
        public SocketMessage getAllAuctions() {
            return SocketMessage.ok(SocketMessage.Action.GET_ALL_AUCTIONS, "OK")
                    .put("auctions", List.of(
                            auction("A1", Auction.AuctionStatus.RUNNING, "SELLER", "BIDDER"),
                            auction("A2", Auction.AuctionStatus.FINISHED, "SELLER", "BIDDER")));
        }

        @Override
        public SocketMessage getAuction(String auctionId) {
            if ("MISSING".equals(auctionId)) {
                return SocketMessage.error(SocketMessage.Action.GET_AUCTION, "missing");
            }
            return SocketMessage.ok(SocketMessage.Action.GET_AUCTION, "OK")
                    .put("auction", auction(auctionId, Auction.AuctionStatus.RUNNING, "SELLER", "BIDDER"));
        }

        @Override
        public SocketMessage placeBid(String auctionId, double amount) {
            if (amount == 170000.0) {
                return SocketMessage.ok(SocketMessage.Action.PLACE_BID, "OK")
                        .put("user", user("BIDDER"));
            }
            if (amount == 180000.0) {
                return SocketMessage.error(SocketMessage.Action.PLACE_BID, "too low");
            }
            return SocketMessage.ok(SocketMessage.Action.PLACE_BID, "OK")
                    .put("bidId", "BID-NEW")
                    .put("auction", auction(auctionId, Auction.AuctionStatus.RUNNING, "SELLER", "BIDDER"))
                    .put("user", user("BIDDER"));
        }

        @Override public SocketMessage registerAutoBid(String auctionId, double maxBid, double increment) {
            return SocketMessage.ok(SocketMessage.Action.REGISTER_AUTO_BID, "OK").put("user", user("BIDDER"));
        }

        @Override public SocketMessage cancelAutoBid(String auctionId) {
            return SocketMessage.ok(SocketMessage.Action.CANCEL_AUTO_BID, "OK");
        }

        @Override public SocketMessage depositUser(String userId, double amount) {
            return SocketMessage.ok(SocketMessage.Action.DEPOSIT_USER, "OK").put("user", user(userId));
        }

        @Override public SocketMessage register(String userType, String username, String email, String password, String fullname) {
            return SocketMessage.error(SocketMessage.Action.REGISTER, "duplicate");
        }

        @Override
        public SocketMessage getMyBids() {
            return SocketMessage.ok(SocketMessage.Action.GET_MY_BIDS, "OK")
                    .put("auctions", List.of(auction("A1", Auction.AuctionStatus.PAID, "SELLER", "BIDDER")))
                    .put("myBestBids", Map.of("A1", 150000.0));
        }

        @Override public SocketMessage createItem(Object item) {
            return SocketMessage.ok(SocketMessage.Action.CREATE_ITEM, "OK").put("itemId", "ITEM-NEW");
        }

        @Override public SocketMessage createAuction(Object item, LocalDateTime startTime, LocalDateTime endTime,
                boolean antiSnipingEnabled, int snipeWindowSeconds, int snipeExtendSeconds) {
            return SocketMessage.ok(SocketMessage.Action.CREATE_AUCTION, "OK").put("auctionId", "AUC-NEW");
        }

        @Override public SocketMessage getMyItems() {
            return SocketMessage.ok(SocketMessage.Action.GET_ITEMS_BY_SELLER, "OK")
                    .put("items", List.of(auction("A1", Auction.AuctionStatus.OPEN, "BIDDER", null).getItem()));
        }

        @Override public SocketMessage getMySellerAuctions() {
            return SocketMessage.ok(SocketMessage.Action.GET_MY_SELLER_AUCTIONS, "OK")
                    .put("auctions", List.of(auction("A1", Auction.AuctionStatus.RUNNING, "BIDDER", null)));
        }

        @Override public SocketMessage finishAuction(String auctionId) {
            return SocketMessage.ok(SocketMessage.Action.FINISH_AUCTION, "OK");
        }

        @Override public SocketMessage deleteAuction(String auctionId) {
            return SocketMessage.ok(SocketMessage.Action.DELETE_AUCTION, "OK");
        }

        @Override public SocketMessage getAllUsers() {
            return SocketMessage.ok(SocketMessage.Action.GET_ALL_USERS, "OK")
                    .put("users", List.of(adminUser(), user("BIDDER")));
        }

        @Override public SocketMessage deleteUser(String userId) {
            return SocketMessage.ok(SocketMessage.Action.DELETE_USER, "OK");
        }
    }
}
