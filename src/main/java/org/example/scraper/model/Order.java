package org.example.scraper.model;

public class Order {
    private String orderNumber;
    private String paymentStatus;
    private boolean chargerIncluded;
    private PhoneModel phoneModel;
    private String price;
    private String parcelMachineNum;
    private String nip;
    private String clientName;
    private String declaredShippingDate;
    private boolean personalPickup;

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getPrice() {
        return price;
    }

    public void setParcelMachineNum(String parcelMachineNum) {
        this.parcelMachineNum = parcelMachineNum;
    }

    public String getParcelMachineNum() {
        return parcelMachineNum;
    }

    public void setNip(String nip) {
        this.nip = nip;
    }

    public String getNip() {
        return nip;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setChargerIncluded(boolean chargerIncluded) {
        this.chargerIncluded = chargerIncluded;
    }

    public boolean isChargerIncluded() {
        return chargerIncluded;
    }

    public void setPhoneModel(PhoneModel phoneModel) {
        this.phoneModel = phoneModel;
    }

    public PhoneModel getPhoneModel() {
        return phoneModel;
    }

    public String getDeclaredShippingDate() {
        return declaredShippingDate;
    }

    public void setDeclaredShippingDate(String shippingEstimate) {
        this.declaredShippingDate = shippingEstimate;
    }

    public boolean isPersonalPickup() {
        return personalPickup;
    }

    public void setPersonalPickup(boolean hasPersonalPickup) {
        this.personalPickup = hasPersonalPickup;
    }
}