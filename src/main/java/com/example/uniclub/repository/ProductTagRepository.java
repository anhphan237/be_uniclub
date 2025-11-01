package com.example.uniclub.repository;

import com.example.uniclub.entity.Product;
import com.example.uniclub.entity.ProductTag;
import com.example.uniclub.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {
    List<ProductTag> findByProduct_ProductId(Long productId);
    void deleteByProductAndTag(Product product, Tag tag);
    @Modifying
    @Query("delete from ProductTag pt where pt.product.productId = :productId and pt.tag.tagId in :tagIds")
    void deleteByProductIdAndTagIds(Long productId, Collection<Long> tagIds);

}
