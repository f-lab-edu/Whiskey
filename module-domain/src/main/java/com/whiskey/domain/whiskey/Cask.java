package com.whiskey.domain.whiskey;

import com.whiskey.domain.base.BaseEntity;
import com.whiskey.domain.whiskey.enums.CaskType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Cask extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaskType type;

}
