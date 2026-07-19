package org.example.scraper.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
public class Order {
    private String orderId;
    private String paymentMethod;
    private String paymentStatus;
    private List<DeviceModel> deviceModelList;
    private BigDecimal totalPrice;
    private String parcelMachineNum;
    private boolean personalPickup;
    private String nip;
    private String companyName;
    private String clientName;
    private LocalDate declaredShippingDate;
    private LocalDate orderSubmissionDate;
    private Address address;
    private String email;
    private OrderNotes notes;

    public Order() {}
}