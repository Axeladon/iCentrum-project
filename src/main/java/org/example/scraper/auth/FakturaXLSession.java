package org.example.scraper.auth;

import org.example.scraper.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FakturaXLSession {

    private static final String API_URL = "https://program.fakturaxl.pl/api/dokument_dodaj.php";
    private static final BigDecimal CHARGER_GROSS_PRICE_PLN = new BigDecimal("100.00");

    private static final long MIN_ISSUE_GAP_MS = 2100L;
    private static final int MAX_RETRY_ATTEMPTS = 4;
    private static final long INITIAL_BACKOFF_MS = 1200L;

    private static final Pattern KOD_OK_PATTERN = Pattern.compile("<kod>\\s*1\\s*</kod>");
    private static final Pattern NR_PATTERN = Pattern.compile("<dokument_nr>(.*?)</dokument_nr>", Pattern.DOTALL);
    private static final Pattern ID_PATTERN = Pattern.compile("<dokument_id>(.*?)</dokument_id>", Pattern.DOTALL);

    // Quantity string used when single item
    private static final String DEFAULT_QTY_STR = "1.000";

    private final long minIssueGapMs;
    private final int maxRetryAttempts;
    private final long initialBackoffMs;
    private final HttpClient httpClient;
    private final String apiToken;

    // 'volatile' is redundant because ensureMinIssueGap() is synchronized
    private long lastIssueAt = 0L;

    private enum InvoiceKind {
        MARGIN,
        VAT_23
    }

    public FakturaXLSession(String apiToken) {
        this.apiToken = apiToken;
        this.minIssueGapMs = MIN_ISSUE_GAP_MS;
        this.maxRetryAttempts = MAX_RETRY_ATTEMPTS;
        this.initialBackoffMs = INITIAL_BACKOFF_MS;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Creates a margin advance invoice and (optionally) a separate VAT-23 invoice for chargers.
     * Returns a list with one or two invoice identifiers (numbers or raw XML on failure).
     */
    public List<String> createMarginAndVatInvoice(Order order, String orderId) {
        if (order == null) return List.of();

        // Adjust order once and get charger count in the same pass
        AdjustedOrder adjusted = buildOrderWithPerPhoneMinus100ForChargers(order);

        // Margin advance invoice (no aggregation, no total override).
        String marginRespXml = createMarginAdvanceInvoice(adjusted.order, orderId);
        String marginNo = extractInvoiceIdentifier(marginRespXml);

        String vatNo = null;
        if (adjusted.chargerCount > 0) {
            BigDecimal vatTotal = CHARGER_GROSS_PRICE_PLN.multiply(BigDecimal.valueOf(adjusted.chargerCount));

            // VAT-23 invoice aggregated by chargerCount with explicit total override.
            String vatRespXml = createVatAdvanceInvoiceAggregated(
                    order,                 // reuse buyer data
                    orderId,
                    adjusted.chargerCount, // aggregatedQty
                    vatTotal               // overrideTotalToPay
            );
            vatNo = extractInvoiceIdentifier(vatRespXml);
        }
        return (vatNo == null) ? List.of(marginNo) : List.of(marginNo, vatNo);
    }

    /* =========================
       High-level convenience API
       ========================= */

    /**
     * Margin advance invoice, default behavior (no aggregation, no overrides).
     */
    private String createMarginAdvanceInvoice(Order order, String orderId) {
        return createAdvanceInvoice(order, orderId, InvoiceKind.MARGIN, Department.BIURO_MARZA);
    }

    /**
     * VAT-23 advance invoice aggregated by quantity with explicit total override.
     */
    private String createVatAdvanceInvoiceAggregated(Order order, String orderId, int aggregatedQty, BigDecimal totalToPay) {
        return createAdvanceInvoice(order, orderId, InvoiceKind.VAT_23, Department.BIURO_VAT, aggregatedQty, totalToPay);
    }

    /**
     * Overload: default behavior (no aggregation, no total override).
     * This avoids semantic nulls at call-sites.
     */
    private String createAdvanceInvoice(Order order, String orderId, InvoiceKind kind, Department department) {
        return createAdvanceInvoiceCore(order, orderId, kind, department, null, null);
    }

    /**
     * Overload: aggregated line with explicit total override.
     * This avoids semantic nulls at call-sites.
     */
    private String createAdvanceInvoice(Order order, String orderId, InvoiceKind kind, Department department,
                                        int aggregatedQty, BigDecimal overrideTotalToPay) {
        return createAdvanceInvoiceCore(order, orderId, kind, department, aggregatedQty, overrideTotalToPay);
    }

    /* =========================
       Core implementation
       ========================= */

    /**
     * Core method. Nullable boxed params are used internally only.
     * - aggregatedQty == null => do not aggregate, use items
     * - overrideTotalToPay == null => compute from order
     */
    private String createAdvanceInvoiceCore(
            Order order,
            String orderId,
            InvoiceKind kind,
            Department department,
            Integer aggregatedQty,            // nullable internally
            BigDecimal overrideTotalToPay     // nullable internally
    ) {
        try {
            LocalDate sale = (order != null && order.getOrderSubmissionDate() != null)
                    ? order.getOrderSubmissionDate()
                    : LocalDate.now();

            String issueDate = LocalDate.now().toString();
            String saleDate = sale.toString();
            String dueDate = sale.plusDays(7).toString();

            BigDecimal totalToPay = (overrideTotalToPay != null)
                    ? overrideTotalToPay
                    : (order != null ? order.getTotalPrice() : BigDecimal.ZERO);
            if (totalToPay == null) totalToPay = BigDecimal.ZERO;

            Xml xml = Xml.start()
                    .tag("api_token", apiToken)
                    .tag("typ_faktury", "11")
                    .tag("typ_faktur_podtyp", "0")
                    .tag("rodzaj_faktury_koszty", "1")
                    .tag("obliczaj_sume_wartosci_faktury_wg", "0")
                    .tag("data_wystawienia", issueDate)
                    .tag("data_sprzedazy", saleDate)
                    .tag("termin_platnosci_data", dueDate)
                    .tag("miejsce_wystawienia", "Wrocław")
                    .tag("waluta", "PLN")
                    .tag("rodzaj_platnosci", "Przelew")
                    .tag("id_dzialy_firmy", String.valueOf(department.getId()));

            if (kind == InvoiceKind.MARGIN) {
                xml.tag("uwagi", "Procedura marży - towary używane")
                        .tag("typ_marzy", "2")
                        .tag("wartosc_marzy_brutto", "0.00")
                        .tag("marza_stawka_vat", "0")
                        .tag("JPK_V7", "MR_UZ");
            }

            xml.tag("status", detectPaymentStatus(order))
                    .tag("kwota_oplacona", totalToPay.toPlainString())
                    .tag("data_oplacenia", saleDate);

            appendBuyer(xml, Buyer.fromOrder(order));

            // Aggregated, VAT-23 only
            if (kind == InvoiceKind.VAT_23 && aggregatedQty != null && aggregatedQty > 0) {
                appendSingleLine(xml, kind, orderId, totalToPay, aggregatedQty);
                return postXmlWithRetries(xml.build());
            }

            List<PhoneModel> items = order.getPhoneModelList();
            if (items == null || items.isEmpty()) {
                appendSingleLine(xml, kind, orderId, totalToPay, 1);
            } else {
                for (PhoneModel ph : items) {
                    xml.open("faktura_pozycje");
                    if (kind == InvoiceKind.MARGIN) {
                        writeMarginLine(xml, ph, orderId);
                    } else {
                        writeVatLine(xml, ph, orderId);
                    }
                    xml.close("faktura_pozycje");
                }
            }
            return postXmlWithRetries(xml.build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create invoice (" + kind + ")", e);
        }
    }

    /* =========================
       Helpers
       ========================= */

    private static void appendBuyer(Xml xml, Buyer buyer) {
        Address address = (buyer != null) ? buyer.address() : null;
        // firma_lub_osoba_prywatna: consider deriving from NIP presence if API requires it (0 vs 1)
        xml.open("nabywca")
                .tag("firma_lub_osoba_prywatna", "0")
                .tag("nazwa", buyer != null ? nullToEmpty(buyer.clientName()) : "")
                .tag("nip", buyer != null ? nullToEmpty(buyer.nip()) : "")
                .tag("ulica_i_numer", address == null ? null : address.getStreetAndNumber())
                .tag("kod_pocztowy", address != null ? nullToEmpty(address.getPostalCode()) : "")
                .tag("miejscowosc", address != null ? nullToEmpty(address.getCity()) : "")
                .tag("kraj", address != null ? nullToEmpty(address.getCountry()) : "")
                .close("nabywca");
    }

    private static void writeMarginLine(Xml x, PhoneModel ph, String orderId) {
        x.tag("nazwa", buildMarginItemName(ph, orderId))
                .tag("ilosc", DEFAULT_QTY_STR)
                .tag("jm", "szt.")
                .tag("vat", "0")
                .tag("wartosc_netto", money(ph.getPrice()))
                .tag("symbol_gtu", "6");
    }

    private static void writeVatLine(Xml x, PhoneModel ph, String orderId) {
        BigDecimal gross = ph.getPrice();
        if (gross == null || gross.signum() <= 0) gross = CHARGER_GROSS_PRICE_PLN;
        x.tag("nazwa", buildVatItemName(ph, orderId))
                .tag("ilosc", DEFAULT_QTY_STR)
                .tag("jm", "szt.")
                .tag("vat", "23")
                .tag("wartosc_brutto", money(gross))
                .tag("symbol_gtu", "6");
    }

    // Single fallback line (quantity-aware) for both MARGIN and VAT_23
    private static void appendSingleLine(Xml xml, InvoiceKind kind, String orderId, BigDecimal totalToPay, int quantity) {
        xml.open("faktura_pozycje");
        if (kind == InvoiceKind.MARGIN) {
            xml.tag("nazwa", "Produkt")
                    .tag("ilosc", DEFAULT_QTY_STR)
                    .tag("jm", "szt.")
                    .tag("vat", "0")
                    .tag("wartosc_netto", money(totalToPay))
                    .tag("symbol_gtu", "6");
        } else {
            String qtyStr = quantity + ".000"; // simple and locale-agnostic
            xml.tag("nazwa", defaultChargerName(orderId))
                    .tag("ilosc", qtyStr)
                    .tag("jm", "szt.")
                    .tag("vat", "23")
                    .tag("wartosc_brutto", money(totalToPay))
                    .tag("symbol_gtu", "6");
        }
        xml.close("faktura_pozycje");
    }

    // Returns adjusted order and charger count to avoid double iteration
    private static AdjustedOrder buildOrderWithPerPhoneMinus100ForChargers(Order src) {
        Order order = new Order(src);

        List<PhoneModel> adjusted = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        int chargerCount = 0;

        for (PhoneModel ph : safeItems(src)) {
            BigDecimal originalPrice = ph.getPrice();
            boolean hasCharger = ph.isChargerIncluded();
            BigDecimal finalPrice = hasCharger
                    ? originalPrice.subtract(CHARGER_GROSS_PRICE_PLN)
                    : originalPrice;

            if (hasCharger) chargerCount++;

            adjusted.add(new PhoneModel(
                    ph.getName(), ph.getColor(), ph.getMemory(), ph.getItemGrade(),
                    ph.isNewBattery(), hasCharger, finalPrice
            ));
            sum = sum.add(finalPrice);
        }

        order.setPhoneModelList(adjusted);
        order.setTotalPrice(sum);
        return new AdjustedOrder(order, chargerCount);
    }

    private static List<PhoneModel> safeItems(Order order) {
        // Return non-null items or an empty list if order/items are null
        return Optional.ofNullable(order)
                .map(Order::getPhoneModelList)
                .orElseGet(List::of)
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private static String money(BigDecimal v) {
        return v == null ? "0.00" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String detectPaymentStatus(Order order) {
        String ps = order != null ? order.getPaymentStatus() : null;
        if (ps == null || ps.isBlank()) return "2";
        String s = ps.toLowerCase(Locale.ROOT);
        if (s.contains("nieopł") || s.contains("unpaid") || s.contains("oczek") || s.contains("pending")) return "1";
        if (s.contains("opł") || s.contains("paid")) return "2";
        return "0";
    }

    private static String buildMarginItemName(PhoneModel ph, String orderId) {
        // Build once, trim once
        String main = (ph.getName() + " " + ph.getMemory() + " " + ph.getColor()).trim();
        return (main + "\nzamówienie #" + nullToEmpty(orderId) + " icentrumsklep.pl").trim();
    }

    private static String buildVatItemName(PhoneModel ph, String orderId) {
        String base = (ph.getName() == null || ph.getName().isBlank()) ? "ŁADOWARKA + KABEL BASEUS" : ph.getName();
        return (base + "\nzamówienie #" + nullToEmpty(orderId) + " z icentrumsklep.pl").trim();
    }

    private static String defaultChargerName(String orderId) {
        return ("ŁADOWARKA + KABEL BASEUS\nzamówienie #" + nullToEmpty(orderId) + " z icentrumsklep.pl").trim();
    }

    private static String nullToEmpty(String s) {
        return (s == null) ? "" : s;
    }

    // Ensures minimal time gap between issues. Blocks current thread.
    private synchronized void ensureMinIssueGap() {
        long now = System.currentTimeMillis();
        long wait = minIssueGapMs - (now - lastIssueAt);
        if (wait > 0) {
            try { Thread.sleep(wait); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        lastIssueAt = System.currentTimeMillis();
    }

    private String postXmlWithRetries(String xml) throws Exception {
        int attempts = 0;
        long backoffMs = initialBackoffMs;

        while (true) {
            ensureMinIssueGap();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/xml; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(xml))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();

            // If API returns <kod>2</kod>, retry with exponential backoff
            if (body != null && body.contains("<kod>2</kod>") && attempts < maxRetryAttempts) {
                attempts++;
                try { Thread.sleep(backoffMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                backoffMs = backoffMs * 2; // initial is >= 1, extra Math.max was redundant
                continue;
            }
            return body;
        }
    }

    /* =========================
       Tiny XML builder
       ========================= */

    private static final class Xml {
        private final StringBuilder sb = new StringBuilder();

        private Xml() { sb.append("<dokument>\n"); }

        static Xml start() { return new Xml(); }

        Xml open(String tag) { sb.append("  <").append(tag).append(">\n"); return this; }

        Xml close(String tag) { sb.append("  </").append(tag).append(">\n"); return this; }

        Xml tag(String tag, String value) {
            sb.append("  <").append(tag).append(">")
                    .append(esc(value))
                    .append("</").append(tag).append(">\n");
            return this;
        }

        String build() { return sb.append("</dokument>").toString(); }

        // Minimal escaping for XML text nodes
        private static String esc(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
        }
    }

    /* =========================
       Response parsing
       ========================= */

    private static String extractInvoiceIdentifier(String xml) {
        if (xml == null || xml.isBlank()) return "";

        Matcher ok = KOD_OK_PATTERN.matcher(xml);
        if (!ok.find()) {
            // not a successful add; return raw XML so caller can decide how to show it
            return xml.trim();
        }

        Matcher mNr = NR_PATTERN.matcher(xml);
        if (mNr.find()) {
            return mNr.group(1).trim();
        }

        Matcher mId = ID_PATTERN.matcher(xml);
        if (mId.find()) {
            return ("ID: " + mId.group(1).trim());
        }

        return xml.trim();
    }

    /* =========================
           Small data holder
           ========================= */
        private record AdjustedOrder(Order order, int chargerCount) {
    }
}
