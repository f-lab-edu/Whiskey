package com.whiskey.domain.stock;

import com.whiskey.domain.base.BaseEntity;
import com.whiskey.domain.stock.enums.StatusType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Stock extends BaseEntity {
    @Column(nullable = false)
    private long whiskeyId;

    @Column(nullable = false)
    private String batchVersion;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private double price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusType statusType;
}