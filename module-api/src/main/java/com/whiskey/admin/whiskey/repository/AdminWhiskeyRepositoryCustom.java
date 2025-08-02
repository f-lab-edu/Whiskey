package com.whiskey.admin.whiskey.repository;

import com.whiskey.admin.whiskey.dto.WhiskeyRegisterDto;
import com.whiskey.admin.whiskey.dto.WhiskeySearchDto;
import com.whiskey.domain.whiskey.Whiskey;
import java.util.List;

public interface AdminWhiskeyRepositoryCustom {
    List<Whiskey> searchWhiskeys(WhiskeySearchDto whiskeyDto);

    int checkDuplicateWhiskey(WhiskeyRegisterDto whiskeyDto);
}
