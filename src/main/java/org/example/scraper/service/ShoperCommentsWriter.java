package org.example.scraper.service;

import org.example.scraper.auth.CookieManager;
import org.example.scraper.model.SiteId;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

public class ShoperCommentsWriter {
    private static final String ORDER_NOTE_URL_PREFIX =
            "https://applecentrum-612788.shoparena.pl/console/orders/changenotes/id/";
    private static final CookieManager cookieManager = CookieManager.getInstance();

    // Adds private note to order
    public static void addPrivateOrderNote(String orderId, String noteText) throws Exception {
        // Always trim spaces from orderId
        String cleanOrderId = orderId.trim();

        String url = ORDER_NOTE_URL_PREFIX + cleanOrderId + "/ispriv/1";
        System.out.println("addPrivateOrderNote URL = " + url);

        Connection.Response response = Jsoup.connect(url)
                .ignoreHttpErrors(true) // let us see real status/body instead of throwing immediately
                .ignoreContentType(true) // response is JSON, not HTML
                .method(Connection.Method.POST)
                .cookies(cookieManager.getAll(SiteId.SHOPER)) // admin session cookies
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...")
                .header("Accept", "application/json, text/plain, */*")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Origin", "https://applecentrum-612788.shoparena.pl")
                .header("Referer",
                        "https://applecentrum-612788.shoparena.pl/admin/orders/view/id/" + cleanOrderId)
                .header("X-Requested-With", "XMLHttpRequest")
                .data("notes", noteText)
                .execute();

        int status = response.statusCode();
        System.out.println("addPrivateOrderNote status = " + status);
        System.out.println("addPrivateOrderNote final URL = " + response.url());
        System.out.println("addPrivateOrderNote body = " + response.body());

        if (status == 200) {
            cookieManager.update(SiteId.SHOPER, response.cookies());
        } else {
            throw new Exception("Failed to add note! Status code: "
                    + status + ", body: " + response.body());
        }
    }
}
