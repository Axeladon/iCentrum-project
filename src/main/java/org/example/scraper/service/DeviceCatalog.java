package org.example.scraper.service;

import com.google.gson.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DeviceCatalog {
    private final Map<String, String> productTypeToModelName = new HashMap<>();

    // ProductType -> (color code -> color name) (e.g. "iPhone12,3" -> (2 -> "Silver"))
    private final Map<String, Map<Integer, String>> productTypeToColorByCode = new HashMap<>();

    // Simple constructor: read and parse file once
    public DeviceCatalog(Path devicesTablePath) throws IOException {
        Objects.requireNonNull(devicesTablePath, "devicesTablePath");

        String jsonText = Files.readString(devicesTablePath, StandardCharsets.UTF_8);
        JsonObject root = parseJsonSmart(jsonText);

        buildProductTypeToModelName(root);
        buildProductTypeToColorByCode(root);
    }

    // Try to parse JSON text; if there is garbage around, cut from first '{' to last '}'
    private JsonObject parseJsonSmart(String text) {
        try {
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (JsonSyntaxException ignored) {

        }

        int first = text.indexOf('{');
        int last = text.lastIndexOf('}');
        if (first < 0 || last <= first) {
            throw new IllegalStateException("Cannot locate JSON object in devices_table file");
        }
        String core = text.substring(first, last + 1);
        return JsonParser.parseString(core).getAsJsonObject();
    }

    // Fill map: ProductType -> ModelName
    private void buildProductTypeToModelName(JsonObject root) {
        JsonArray models = root.getAsJsonArray("models");
        if (models != null) {
            for (JsonElement el : models) {
                if (!el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();
                if (!obj.has("ProductTypes") || !obj.has("ModelName")) continue;

                String productTypes = obj.get("ProductTypes").getAsString();
                String modelName = obj.get("ModelName").getAsString();

                String[] parts = productTypes.split("[;,]");
                for (String p : parts) {
                    String pt = p.trim();
                    if (!pt.isEmpty()) {
                        productTypeToModelName.putIfAbsent(pt, modelName);
                    }
                }
            }
        }

        JsonArray devices = root.getAsJsonArray("devices");
        if (devices != null) {
            for (JsonElement el : devices) {
                if (!el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();
                if (!obj.has("ProductType") || !obj.has("ProductName")) continue;

                String pt = obj.get("ProductType").getAsString();
                String name = obj.get("ProductName").getAsString();
                productTypeToModelName.putIfAbsent(pt, name);
            }
        }
    }

    // ProductType -> (BackColor numeric code -> BackColorText)
    private void buildProductTypeToColorByCode(JsonObject root) {
        JsonArray images = root.getAsJsonArray("Images");
        if (images == null) return;

        Pattern numeric = Pattern.compile("\\d+");

        for (JsonElement blockEl : images) {
            if (!blockEl.isJsonObject()) continue;
            JsonObject block = blockEl.getAsJsonObject();

            JsonArray supportTypes = block.getAsJsonArray("SupportTypes");
            JsonArray imgs = block.getAsJsonArray("imgs");
            if (supportTypes == null || imgs == null) continue;

            // Collect product types from this block
            Map<Integer, String> codesInBlock = new HashMap<>();

            for (JsonElement imgEl : imgs) {
                if (!imgEl.isJsonObject()) continue;
                JsonObject img = imgEl.getAsJsonObject();
                if (!img.has("BackColor") || !img.has("BackColorText")) continue;

                String codeStr = img.get("BackColor").getAsString();
                Matcher m = numeric.matcher(codeStr);
                if (!m.matches()) continue; // skip non-numeric codes like "black"

                int code = Integer.parseInt(codeStr);
                String colorName = img.get("BackColorText").getAsString();

                // keep first name for this code inside block
                codesInBlock.putIfAbsent(code, colorName);
            }

            if (codesInBlock.isEmpty()) continue;

            // Assign collected codes to each ProductType in this block
            for (JsonElement stEl : supportTypes) {
                if (!stEl.isJsonObject()) continue;
                JsonObject st = stEl.getAsJsonObject();
                if (!st.has("ProductType")) continue;

                String pt = st.get("ProductType").getAsString();
                Map<Integer, String> mapForPt =
                        productTypeToColorByCode.computeIfAbsent(pt, k -> new HashMap<>());

                for (Map.Entry<Integer, String> e : codesInBlock.entrySet()) {
                    mapForPt.putIfAbsent(e.getKey(), e.getValue());
                }
            }
        }
    }

    public String getModelName(String productType) {
        return productTypeToModelName.get(productType);
    }

    public String getColorName(String productType, int colorCode) {
        Map<Integer, String> map = productTypeToColorByCode.get(productType);
        if (map == null) return null;
        return map.get(colorCode);
    }
}
