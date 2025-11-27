package com.example.uniclub.repository;

import com.example.uniclub.entity.ReturnImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnImageRepository extends JpaRepository<ReturnImage, Long> {

    List<ReturnImage> findByOrder_OrderId(Long orderId);

    List<ReturnImage> findByOrder_OrderIdOrderByDisplayOrderAsc(Long orderId);

    int countByOrder_OrderId(Long orderId);   // 🔥 dùng để set displayOrder

    void deleteByIdAndOrder_OrderId(Long imageId, Long orderId);
}
