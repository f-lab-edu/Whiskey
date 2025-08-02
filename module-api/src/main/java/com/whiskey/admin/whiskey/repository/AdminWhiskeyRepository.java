package com.whiskey.admin.whiskey.repository;

import com.whiskey.domain.whiskey.Whiskey;
import com.whiskey.domain.whiskey.enums.MaltType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminWhiskeyRepository extends JpaRepository<Whiskey, Long>, AdminWhiskeyRepositoryCustom {

}