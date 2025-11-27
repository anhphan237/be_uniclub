package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "return_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ảnh lỗi thuộc 1 đơn hàng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private ProductOrder order;

    // URL trả về từ Cloudinary
    @Column(nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    // Cloudinary publicId để xoá file
    @Column(nullable = false)
    private String publicId;

    // Thứ tự hiển thị ảnh
    @Column(nullable = false)
    private Integer displayOrder;
}
