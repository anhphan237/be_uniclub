package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_categories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id","category_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductCategory {

    @EmbeddedId
    private ProductCategoryId id;

    @ManyToOne @MapsId("productId")
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne @MapsId("categoryId")
    @JoinColumn(name = "category_id")
    private Category category;
}
