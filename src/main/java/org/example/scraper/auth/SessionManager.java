package org.example.scraper.auth;

import org.example.scraper.model.SiteId;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.Map;

public class SessionManager {
    private static volatile SessionManager instance;

    private final CookieManager cookieManager = new CookieManager();
    private static final String DEFAULT_ORDER_NUMBER = "123456";
    private static final String ORDER_PAGE_URL_PREFIX = "https://applecentrum-612788.shoparena.pl/admin/orders/view/id/";
    private static final String LOGIN_URL = "https://applecentrum-612788.shoparena.pl/admin/auth/login";

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    public void login(String login, String password) {
        logCookies("Before login");
        try {
            Connection.Response loginResponse = Jsoup.connect(LOGIN_URL)
                    .method(Connection.Method.POST)
                    .cookies(getLoginCookies())
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .data("redirect", "")
                    .data("login", login)
                    .data("password", password)
                    .execute();

            if (loginResponse.statusCode() == 200 || loginResponse.statusCode() == 302) {
                cookieManager.update(SiteId.SHOPER, loginResponse.cookies());
            }
            logCookies("After login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendSmsCode(String url, String code) throws Exception {
        logCookies("Before sending 2FA");
        System.out.println("Sending 2FA code to the server...");

        Connection.Response response = Jsoup.connect(url)
                .method(Connection.Method.POST)
                .cookies(cookieManager.getAll(SiteId.SHOPER))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Referer", ORDER_PAGE_URL_PREFIX + DEFAULT_ORDER_NUMBER)
                .data("redirect", "")
                .data("code", code)
                .data("trust", "1")
                .execute();

        if (response.statusCode() == 200 || response.statusCode() == 302) {
            System.out.println("2FA verification successful!");
            logCookies("After sending 2FA");
        } else {
            throw new Exception("2FA verification failed! Status code: " + response.statusCode());
        }
        cookieManager.update(SiteId.SHOPER, response.cookies());
    }

    public Document getPage(String url) throws Exception {
        logCookies("Before getPage: " + url);
        Connection.Response pageResponse = Jsoup.connect(url)
                .cookies(cookieManager.getAll(SiteId.SHOPER))
                .method(Connection.Method.GET)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...")
                .header("Referer", "https://applecentrum-612788.shoparena.pl/admin/dashboard")
                .execute();

        if (pageResponse.statusCode() == 200) {
            System.out.println("Page " + url + " successfully loaded!");
            logCookies("After getPage: " + url);
        } else {
            throw new Exception("Failed to load page! Status code: " + pageResponse.statusCode());
        }
        return pageResponse.parse();
    }

    public AccessStatus getAccessStatus(Document page) throws Exception {
        if (isAccess(page)) {
            System.out.println("Status: SUCCESS");
            return AccessStatus.SUCCESS;
        } else {
            if (isLoginPage(page)) {
                System.out.println("Status: LOGIN_REQUIRED");
                return AccessStatus.LOGIN_REQUIRED;
            } else if (isTwoFAPage(page)){
                System.out.println("Status: TWO_FACTOR_REQUIRED");
                return AccessStatus.TWO_FACTOR_REQUIRED;
            } else {
                System.out.println("Status: ERROR");
                return AccessStatus.ERROR;
            }
        }
    }

    private boolean isLoginPage(Document doc) {
        String text = doc.body().text().toLowerCase();
        return text.contains("zaloguj się");
    }

    private boolean isAccess(Document doc) {
        String text = doc.body().text().toLowerCase();
        return text.contains("podgląd zamówienia") || text.contains("icentrumsklep.pl");
    }

    private boolean isTwoFAPage(Document doc) {
        String text = doc.body().text().toLowerCase();
        return text.contains("kod z sms-a");
    }

    public void logCookies(String message) {
        System.out.println("[" + message + "]");
        cookieManager.getAll(SiteId.SHOPER).forEach((k, v) -> System.out.println(k + ": " + (v.length() > 80 ? v.substring(0, 80) + "..." : v)));
    }

    public Map<String, String> getLoginCookies() {
        Map<String, String> all = cookieManager.getAll(SiteId.SHOPER);
        Map<String, String> filtered = new HashMap<>();

        all.forEach((k, v) -> {
            if (k.equals("admin_ip_verify") || k.equals("ic") || k.startsWith("ca276")) {
                filtered.put(k, v);
            }
        });

        return filtered;
    }
}