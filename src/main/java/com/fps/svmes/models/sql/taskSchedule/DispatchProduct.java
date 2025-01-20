package com.fps.svmes.models.sql.taskSchedule;

import com.fps.svmes.models.sql.production.Product;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dispatch_product", schema = "quality_management")
@Data
@NoArgsConstructor
public class DispatchProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_id", nullable = false)
    private Dispatch dispatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "status", nullable = false, columnDefinition = "SMALLINT DEFAULT 1")
    private Short status = 1; // Default active

    public DispatchProduct(Dispatch dispatch, Product product) {
        this.dispatch = dispatch;
        this.product = product;
    }
}

