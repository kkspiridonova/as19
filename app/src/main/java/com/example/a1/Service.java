package com.example.a1;

public class Service {
    private String serviceId;
    private String serviceName;
    private double price;
    private String category;

    public Service() {
    }

    public Service(String serviceId, String serviceName, double price, String category) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.price = price;
        this.category = category;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}