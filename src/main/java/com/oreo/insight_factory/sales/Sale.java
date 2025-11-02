package com.oreo.insight_factory.sales;

import com.oreo.insight_factory.users.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "sales")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private Integer units;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private String branch;

    @Column(nullable = false)
    private Instant soldAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
}
