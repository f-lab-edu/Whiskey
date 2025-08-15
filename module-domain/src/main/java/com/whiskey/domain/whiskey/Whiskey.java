package com.whiskey.domain.whiskey;

import com.whiskey.domain.base.BaseEntity;
import com.whiskey.domain.whiskey.enums.MaltType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Whiskey extends BaseEntity {
    @Column(nullable = false)
    private String distillery;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String country;

    @Column
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaltType maltType;

    @Column(nullable = false)
    private double abv;

    @Column(nullable = false)
    private int volume;

    @Column
    private String description;

    @Column(name = "image_path")
    private String imagePath;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "whiskey_id")
    private List<Cask> casks = new ArrayList<>();

    @Column(name = "avg_rating")
    private double avgRating;

    @Column(name = "review_count")
    private int reviewCount;

    @Version
    private long version;

    @Builder
    public Whiskey(String distillery, String name, String country, Integer age, int volume, MaltType maltType, double abv, String description) {
        this.distillery = distillery;
        this.name = name;
        this.country = country;
        this.age = age;
        this.maltType = maltType;
        this.abv = abv;
        this.volume = volume;
        this.description = description;
    }

    public void addCasks(List<Cask> casks) {
        if(casks != null && !casks.isEmpty()) {
            this.casks.addAll(casks);
        }
    }

    public void updateRating(double avgRating, int reviewCount) {
        this.avgRating = avgRating;
        this.reviewCount = reviewCount;
    }
}
