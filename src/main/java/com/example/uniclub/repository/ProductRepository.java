package com.example.uniclub.repository;

import com.example.uniclub.entity.Product;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Event;
import com.example.uniclub.enums.ProductTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByClubAndTypeAndIsActiveTrue(Club club, ProductTypeEnum type);
    List<Product> findByEventAndIsActiveTrue(Event event);
}
