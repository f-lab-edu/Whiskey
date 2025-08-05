package com.whiskey.domain.whiskey.repository;

import com.whiskey.domain.whiskey.Whiskey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhiskeyRepository extends JpaRepository<Whiskey, Long>, WhiskeyRepositoryCustom {

}