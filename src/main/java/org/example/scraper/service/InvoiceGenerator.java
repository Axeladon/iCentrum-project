package org.example.scraper.service;

import org.example.scraper.dto.InvoiceResponseDto;
import org.example.scraper.model.CrmDevice;
import org.example.scraper.model.Order;
import org.example.scraper.model.PhoneModel;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvoiceGenerator {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String BIURO_MARZA = "140873";
    private static final String BIURO_VAT = "140881";

    private static final Integer CHARGER_PRICE = 100;

    private final String apiToken;

    public InvoiceGenerator(String apiToken) {
        this.apiToken = apiToken;
    }

    public void generatePhoneAndAccessoriesAdvanceInvoice(Order order) throws Exception {
        int chargerCount = 0;

        for (PhoneModel phoneModel : order.getPhoneModelList()) {
            generatePhoneAdvanceInvoice(order, phoneModel);
            if (phoneModel.isChargerIncluded()) chargerCount++;
            Thread.sleep(2100);
        }
        generateChargerAdvanceInvoice(order, chargerCount);
    }

    public List<InvoiceResponseDto> generatePhoneMarginAndAccessoriesInvoice(Order order) throws Exception {
        List<InvoiceResponseDto> invoiceResponseDtoList = new ArrayList<>();

        int chargerCount = 0;

        for (PhoneModel phoneModel : order.getPhoneModelList()) {
            invoiceResponseDtoList.add(generateIPhoneMarginInvoice(order, phoneModel));
            if (phoneModel.isChargerIncluded()) chargerCount++;
            Thread.sleep(2100);
        }

        if (!(chargerCount == 0)) {
            invoiceResponseDtoList.add(generateChargerVatInvoice(order, chargerCount));
        }

        return invoiceResponseDtoList;
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
            case "BLIK - Przelew online" -> "BLIK";
            case "Za pobraniem"          -> "Opłata za pobraniem";
            case "PayU / RATY 0%"        -> "PayU";
            case "PayPo - Przelewy24"    -> "Przelewy24";
            default                      -> "Przelew";
        };
    }

    private InvoiceResponseDto validateInvoiceResponse(String response) {
        InvoiceResponseDto irdto = new InvoiceResponseDto();
        try {
            var doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(response)));

            String kod = doc.getElementsByTagName("kod").item(0).getTextContent();
            irdto.setKod(kod);

            if (!"1".equals(kod)) {
                return irdto;
            }

            String id = doc.getElementsByTagName("dokument_id").item(0).getTextContent();
            String nr = doc.getElementsByTagName("dokument_nr").item(0).getTextContent();
            String unik = doc.getElementsByTagName("unikatowy_kod").item(0).getTextContent();

            irdto.setDokumentId(id);
            irdto.setDokumentNr(nr);
            irdto.setUnikatowyKod(unik);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return irdto;
    }

    private String generatePhoneFinalInvoice() {
        return "";
    }

    private String generateChargerFinalInvoice() {
        return "";
    }

    private String generateIPhoneVatInvoice() {
        return "";
    }

    private InvoiceResponseDto generateIPhoneMarginInvoice(Order order, PhoneModel phoneModel) throws Exception {
        CrmDevice crmDevice = new ThreeUToolsService().createCrmDeviceFromDirectory();
        String imei = crmDevice.getImei();
        String imei2 = crmDevice.getImei2();

        String model = phoneModel.getName().toUpperCase();
        String memory = phoneModel.getMemory();
        String color = crmDevice.getColor().toUpperCase();
        String iphone = model + " " + memory + " " + color;
        Integer battery = new CrmIntegrationService().getBatteryByImei(crmDevice.getImei());

        BigDecimal price = phoneModel.isChargerIncluded()
                ? phoneModel.getPrice().subtract(BigDecimal.valueOf(100))
                : phoneModel.getPrice();

        String invoiceName =
                        "TELEFON KOMÓRKOWY\n" +
                        iphone + "\n" +
                        "KONDYCJA BATERII: " + battery + "%\n" +
                        "IMEI: " + imei + "\n" +
                        ((imei2 != null && !imei2.isBlank()) ? "IMEI2: " + imei2 + "\n" : "") +
                        "GWARANCJA SERWISOWA 24 MIESIĄCE";

        String xml = "<dokument>" +
                "<api_token>" + apiToken + "</api_token>" +
                "<typ_faktury>5</typ_faktury>" +
                "<typ_faktur_podtyp>0</typ_faktur_podtyp>" +
                "<obliczaj_sume_wartosci_faktury_wg>0</obliczaj_sume_wartosci_faktury_wg>" +
                "<numer_faktury></numer_faktury>" +
                "<data_wystawienia>" + LocalDate.now() + "</data_wystawienia>" +
                "<data_sprzedazy>" + order.getOrderSubmissionDate() + "</data_sprzedazy>" +
                "<miejsce_wystawienia>Wrocław</miejsce_wystawienia>" +
                "<data_oplacenia>" + order.getOrderSubmissionDate() + "</data_oplacenia>" +
                "<status>2</status>" +
                "<kwota_oplacona>" + price + "</kwota_oplacona>" +
                "<uwagi>Procedura marży - towary używane</uwagi>" +
                "<waluta>PLN</waluta>" +
                "<kurs>1</kurs>" +
                "<rodzaj_platnosci>" + mapPaymentMethod(order.getPaymentMethod()) + "</rodzaj_platnosci>" +
                "<jezyk>0</jezyk>" +
                "<szablon>0</szablon>" +
                "<imie_nazwisko_wystawcy></imie_nazwisko_wystawcy>" +
                "<id_dzialy_firmy>" + BIURO_MARZA + "</id_dzialy_firmy>" +
                "<wyslij_dokument_do_klienta_emailem>0</wyslij_dokument_do_klienta_emailem>" +
                "<obliczaj_wartosc_faktury_od>0</obliczaj_wartosc_faktury_od>" +

                "<nabywca>" +
                    "<firma_lub_osoba_prywatna>0</firma_lub_osoba_prywatna>" +
                    "<nazwa>" + order.getClientName() + "</nazwa>" +
                    "<telefon> </telefon>" +
                    "<ulica_i_numer>" + order.getAddress().getStreetAndNumber() + "</ulica_i_numer>" +
                    "<kod_pocztowy>" + order.getAddress().getPostalCode() + "</kod_pocztowy>" +
                    "<miejscowosc>" + order.getAddress().getCity() + "</miejscowosc>" +
                    "<kraj>PL</kraj>" +
                    "<nip>" + order.getNip() + "</nip>" +
                    "<email>" + order.getEmail() + "</email>" +
                "</nabywca>" +

                "<faktura_pozycje>" +
                    "<nazwa>" + invoiceName + "</nazwa>" +
                    "<ilosc>1.000</ilosc>" +
                    "<jm>szt.</jm>" +
                    "<vat>0</vat>" +
                    "<symbol_gtu>6</symbol_gtu>" +
                    "<wartosc_brutto>" + price + "</wartosc_brutto>" +
                "</faktura_pozycje>" +
                "</dokument>";

        String response = sendInvoiceRequest(xml);
        return validateInvoiceResponse(response);
    }

    private InvoiceResponseDto generateChargerVatInvoice(Order order, int chargerCount) throws Exception {
        int price = CHARGER_PRICE * chargerCount;

        String xml = "<dokument>" +
                "<api_token>" + apiToken + "</api_token>" +
                "<typ_faktury>0</typ_faktury>" +
                "<typ_faktur_podtyp>0</typ_faktur_podtyp>" +
                "<obliczaj_sume_wartosci_faktury_wg>0</obliczaj_sume_wartosci_faktury_wg>" +
                "<numer_faktury></numer_faktury>" +
                "<data_wystawienia>" + LocalDate.now() + "</data_wystawienia>" +
                "<data_sprzedazy>" + order.getOrderSubmissionDate() + "</data_sprzedazy>" +
                "<miejsce_wystawienia>Wrocław</miejsce_wystawienia>" +
                "<data_oplacenia>" + order.getOrderSubmissionDate() + "</data_oplacenia>" +
                "<status>2</status>" +
                "<kwota_oplacona>" + price + "</kwota_oplacona>" +
                "<uwagi>" + "zamówienie # " + order.getOrderNumber() + "</uwagi>" +
                "<waluta>PLN</waluta>" +
                "<kurs>1</kurs>" +
                "<rodzaj_platnosci>" + mapPaymentMethod(order.getPaymentMethod()) + "</rodzaj_platnosci>" +
                "<jezyk>0</jezyk>" +
                "<szablon>0</szablon>" +
                "<imie_nazwisko_wystawcy></imie_nazwisko_wystawcy>" +
                "<id_dzialy_firmy>" + BIURO_VAT + "</id_dzialy_firmy>" +
                "<wyslij_dokument_do_klienta_emailem>0</wyslij_dokument_do_klienta_emailem>" +
                "<obliczaj_wartosc_faktury_od>0</obliczaj_wartosc_faktury_od>" +

                "<nabywca>" +
                    "<firma_lub_osoba_prywatna>0</firma_lub_osoba_prywatna>" +
                    "<nazwa>" + order.getClientName() + "</nazwa>" +
                    "<telefon> </telefon>" +
                    "<ulica_i_numer>" + order.getAddress().getStreetAndNumber() + "</ulica_i_numer>" +
                    "<kod_pocztowy>" + order.getAddress().getPostalCode() + "</kod_pocztowy>" +
                    "<miejscowosc>" + order.getAddress().getCity() + "</miejscowosc>" +
                    "<kraj>PL</kraj>" +
                    "<nip>" + order.getNip() + "</nip>" +
                    "<email>" + order.getEmail() + "</email>" +
                "</nabywca>" +

                "<faktura_pozycje>" +
                    "<nazwa>" + "ŁADOWARKA + KABEL BASEUS" + "\n" + "</nazwa>" +
                    "<ilosc>" + chargerCount + "</ilosc>" +
                    "<jm>szt.</jm>" +
                    "<vat>23</vat>" +
                    "<symbol_gtu>6</symbol_gtu>" +
                    "<wartosc_brutto>" + price + "</wartosc_brutto>" +
                "</faktura_pozycje>" +
                "</dokument>";
        String response = sendInvoiceRequest(xml);
        return validateInvoiceResponse(response);
    }

    private void generatePhoneAdvanceInvoice(Order order, PhoneModel phoneModel) throws Exception {

        String model = phoneModel.getName().toUpperCase();
        String memory = phoneModel.getMemory();
        String color = phoneModel.getColor().toUpperCase();
        String iphone = model + " " + memory + " " + color;

        BigDecimal price = phoneModel.isChargerIncluded()
                ? phoneModel.getPrice().subtract(BigDecimal.valueOf(100))
                : phoneModel.getPrice();

        String xml = "<dokument>" +
                "<api_token>" + apiToken + "</api_token>" +
                "<typ_faktury>11</typ_faktury>" +
                "<typ_faktur_podtyp>0</typ_faktur_podtyp>" +
                "<obliczaj_sume_wartosci_faktury_wg>0</obliczaj_sume_wartosci_faktury_wg>" +
                "<numer_faktury></numer_faktury>" +
                "<data_wystawienia>" + LocalDate.now() + "</data_wystawienia>" +
                "<data_sprzedazy>" + order.getOrderSubmissionDate() + "</data_sprzedazy>" +
                "<miejsce_wystawienia>Wrocław</miejsce_wystawienia>" +
                "<termin_platnosci_data>" + LocalDate.now().plusDays(3) + "</termin_platnosci_data>" +
                "<data_oplacenia>" + order.getOrderSubmissionDate() + "</data_oplacenia>" +
                "<status>2</status>" +
                "<kwota_oplacona>" + price + "</kwota_oplacona>" +
                "<uwagi>Procedura marży - towary używane</uwagi>" +
                "<waluta>PLN</waluta>" +
                "<kurs>1</kurs>" +
                "<rodzaj_platnosci>" + mapPaymentMethod(order.getPaymentMethod()) + "</rodzaj_platnosci>" +
                "<jezyk>0</jezyk>" +
                "<szablon>0</szablon>" +
                "<imie_nazwisko_wystawcy></imie_nazwisko_wystawcy>" +
                "<id_dzialy_firmy>" + BIURO_MARZA + "</id_dzialy_firmy>" +
                "<wyslij_dokument_do_klienta_emailem>0</wyslij_dokument_do_klienta_emailem>" +
                "<obliczaj_wartosc_faktury_od>0</obliczaj_wartosc_faktury_od>" +

                "<nabywca>" +
                    "<firma_lub_osoba_prywatna>0</firma_lub_osoba_prywatna>" +
                    "<nazwa>" + order.getClientName() + "</nazwa>" +
                    "<email> </email>" +
                    "<telefon> </telefon>" +
                    "<ulica_i_numer>" + order.getAddress().getStreetAndNumber() + "</ulica_i_numer>" +
                    "<kod_pocztowy>" + order.getAddress().getPostalCode() + "</kod_pocztowy>" +
                    "<miejscowosc>" + order.getAddress().getCity() + "</miejscowosc>" +
                    "<kraj>PL</kraj>" +
                    "<nip>" + order.getNip() + "</nip>" +
                "</nabywca>" +

                "<faktura_pozycje>" +
                    "<nazwa>" + iphone + "\n" + "zamówienie #" + order.getOrderNumber() + " icentrumsklep.pl" + "</nazwa>" +
                    "<ilosc>1.000</ilosc>" +
                    "<jm>szt.</jm>" +
                    "<vat>0</vat>" +
                    "<symbol_gtu>6</symbol_gtu>" +
                    "<wartosc_brutto>" + price + "</wartosc_brutto>" +
                "</faktura_pozycje>" +
                "</dokument>";

        sendInvoiceRequest(xml);
    }

    private void generateChargerAdvanceInvoice(Order order, int chargerCount) throws Exception {
        int price = CHARGER_PRICE * chargerCount;

        String xml = "<dokument>" +
                "<api_token>" + apiToken + "</api_token>" +
                "<typ_faktury>11</typ_faktury>" +
                "<typ_faktur_podtyp>0</typ_faktur_podtyp>" +
                "<obliczaj_sume_wartosci_faktury_wg>0</obliczaj_sume_wartosci_faktury_wg>" +
                "<numer_faktury></numer_faktury>" +
                "<data_wystawienia>" + LocalDate.now() + "</data_wystawienia>" +
                "<data_sprzedazy>" + order.getOrderSubmissionDate() + "</data_sprzedazy>" +
                "<miejsce_wystawienia>Wrocław</miejsce_wystawienia>" +
                "<termin_platnosci_data>" + LocalDate.now().plusDays(3) + "</termin_platnosci_data>" +
                "<data_oplacenia>" + order.getOrderSubmissionDate() + "</data_oplacenia>" +
                "<status>2</status>" +
                "<kwota_oplacona>" + price + "</kwota_oplacona>" +
                "<uwagi></uwagi>" +
                "<waluta>PLN</waluta>" +
                "<kurs>1</kurs>" +
                "<rodzaj_platnosci>" + mapPaymentMethod(order.getPaymentMethod()) + "</rodzaj_platnosci>" +
                "<jezyk>0</jezyk>" +
                "<szablon>0</szablon>" +
                "<imie_nazwisko_wystawcy></imie_nazwisko_wystawcy>" +
                "<id_dzialy_firmy>" + BIURO_VAT + "</id_dzialy_firmy>" +
                "<wyslij_dokument_do_klienta_emailem>0</wyslij_dokument_do_klienta_emailem>" +
                "<obliczaj_wartosc_faktury_od>0</obliczaj_wartosc_faktury_od>" +

                "<nabywca>" +
                    "<firma_lub_osoba_prywatna>0</firma_lub_osoba_prywatna>" +
                    "<nazwa>" + order.getClientName() + "</nazwa>" +
                    "<email>" + order.getEmail() + "</email>" +
                    "<telefon> </telefon>" +
                    "<ulica_i_numer>" + order.getAddress().getStreetAndNumber() + "</ulica_i_numer>" +
                    "<kod_pocztowy>" + order.getAddress().getPostalCode() + "</kod_pocztowy>" +
                    "<miejscowosc>" + order.getAddress().getCity() + "</miejscowosc>" +
                    "<kraj>PL</kraj>" +
                    "<nip>" + order.getNip() + "</nip>" +
                "</nabywca>" +

                "<faktura_pozycje>" +
                    "<nazwa>" + "ŁADOWARKA + KABEL BASEUS" + "\n" + "zamówienie #" + order.getOrderNumber() + " icentrumsklep.pl" + "</nazwa>" +
                    "<ilosc>" + chargerCount + "</ilosc>" +
                    "<jm>szt.</jm>" +
                    "<vat>23</vat>" +
                    "<symbol_gtu>6</symbol_gtu>" +
                    "<wartosc_brutto>" + price + "</wartosc_brutto>" +
                "</faktura_pozycje>" +
                "</dokument>";

        sendInvoiceRequest(xml);
    }
}
