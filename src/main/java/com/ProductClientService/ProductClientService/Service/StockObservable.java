package com.ProductClientService.ProductClientService.Service;

import java.util.List;

import com.ProductClientService.ProductClientService.Model.ProductVariant;

public interface StockObservable {
    public void addObserver(StockObserver observer);

    public void removeObserver(StockObserver observer);

    public void notifyObservers(ProductVariant variant, List<String> emails);
}