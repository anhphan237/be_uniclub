package com.example.uniclub.repository;

import com.example.uniclub.entity.Product;
import com.example.uniclub.entity.ProductTag;
import com.example.uniclub.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {
    List<ProductTag> findByProduct_ProductId(Long productId);
    void deleteByProductAndTag(Product product, Tag tag);

}
