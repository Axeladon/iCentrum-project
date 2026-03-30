package org.example.scraper.ui.views;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.example.scraper.dto.InvoiceResponseDto;
import org.example.scraper.model.Order;
import org.example.scraper.service.*;
import org.example.scraper.service.fs.InfoFileManager;

import java.nio.file.Path;
import java.util.List;

public class PackageView {
    private final VBox root = new VBox(10);

    //todo hide apitoken
    private String token = "TOKEN";

    public PackageView() {

        root.setPadding(new Insets(10));

        Button testButton = new Button("Create invoice");

        testButton.setOnAction(e -> {

            try {
                PackageService packageService = new PackageService();
                String imei = packageService.getImei();
                String orderNumber = packageService.getOrderNumber(imei);

                Order order = packageService.getNewOrder(orderNumber);

                InvoiceGenerator invoiceGenerator = new InvoiceGenerator(token);

                //invoiceGenerator.generatePhoneMarginAndAccessoriesInvoice(order); // zaliczka

                List<InvoiceResponseDto> invoiceResponseDtos = invoiceGenerator.generatePhoneMarginAndAccessoriesInvoice(order);
                for (InvoiceResponseDto irdto : invoiceResponseDtos) {
                    if (irdto.getKod().equals("1")) {
                        OpenBrowser.openFakturaxlPage(irdto.getDokumentId()); //open a newly created invoice in the browser
                    } else {
                        showError(translateErrorCode(irdto.getKod()));
                    }

                }

                Path threeUToolsDir = new ThreeUToolsService().getSelectedDirectoryOrThrow();
                new InfoFileManager().deleteAllInfoFiles(threeUToolsDir);

                Thread.sleep(1000);

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        root.getChildren().addAll(
                new Label("The window is under development"),
                testButton
        );
    }

    private void showError(String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error!");
        alert.setContentText(errorMessage);

        alert.showAndWait();
    }

    private String translateErrorCode(String errorCode) {
        switch (errorCode) {
            case "10":
                return "NIP is not valid";
            default:
                return "Unknown error code: " + errorCode;
        }
    }

    public VBox getRoot() { return root; }
}
