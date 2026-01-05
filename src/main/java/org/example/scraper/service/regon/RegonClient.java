package org.example.scraper.service.regon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import java.util.List;

public class RegonClient {

    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String URL =
            "https://wyszukiwarkaregon.stat.gov.pl/wsBIR/UslugaBIRzewnPubl.svc/ajaxEndpoint/daneSzukaj";

    private static String decrypt(String enc) {
        if (!enc.startsWith("enc:")) return enc;

        enc = enc.substring(4);
        String[] parts = enc.split("!");
        StringBuilder sb = new StringBuilder();

        for (String part : parts) {
            if (part.length() < 3) continue;

            char prefix = part.charAt(0);
            int base = switch (prefix) {
                case '1' -> 32;
                case '2' -> 48;
                case '3' -> 64;
                case '4' -> 80;
                case '5' -> 96;
                default -> 0;
            };

            int val = Integer.parseInt(part.substring(1));
            sb.append((char) (base + val));
        }
        return sb.toString();
    }

    public RegonRecord findByNip(String nip) {
        try {
            RegonSearchRequest req = new RegonSearchRequest();
            req.params.nip = nip;

            RequestBody body = RequestBody.create(
                    mapper.writeValueAsString(req),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(URL)
                    .post(body)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Mozilla/5.0")
                    .build();

            Response response = client.newCall(request).execute();

            String encrypted = mapper.readTree(response.body().string()).get("d").asText();
            String decrypted = decrypt(encrypted);

            List<RegonRecord> list = mapper.readValue(
                    decrypted,
                    new TypeReference<List<RegonRecord>>() {}
            );

            return list.isEmpty() ? null : list.get(0);

        } catch (Exception e) {
            throw new RuntimeException("REGON API error", e);
        }
    }
}
