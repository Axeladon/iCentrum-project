package org.example.scraper.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class Order {
    private String orderNumber;
    private String paymentStatus;
    private List<PhoneModel> phoneModelList;
    private BigDecimal totalPrice;
    private String parcelMachineNum;
    private boolean personalPickup;
    private String nip;
    private String clientName;
    private LocalDate declaredShippingDate;
    private LocalDate orderSubmissionDate;
    private Address address;

    public Order() {}

    // copy constructor
    public Order(Order other) {
        this.orderNumber = other.orderNumber;
        this.paymentStatus = other.paymentStatus;
        this.totalPrice = other.totalPrice;
        this.parcelMachineNum = other.parcelMachineNum;
        this.nip = other.nip;
        this.clientName = other.clientName;
        this.declaredShippingDate = other.declaredShippingDate;
        this.personalPickup = other.personalPickup;
        this.orderSubmissionDate = other.orderSubmissionDate;
        this.phoneModelList = new ArrayList<>(other.phoneModelList);
        this.address = new Address(other.address);
    }

}