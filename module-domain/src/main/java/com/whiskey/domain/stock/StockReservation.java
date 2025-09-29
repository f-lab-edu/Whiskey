package com.whiskey.domain.stock;

import com.whiskey.domain.base.BaseEntity;
import com.whiskey.domain.stock.enums.ReservationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockReservation extends BaseEntity {
    @Column(nullable = false)
    private long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id")
    private Stock stockId;

    @Column(nullable = false)
    private int reservedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationType reservationType;

    @CreatedDate
    @Column(name = "expire_at")
    private LocalDateTime expireAt;
}