package org.example.scraper.model;

import java.math.BigDecimal;
import java.util.List;

public class Order {
    private String orderNumber;
    private String paymentStatus;
    private List<PhoneModel> phoneModelList;
    private String totalPrice;
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

    public void setTotalPrice(String price) {
        this.totalPrice = price;
    }

    public String getTotalPrice() {
        return totalPrice;
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

    public List<PhoneModel> getPhoneModelList() {
        return phoneModelList;
    }

    public void setPhoneModelList(List<PhoneModel> phoneModelList) {
        this.phoneModelList = phoneModelList;
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