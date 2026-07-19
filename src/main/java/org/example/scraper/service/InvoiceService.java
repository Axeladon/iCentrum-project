package org.example.scraper.service;

import org.example.scraper.model.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InvoiceService {

    private static final String FINAL_INVOICE_MARKER = "ID:";
    private final InvoiceGenerator invoiceGenerator = new InvoiceGenerator();

    private final String RATE_LIMIT_CODE = "2";
    private final Integer API_DELAY_MS = 2100;

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    public List<InvoiceResponse> generatePhoneAndAccessoriesAdvanceInvoice(Order order) {
        List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
        int chargerCount = 0;

        for (DeviceModel deviceModel : order.getDeviceModelList()) {
            invoiceResponseList.add(createInvoiceUntilValid(
                    () -> invoiceGenerator.generatePhoneAdvanceInvoice(order, deviceModel))
            );

            if (deviceModel.isChargerIncluded()) chargerCount++;
        }

        if (chargerCount > 0) {
            final int finalChargerCount = chargerCount;
            waitBeforeNextRequest();
            invoiceResponseList.add(createInvoiceUntilValid(
                    () -> invoiceGenerator.generateChargerAdvanceInvoice(order, finalChargerCount))
            );
        }

        return invoiceResponseList;
    }

    public InvoiceResponse generateDeviceInvoice(Order order, DeviceModel deviceModel, boolean batReplMessage, CrmDevice crmDevice) {
        if (isFinalInvoice(order)) {
            String fz = getFinalInvoiceId(order, 0);

            return createInvoiceUntilValid(
                    () -> invoiceGenerator.generatePhoneFinalInvoice(
                            order,
                            deviceModel,
                            fz,
                            batReplMessage,
                            crmDevice
                    )
            );
        }

        return createInvoiceUntilValid(
                () -> invoiceGenerator.generateIPhoneMarginInvoice(
                        order,
                        deviceModel,
                        batReplMessage,
                        crmDevice
                )
        );
    }

    public InvoiceResponse generateChargerInvoice(Order order, int chargerCount, boolean appleWatchCharger) {
        waitBeforeNextRequest();

        if (isFinalInvoice(order)) {
            String fz_v = getFinalInvoiceId(order, 1);

            return createInvoiceUntilValid(
                    () -> invoiceGenerator.generateChargerFinalInvoice(
                            order,
                            chargerCount,
                            fz_v,
                            appleWatchCharger
                    )
            );
        }

        return createInvoiceUntilValid(
                () -> invoiceGenerator.generateChargerVatInvoice(
                        order,
                        chargerCount,
                        appleWatchCharger
                )
        );
    }

    private String getFinalInvoiceId(Order order, int index) {
        String privateNote = order.getNotes().getAdminPrivateNoteOrDefault();

        Pattern pattern = Pattern.compile(FINAL_INVOICE_MARKER + "\\s*(\\d+)");
        Matcher matcher = pattern.matcher(privateNote);

        int currentIndex = 0;

        while (matcher.find()) {
            if (currentIndex == index) {
                return matcher.group(1);
            }

            currentIndex++;
        }

        return null;
    }

    public boolean isFinalInvoice(Order order) {
        String privateNote = order.getNotes().getAdminPrivateNoteOrDefault();
        return privateNote != null && privateNote.contains(FINAL_INVOICE_MARKER) && !privateNote.isBlank();
    }

    private InvoiceResponse validateInvoiceResponse(String response) {
        InvoiceResponse invResp = new InvoiceResponse();
        try {
            var doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(response)));

            String kod = doc.getElementsByTagName("kod").item(0).getTextContent();
            invResp.setKod(kod);

            if (!"1".equals(kod)) {
                return invResp;
            }

            String id = doc.getElementsByTagName("dokument_id").item(0).getTextContent();
            String nr = doc.getElementsByTagName("dokument_nr").item(0).getTextContent();
            String unik = doc.getElementsByTagName("unikatowy_kod").item(0).getTextContent();

            invResp.setDokumentId(id);
            invResp.setDokumentNr(nr);
            invResp.setUnikatowyKod(unik);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return invResp;
    }

    private InvoiceResponse createInvoiceUntilValid(ThrowingSupplier<String> requestSupplier) {

        try {
            InvoiceResponse response;
            do {
                response = validateInvoiceResponse(requestSupplier.get());

                if (RATE_LIMIT_CODE.equals(response.getKod())) {
                    waitBeforeNextRequest();
                }

                System.out.println("Faktura: " + response.getDokumentNr() + ",  response code: " + response.getKod());

            } while (RATE_LIMIT_CODE.equals(response.getKod()));
            return response;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void waitBeforeNextRequest() {
        try {
            Thread.sleep(API_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted", e);
        }
    }
}