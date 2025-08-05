package com.whiskey.domain.whiskey.repository;

import com.whiskey.domain.whiskey.Whiskey;
import com.whiskey.domain.whiskey.dto.WhiskeyRegisterCommand;
import com.whiskey.domain.whiskey.dto.WhiskeySearchCommand;
import java.util.List;

public interface WhiskeyRepositoryCustom {
    List<Whiskey> searchWhiskeys(WhiskeySearchCommand whiskeyDto);

    int checkDuplicateWhiskey(WhiskeyRegisterCommand whiskeyDto);
}
