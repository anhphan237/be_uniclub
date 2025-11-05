package com.example.uniclub.repository;

import com.example.uniclub.entity.Product;
import com.example.uniclub.entity.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductMediaRepository extends JpaRepository<ProductMedia, Long> {
    List<ProductMedia> findByProductOrderByDisplayOrderAscMediaIdAsc(Product product);
    List<ProductMedia> findByProduct(Product product);
    long countByProduct(Product product);


}
