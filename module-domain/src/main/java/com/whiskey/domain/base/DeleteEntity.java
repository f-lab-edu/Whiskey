package com.whiskey.domain.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;

@MappedSuperclass
@Getter
public abstract class DeleteEntity extends BaseEntity {
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}