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
    private String paymentMethod;
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
    private String email;

    public Order() {}

    // copy constructor
    public Order(Order other) {
        this.orderNumber = other.orderNumber;
        this.paymentMethod = other.paymentMethod;
        this.paymentStatus = other.paymentStatus;
        this.phoneModelList = new ArrayList<>(other.phoneModelList);
        this.totalPrice = other.totalPrice;
        this.parcelMachineNum = other.parcelMachineNum;
        this.personalPickup = other.personalPickup;
        this.nip = other.nip;
        this.clientName = other.clientName;
        this.declaredShippingDate = other.declaredShippingDate;
        this.orderSubmissionDate = other.orderSubmissionDate;
        this.address = new Address(other.address);
        this.email = other.email;
    }
}