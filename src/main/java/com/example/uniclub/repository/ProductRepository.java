package com.example.uniclub.repository;

import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.Product;
import com.example.uniclub.enums.ProductStatusEnum;
import com.example.uniclub.enums.ProductTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByClubAndTypeAndIsActiveTrue(Club club, ProductTypeEnum type);
    List<Product> findByEventAndIsActiveTrue(Event event);

    // Bổ sung:
    Page<Product> findByClub_ClubId(Long clubId, Pageable pageable);
    Page<Product> findByClub_ClubIdAndStatus(Long clubId, ProductStatusEnum status, Pageable pageable);
    Page<Product> findByEvent_EventId(Long eventId, Pageable pageable);
    Page<Product> findByEvent_EventIdAndStatus(Long eventId, ProductStatusEnum status, Pageable pageable);
    Page<Product> findByStatus(ProductStatusEnum status, Pageable pageable);

    Optional<Product> findByClubAndNameIgnoreCase(Club club, String name);
}
