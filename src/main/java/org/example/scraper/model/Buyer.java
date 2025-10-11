package org.example.scraper.model;

public record Buyer(String clientName, String nip, Address address) {
    public Buyer {
        nip = (nip != null && nip.isBlank()) ? null : nip;
        clientName = (clientName == null) ? "" : clientName;
    }

    public static Buyer fromOrder(Order o) {
        Address addrCopy = new Address(o.getAddress());
        return new Buyer(o.getClientName(), o.getNip(), addrCopy);
    }
}
