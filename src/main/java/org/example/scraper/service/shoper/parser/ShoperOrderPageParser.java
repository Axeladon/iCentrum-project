package org.example.scraper.service.shoper.parser;

import org.example.scraper.model.Address;
import org.example.scraper.model.OrderNotes;
import org.example.scraper.service.utils.PolishDateUtil;
import org.example.scraper.service.utils.PriceUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShoperOrderPageParser {
    private static final Pattern ZIP_PATTERN = Pattern.compile("\\b\\d{2}-\\d{3}\\b");

    private final Document page;

    public ShoperOrderPageParser(Document orderPage) {
        this.page = orderPage;
    }

    public String extractClientName() {
        return Objects.requireNonNull(
                page.select("ul.list > li > span.js__copy-on-click.color_on-hover-highlight-bg").first()
        ).text();
    }

    public String extractPaymentStatus() {
        return page.select("div#order-paid-field a").text();
    }

    public String extractPaymentMethod() {
        Elements paymentItems = page.select("li");
        for (Element li : paymentItems) {
            String liText = li.text();
            if (liText.contains("Płatność")) {
                Element strong = li.selectFirst("strong");
                if (strong != null && !strong.text().isBlank()) {
                    return strong.text().trim();
                }
            }
        }
        return "";
    }

    public String extractEmail() {
        Element mailLink = page.selectFirst("a[href^=mailto:]");
        return mailLink != null
                ? mailLink.attr("href").replaceFirst("(?i)^mailto:", "").trim()
                : "Null";
    }

    public Address extractAddress() {
        Elements addressSpans = page.select("ul.list li span.js__copy-on-click.color_on-hover-highlight-bg");

        String streetAndNumber = "";
        String postalCode = "";
        String city = "";
        String country = "";

        for (int i = 0; i < addressSpans.size(); i++) {
            String text = addressSpans.get(i).text();
            if (ZIP_PATTERN.matcher(text).find()) {
                postalCode = text; // ZIP code
                if (i - 1 >= 0) streetAndNumber = addressSpans.get(i - 1).text(); // street before ZIP
                if (i + 1 < addressSpans.size()) city = addressSpans.get(i + 1).text(); // city after ZIP
                if (i + 2 < addressSpans.size()) country = "PL"; // normalize to PL
                break;
            }
        }
        return new Address(streetAndNumber, postalCode, city, country);
    }

    public BigDecimal extractPrice() {
        String price = page.select(
                "span.color_dark.size_xxl.color_highlight-1, " +
                        "span.color_dark.size_xxl.color_highlight-2, " +
                        "span.color_dark.size_xxl.color_highlight-3"
        ).text();
        return PriceUtils.parsePrice(price);
    }

    public String extractParcelMachine() {
        Element firstPickupPoint = page.select("span[data-test-id=pickup-point-point]").first();
        return (firstPickupPoint != null) ? firstPickupPoint.text() : "";
    }

    public String extractNip() {
        Element nipElement = page.select("div.message-box__content strong.js__copy-on-click").first();
        return (nipElement != null) ? nipElement.text() : "";
    }

    public String extractCompanyName() {

        String html = page.html();

        Pattern pattern = Pattern.compile(
                "Nazwa firmy:\\s*<span class=\"js__copy-on-click color_on-hover-highlight-bg\">(.*?)</span>",
                Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(html);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    /** Declared shipping date (kept as original "dd month yyyy" text) */
    public LocalDate extractDeclaredShippingDate() {
        Element element = page.selectFirst("span.size_xs:contains(Deklarowana data wysyłki)");

        Matcher matcher = Pattern.compile("\\(([^)]+)\\)").matcher(element.text());
        return matcher.find()
                ? PolishDateUtil.toLocalDate(matcher.group(1))
                : null;
    }

    public boolean extractPersonalPickup() {
        return page.text().contains("Odbiór osobisty");
    }

    /** Returns LocalDate.EPOCH if date is not found or invalid */
    public LocalDate extractOrderSubmissionDate() {
        Element li = page.selectFirst("li:contains(Data złożenia zamówienia)");
        if (li == null) return LocalDate.EPOCH;

        Element strong = li.selectFirst("strong.color_dark");
        if (strong == null) return LocalDate.EPOCH;

        String date = strong.text();
        if (date.matches("\\d{1,2}\\s+\\p{L}+\\s+\\d{4}(?:\\s+\\d{2}:\\d{2})?")) {
            return PolishDateUtil.toLocalDate(date);
        }
        return LocalDate.EPOCH;
    }

    public OrderNotes extractOrderNotes() {
        Element notesElement = page.selectFirst("order-notes");
        String text = notesElement.attr(":notes");

        Pattern pattern = Pattern.compile("content:\\s*'(.*?)',\\s*emptyContent:");
        Matcher matcher = pattern.matcher(text);

        OrderNotes notes = new OrderNotes();

        if (matcher.find()) notes.setClientNote(matcher.group(1));
        if (matcher.find()) notes.setAdminPrivateNote(matcher.group(1));
        if (matcher.find()) notes.setAdminPublicNote(matcher.group(1));

        return notes;
    }
}
