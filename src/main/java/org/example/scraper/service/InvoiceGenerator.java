package org.example.scraper.service;

import lombok.Getter;
import org.example.scraper.auth.CredentialsManager;
import org.example.scraper.model.CrmDevice;
import org.example.scraper.model.DeviceModel;
import org.example.scraper.model.Order;
import org.example.scraper.service.crm.CrmIntegrationService;
import org.example.scraper.service.utils.InvoiceBatteryFormat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class InvoiceGenerator {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final CrmIntegrationService crmIntegrationService = new CrmIntegrationService();

    private static final String BIURO_MARZA = "140873";
    private static final String BIURO_VAT = "140881";

    private static final Integer CHARGER_PRICE = 100;

    @Getter
    public enum DocumentType {
        FAKTURA_VAT(0, "Faktura VAT"),
        FAKTURA_KONCOWA(3, "Faktura końcowa"),
        FAKTURA_MARZA(5, "Faktura marża"),
        FAKTURA_ZALICZKOWA(11, "Faktura zaliczkowa");

        private final int code;
        private final String description;

        DocumentType(int code, String description) {
            this.code = code;
            this.description = description;
        }
    }

    public String generatePhoneFinalInvoice(
            Order order,
            DeviceModel deviceModel,
            String invoiceId,
            boolean batteryCommunicate,
            CrmDevice crmDevice
    ) throws Exception {
        BigDecimal price = calculateDevicePrice(deviceModel);

        String positionXml = buildInvoicePosition(
                buildDeviceDescription(order, deviceModel, crmDevice, batteryCommunicate, true),
                "1.000",
                0,
                price
        );

        String xml = buildBaseInvoiceXml(
                order,
                DocumentType.FAKTURA_KONCOWA,
                BIURO_MARZA,
                price,
                buildMarginNote(order),
                false,
                buildRelationXml(invoiceId) + buildMarginXml(),
                positionXml
        );

        return sendInvoiceRequest(xml);
    }

    public String generateChargerFinalInvoice(
            Order order,
            int chargerCount,
            String invoiceId,
            boolean appleWatchCharger
    ) throws Exception {

        BigDecimal price = calculateChargerPrice(chargerCount);

        String positionXml = buildInvoicePosition(
                buildChargerDescription(appleWatchCharger),
                String.valueOf(chargerCount),
                23,
                price
        );

        String xml = buildBaseInvoiceXml(
                order,
                DocumentType.FAKTURA_KONCOWA,
                BIURO_VAT,
                price,
                "Zamówienie #" + order.getOrderId(),
                false,
                buildRelationXml(invoiceId) + buildZeroMarginXml(),
                positionXml
        );

        return sendInvoiceRequest(xml);
    }

    public String generateIPhoneVatInvoice() {
        return "";
    }

    public String generateIPhoneMarginInvoice(
            Order order,
            DeviceModel deviceModel,
            boolean batReplMessage,
            CrmDevice crmDevice
    ) throws Exception {
        BigDecimal price = calculateDevicePrice(deviceModel);

        String positionXml = buildInvoicePosition(
                buildDeviceDescription(order, deviceModel, crmDevice, batReplMessage, false),
                "1.000",
                0,
                price
        );

        String xml = buildBaseInvoiceXml(
                order,
                DocumentType.FAKTURA_MARZA,
                BIURO_MARZA,
                price,
                buildMarginNote(order),
                false,
                "",
                positionXml
        );

        return sendInvoiceRequest(xml);
    }

    public String generateChargerVatInvoice(Order order, int chargerCount, boolean appleWatchCharger) throws Exception {
        BigDecimal price = calculateChargerPrice(chargerCount);

        String positionXml = buildInvoicePosition(
                buildChargerDescription(appleWatchCharger),
                String.valueOf(chargerCount),
                23,
                price
        );

        String xml = buildBaseInvoiceXml(
                order,
                DocumentType.FAKTURA_VAT,
                BIURO_VAT,
                price,
                "zamówienie # " + order.getOrderId(),
                false,
                "",
                positionXml
        );

        return sendInvoiceRequest(xml);
    }

    public String generatePhoneAdvanceInvoice(Order order, DeviceModel deviceModel) throws Exception {
        BigDecimal price = calculateDevicePrice(deviceModel);

        String positionXml = buildInvoicePosition(
                buildAdvanceDeviceDescription(order, deviceModel),
                "1.000",
                0,
                price
        );

        String xml = buildBaseInvoiceXml(
                order,
                DocumentType.FAKTURA_ZALICZKOWA,
                BIURO_MARZA,
                price,
                buildMarginNote(order),
                true,
                buildMarginXml(),
                positionXml
        );

        return sendInvoiceRequest(xml);
    }

    public String generateChargerAdvanceInvoice(Order order, int chargerCount) throws Exception {
        BigDecimal price = calculateChargerPrice(chargerCount);

        String positionXml = buildInvoicePosition(
                buildChargerDescription(false, true, order),
                String.valueOf(chargerCount),
                23,
                price
        );

        String xml = buildBaseInvoiceXml(
                order,
                DocumentType.FAKTURA_ZALICZKOWA,
                BIURO_VAT,
                price,
                "",
                true,
                "",
                positionXml
        );

        return sendInvoiceRequest(xml);
    }

    private String buildDeviceDescription(
            Order order,
            DeviceModel deviceModel,
            CrmDevice crmDevice,
            boolean batReplMessage,
            boolean includeOrderLine
    ) throws Exception {
        if (isAppleWatch(crmDevice)) {
            return buildAppleWatchDescription(order, deviceModel, crmDevice, includeOrderLine);
        }

        return buildPhoneDescription(order, deviceModel, crmDevice, batReplMessage, includeOrderLine);
    }

    private String buildPhoneDescription(
            Order order,
            DeviceModel deviceModel,
            CrmDevice crmDevice,
            boolean batReplMessage,
            boolean includeOrderLine
    ) throws Exception {
        String imei = crmDevice.getImei();
        String imei2 = crmDevice.getImei2();

        String model = upper(deviceModel.getName());
        String memory = deviceModel.getMemory();
        String color = upper(crmDevice.getColor());
        String phoneName = model + " " + memory + " " + color;

        Integer battery = crmIntegrationService.getBatteryByImei(imei);

        String batteryVariant = InvoiceBatteryFormat.batteryFormat(deviceModel.getBattery());
        String batMess = batReplMessage ? " (KOMUNIKAT)" : "";

        String description =
                "TELEFON KOMÓRKOWY\n" +
                        phoneName + "\n" +
                        batteryVariant + battery + "%" + batMess + "\n" +
                        "IMEI: " + imei + "\n" +
                        buildImei2Line(imei2) +
                        "GWARANCJA SERWISOWA 24 MIESIĄCE";

        if (includeOrderLine) {
            description += "\nZamówienie #" + order.getOrderId() + " icentrumsklep.pl";
        }

        return description;
    }

    private String buildAppleWatchDescription(Order order, DeviceModel deviceModel, CrmDevice crmDevice, boolean includeOrderLine) {
        String model = upper(deviceModel.getName());
        String memory = deviceModel.getMemory();
        String color = upper(crmDevice.getColor());

        String watchName = (model + " " + memory + " " + color).trim();

        String description =
                watchName + "\n" +
                        "NR SERYJNY: " +  crmDevice.getImei() + "\n" +
                        "GWARANCJA SERWISOWA 24 MIESIĄCE";

        if (includeOrderLine) {
            description += "\nZamówienie #" + order.getOrderId() + " icentrumsklep.pl";
        }

        return description;
    }

    private String buildAdvanceDeviceDescription(Order order, DeviceModel deviceModel) {
        String model = upper(deviceModel.getName());
        String memory = deviceModel.getMemory();
        String color = upper(deviceModel.getColor());

        return model + " " + memory + " " + color + "\n" + "zamówienie #" + order.getOrderId() + " icentrumsklep.pl";
    }

    private String buildChargerDescription(boolean appleWatchCharger) {
        return appleWatchCharger
                ? "BEZPRZEWODOWA ŁADOWARKA BASEUS"
                : "ŁADOWARKA + KABEL BASEUS";
    }

    private String buildChargerDescription(boolean appleWatchCharger, boolean includeOrderLine, Order order) {
        String description = buildChargerDescription(appleWatchCharger);

        if (includeOrderLine) {
            description += "\nzamówienie #" + order.getOrderId() + " icentrumsklep.pl";
        }

        return description;
    }

    private String buildInvoicePosition(String name, String quantity, int vat, BigDecimal grossValue) {
        return "<faktura_pozycje>" +
                "<nazwa>" + name + "</nazwa>" +
                "<ilosc>" + quantity + "</ilosc>" +
                "<jm>szt.</jm>" +
                "<vat>" + vat + "</vat>" +
                "<symbol_gtu>6</symbol_gtu>" +
                "<wartosc_brutto>" + grossValue + "</wartosc_brutto>" +
                "</faktura_pozycje>";
    }

    private String buildBaseInvoiceXml(
            Order order,
            DocumentType documentType,
            String departmentId,
            BigDecimal paidAmount,
            String note,
            boolean includePaymentDeadline,
            String extraXml,
            String positionXml
    ) {
        return "<dokument>" +
                "<api_token>" + CredentialsManager.getFakturaxlToken() + "</api_token>" +
                "<typ_faktury>" + documentType.getCode() + "</typ_faktury>" +
                "<typ_faktur_podtyp>0</typ_faktur_podtyp>" +
                "<obliczaj_sume_wartosci_faktury_wg>0</obliczaj_sume_wartosci_faktury_wg>" +
                "<numer_faktury></numer_faktury>" +
                "<data_wystawienia>" + LocalDate.now() + "</data_wystawienia>" +
                "<data_sprzedazy>" + order.getOrderSubmissionDate() + "</data_sprzedazy>" +
                "<miejsce_wystawienia>Wrocław</miejsce_wystawienia>" +
                buildPaymentDeadlineXml(includePaymentDeadline) +
                "<data_oplacenia>" + order.getOrderSubmissionDate() + "</data_oplacenia>" +
                "<status>2</status>" +
                "<kwota_oplacona>" + paidAmount + "</kwota_oplacona>" +
                "<uwagi>" + note + "</uwagi>" +
                "<waluta>PLN</waluta>" +
                "<kurs>1</kurs>" +
                "<rodzaj_platnosci>" + mapPaymentMethod(order.getPaymentMethod()) + "</rodzaj_platnosci>" +
                "<jezyk>0</jezyk>" +
                "<szablon>0</szablon>" +
                "<imie_nazwisko_wystawcy></imie_nazwisko_wystawcy>" +
                "<id_dzialy_firmy>" + departmentId + "</id_dzialy_firmy>" +
                "<wyslij_dokument_do_klienta_emailem>0</wyslij_dokument_do_klienta_emailem>" +
                "<obliczaj_wartosc_faktury_od>0</obliczaj_wartosc_faktury_od>" +
                extraXml +
                buildPurchaserXml(order) +
                positionXml +
                "</dokument>";
    }

    private String buildPurchaserXml(Order order) {
        String purchaser = "".equals(order.getNip()) ? order.getClientName() : order.getCompanyName();

        return "<nabywca>" +
                "<firma_lub_osoba_prywatna>0</firma_lub_osoba_prywatna>" +
                "<nazwa>" + purchaser + "</nazwa>" +
                "<email>" + order.getEmail() + "</email>" +
                "<telefon> </telefon>" +
                "<ulica_i_numer>" + order.getAddress().getStreetAndNumber() + "</ulica_i_numer>" +
                "<kod_pocztowy>" + order.getAddress().getPostalCode() + "</kod_pocztowy>" +
                "<miejscowosc>" + order.getAddress().getCity() + "</miejscowosc>" +
                "<kraj>PL</kraj>" +
                "<nip>" + order.getNip() + "</nip>" +
                "</nabywca>";
    }

    private String buildMarginNote(Order order) {
        String note = "Procedura marży - towary używane";
        String publicNote = order.getNotes().getAdminPublicNoteOrDefault();

        if (publicNote != null && publicNote.toLowerCase().contains("pakiet serwisowy")) {
            note += "\nPakiet serwisowy – jednorazowa, bezpłatna wymiana baterii w okresie trwania gwarancji";
        }

        return note;
    }

    private String buildMarginXml() {
        return "<typ_marzy>2</typ_marzy>" +
                "<wartosc_marzy_brutto>0</wartosc_marzy_brutto>" +
                "<marza_stawka_vat>0</marza_stawka_vat>";
    }

    private String buildZeroMarginXml() {
        return "<wartosc_marzy_brutto>0</wartosc_marzy_brutto>" +
                "<marza_stawka_vat>0</marza_stawka_vat>";
    }

    private String buildRelationXml(String invoiceId) {
        return "<dokument_rel_id>" + invoiceId + "</dokument_rel_id>";
    }

    private String buildPaymentDeadlineXml(boolean includePaymentDeadline) {
        return includePaymentDeadline
                ? "<termin_platnosci_data>" + LocalDate.now() + "</termin_platnosci_data>"
                : "";
    }

    private String buildImei2Line(String imei2) {
        return imei2 != null && !imei2.isBlank()
                ? "IMEI2: " + imei2 + "\n"
                : "";
    }

    private BigDecimal calculateDevicePrice(DeviceModel deviceModel) {
        return deviceModel.isChargerIncluded()
                ? deviceModel.getPrice().subtract(BigDecimal.valueOf(CHARGER_PRICE))
                : deviceModel.getPrice();
    }

    private BigDecimal calculateChargerPrice(int chargerCount) {
        return BigDecimal.valueOf(CHARGER_PRICE).multiply(BigDecimal.valueOf(chargerCount));
    }

    private boolean isAppleWatch(CrmDevice crmDevice) {
        return crmDevice.getModel() != null
                && crmDevice.getModel().toLowerCase().contains("watch");
    }

    private String upper(String value) {
        return value == null ? "" : value.toUpperCase();
    }

    private String sendInvoiceRequest(String xml) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://program.fakturaxl.pl/api/dokument_dodaj.php"))
                .header("Content-Type", "application/xml; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(xml, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return response.body();
    }

    private String mapPaymentMethod(String paymentMethod) {
        return switch (paymentMethod) {
            case "BLIK - Przelew online"                  -> "BLIK";
            case "Za pobraniem",
                 "Za pobraniem (InPost Pay) - InPost Pay" -> "Opłata za pobraniem";
            case "PayU / RATY 0%"                         -> "PayU";
            case "PayPo - Przelewy24"                     -> "Przelewy24";
            default                                       -> "Przelew";
        };
    }
}