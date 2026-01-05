package org.example.scraper.service.regon;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RegonSearchRequest {

    @JsonProperty("jestWojPowGmnMiej")
    public boolean includeLocation = true;

    @JsonProperty("pParametryWyszukiwania")
    public SearchParams params = new SearchParams();

    public static class SearchParams {

        @JsonProperty("Nip")
        public String nip;

        @JsonProperty("Regon")
        public String regon;

        @JsonProperty("Krs")
        public String krs;

        @JsonProperty("NazwaPodmiotu")
        public String name;

        @JsonProperty("PrzeważajacePKD")
        public boolean mainPkd = false;
    }
}
