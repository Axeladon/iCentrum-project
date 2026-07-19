package org.example.scraper.service.shoper;

import org.example.scraper.auth.SessionManager;
import org.example.scraper.exception.ShoperDataFetchException;
import org.example.scraper.model.dto.ShoperOrderPagesDto;
import org.jsoup.nodes.Document;

public class ShoperDataFetcher {

    private static final String ORDER_URL = "https://applecentrum-612788.shoparena.pl/admin/orders/view/id/";
    private static final String ORDER_TABLE_URL = "https://applecentrum-612788.shoparena.pl/admin/orders/viewTable/id/";
    private static final String ORDER_CUSTOMER_URL = "https://applecentrum-612788.shoparena.pl/admin/customers/edit/id/";

    public static Document getOrderPage(String orderNumber) {
        return fetchPage(
                ORDER_URL + orderNumber,
                "Failed to load Shoper view page. Order: " + orderNumber
        );
    }

    public static Document getDeviceTablePage(String orderNumber) {
        return fetchPage(
                ORDER_TABLE_URL + orderNumber,
                "Failed to load Shoper view table page. Order: " + orderNumber
        );
    }

    public static Document getCustomerPage(String orderNumber) {
        return fetchPage(
                ORDER_CUSTOMER_URL + orderNumber,
                "Failed to load Shoper customer page. Order: " + orderNumber
        );
    }

    public static ShoperOrderPagesDto getShoperOrderPages(String orderNumber) {
        Document orderPage = getOrderPage(orderNumber);
        Document deviceTablePage = getDeviceTablePage(orderNumber);
        Document paymentPage = getCustomerPage(orderNumber);
        return new ShoperOrderPagesDto(orderPage, deviceTablePage, paymentPage);
    }

    private static Document fetchPage(String url, String errorMessage) {
        try {
            return SessionManager.getInstance().getPage(url);
        } catch (Exception e) {
            throw new ShoperDataFetchException(errorMessage, e);
        }
    }
}
