package com.example.uniclub.repository;

import com.example.uniclub.entity.Product;
import com.example.uniclub.entity.ProductStockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductStockHistoryRepository extends JpaRepository<ProductStockHistory, Long> {
    List<ProductStockHistory> findByProductOrderByChangedAtDesc(Product product);
}
