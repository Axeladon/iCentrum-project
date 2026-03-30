package org.example.scraper.service;

import org.example.scraper.model.RepairPrices;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class ApplefixPriceScraper {

    public record TwoPrices(String price1, String price2){}

    private static final List<String> iPhones = List.of(
            "iphone-17-pro-max",
            "iphone-17-pro",
            "iphone-17",
            "iphone-air",
            "iphone-16-pro-max",
            "iphone-16-pro",
            "iphone-16-plus",
            "iphone-16",
            "iphone-15-pro-max",
            "iphone-15-pro",
            "iphone-15-plus",
            "iphone-15",
            "iphone-14-pro-max",
            "iphone-14-pro",
            "iphone-14-plus",
            "iphone-14",
            "iphone-13-pro-max",
            "iphone-13-pro",
            "iphone-13",
            "iphone-13-mini",
            "iphone-12-pro-max",
            "iphone-12-pro",
            "iphone-12",
            "iphone-12-mini",
            "iphone-11-pro-max",
            "iphone-11-pro",
            "iphone-11"
    );

    public static void main(String[] args) throws IOException {
        RepairPrices rp = new RepairPrices();

        for (int i = 0; i < iPhones.size(); i++) {

            String iPhone = iPhones.get(i);

            addScreen(rp, "https://applefix.pl/produkt/wymiana-wyswietlacza-" + iPhone);
            addGlass(rp, "https://applefix.pl/produkt/wymiana-szybki-" + iPhone);
            addBattery(rp, "https://applefix.pl/produkt/wymiana-baterii-" + iPhone);
            addChargingPort(rp, "https://applefix.pl/produkt/wymiana-gniazda-ladowania-" + iPhone);
            addHousing(rp, "https://applefix.pl/produkt/wymiana-obudowy-" + iPhone);
            addFaceId(rp, "https://applefix.pl/produkt/face-id-" + iPhone);
            addCameraAndItsGlass(rp, "https://applefix.pl/produkt/wymiana-tylnej-kamery-" + iPhone);
            addFrontCamera(rp, "https://applefix.pl/produkt/wymiana-przedniej-kamery-" + iPhone);
            addSpeakerAndBuzzer(rp, iPhone);
            addMicrophone(rp, "https://applefix.pl/produkt/wymiana-mikrofonu-" + iPhone);
            addSideButtons(rp, "https://applefix.pl/produkt/przycisk-boczny-" + iPhone);

            printPriceInfo(rp, iPhone);
        }
    }

    private static Document scrape(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; SimpleScraper/1.0)")
                .timeout(20_000)
                .get();
    }

    private static void addScreen(RepairPrices rp, String url) throws IOException {
        Document doc = scrape(url);

        String original = null, replacement = null;

        for (Element el : doc.select("div.woovr-variation[data-price][data-attrs]")) {
            String attrs = el.attr("data-attrs");
            String price = el.attr("data-price");
            if (original == null && attrs.contains("montaz-oryginalny-wyswietlacz")) original = price;
            if (replacement == null && attrs.contains("montaz-wysokiej-jakosci-zamiennika")) replacement = price;
            if (original != null && replacement != null) break;
        }

        rp.setScreenOriginal(original);
        rp.setScreenReplacement(replacement);
    }

    private static void addHousing(RepairPrices rp, String url) throws IOException {
        TwoPrices twoPrices = getTwoPrices(url);

        rp.setHousing(twoPrices.price2);
        rp.setBackCover(twoPrices.price1);
    }

    private static void addCameraAndItsGlass(RepairPrices rp, String url) throws IOException {
        TwoPrices twoPrices = getTwoPrices(url);

        rp.setCameraGlass(twoPrices.price2);
        rp.setRearCamera(twoPrices.price1);
    }

    private static void addSpeakerAndBuzzer(RepairPrices rp, String phone) throws IOException {
        String url = "";
        if ("iphone-13-pro-max".equals(phone)) {
            url = "https://applefix.pl/produkt/glosnik-" + phone;
        } else {
            url = "https://applefix.pl/produkt/wymiana-glosnika-" + phone;
        }

        TwoPrices twoPrices = getTwoPrices(url);

        rp.setBuzzer(twoPrices.price1);
        rp.setSpeaker(twoPrices.price2);
    }

    private static void addFaceId(RepairPrices rp, String url) throws IOException {
        rp.setTouchFaceId(
                scrape(url).select("div.woovr-variation[data-price]:has(.woovr-variation-name:contains(Naprawa Face ID))")
                        .first().attr("data-price")
        );
    }

    private static void addSideButtons(RepairPrices rp, String url) throws IOException {
        rp.setSideButtons(getSinglePrice(url));
    }

    private static void addMicrophone(RepairPrices rp, String url) throws IOException {
        rp.setMicrophone(getSinglePrice(url));
    }

    private static void addGlass(RepairPrices rp, String url) throws IOException {
        rp.setGlass(getSinglePrice(url));
    }

    private static void addBattery(RepairPrices rp, String url) throws IOException {
        rp.setBattery(getSinglePrice(url));
    }

    private static void addChargingPort(RepairPrices rp, String url) throws IOException {
        rp.setChargingPort(getSinglePrice(url));
    }

    private static void addFrontCamera(RepairPrices rp, String url) throws IOException {
        rp.setFrontCamera(getSinglePrice(url));
    }

    private static String getSinglePrice(String url) throws IOException {
        Document doc = scrape(url);
        Element el = doc.selectFirst("p.price ins .woocommerce-Price-amount bdi");
        return el.ownText().split(",")[0].trim();
    }

    private static TwoPrices getTwoPrices(String url) throws IOException {
        Document doc = scrape(url);

        Iterator<String> it = doc.select("div.woovr-variation[data-price]")
                .eachAttr("data-price")
                .stream()
                .limit(2)
                .iterator();

        return new TwoPrices(it.next(), it.next());
    }

    public static void printPriceInfo(RepairPrices rp, String phone) {
        System.out.println(phone);
        System.out.println("Ekran (org): " + rp.getScreenOriginal());
        System.out.println("Ekran (zam): " + rp.getScreenReplacement());
        System.out.println("Szyba: " + rp.getGlass());
        System.out.println("Bateria: " + rp.getBattery());
        System.out.println("Port: " + rp.getChargingPort());
        System.out.println("Klapka: " + rp.getBackCover());
        System.out.println("Korpus: " + rp.getHousing());
        System.out.println("Face/Touch ID: " + rp.getTouchFaceId());
        System.out.println("Szkla aparatu: " + rp.getCameraGlass());
        System.out.println("Aparat tyl: " + rp.getRearCamera());
        System.out.println("Aparat przod: " + rp.getFrontCamera());
        System.out.println("Speaker: " + rp.getSpeaker());
        System.out.println("Buzzer: " + rp.getBuzzer());
        System.out.println("Mikrofon: " + rp.getMicrophone());
        System.out.println("Przyciski boczne: " + rp.getSideButtons());
        System.out.println();
    }
}
