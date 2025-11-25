package com.whiskey.domain.whiskey.service;

import com.whiskey.domain.whiskey.dto.WhiskeyCursorResponse;
import com.whiskey.domain.whiskey.dto.WhiskeyInfo;
import com.whiskey.domain.whiskey.dto.WhiskeyCommand;
import com.whiskey.domain.whiskey.dto.WhiskeySearchCondition;
import com.whiskey.domain.whiskey.repository.WhiskeyRepository;
import com.whiskey.domain.whiskey.Cask;
import com.whiskey.domain.whiskey.Whiskey;
import com.whiskey.exception.ErrorCode;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhiskeyService {

    private final WhiskeyRepository whiskeyRepository;

    @Transactional
    public void register(WhiskeyCommand whiskeyDto) {
        checkDuplicate(whiskeyDto);

        Whiskey whiskey = Whiskey.builder()
            .distillery(whiskeyDto.distillery())
            .name(whiskeyDto.name())
            .country(whiskeyDto.country())
            .age(whiskeyDto.age())
            .maltType(whiskeyDto.maltType())
            .abv(whiskeyDto.abv())
            .volume(whiskeyDto.volume())
            .description(whiskeyDto.description())
            .build();

        List<Cask> casks = whiskeyDto.casks().stream()
                .map(caskDto -> {
                    Cask cask = new Cask();
                    cask.setType(caskDto.type());
                    return cask;
                }).toList();

        whiskey.addCasks(casks);
        whiskeyRepository.save(whiskey);
    }

    private void checkDuplicate(WhiskeyCommand whiskeyDto) {
        int count = whiskeyRepository.checkDuplicateWhiskey(whiskeyDto);

        if(count > 0) {
            throw ErrorCode.CONFLICT.exception("이미 등록된 위스키입니다.");
        }
    }

    @Transactional
    public void update(Long id, WhiskeyCommand whiskeyDto) {
        checkDuplicate(whiskeyDto);

        Whiskey whiskey = whiskeyRepository.findById(id).orElseThrow(() -> ErrorCode.NOT_FOUND.exception("위스키를 찾을 수 없습니다."));

        whiskey.setDistillery(whiskeyDto.distillery());
        whiskey.setName(whiskeyDto.name());
        whiskey.setCountry(whiskeyDto.country());
        whiskey.setAge(whiskeyDto.age());
        whiskey.setMaltType(whiskeyDto.maltType());
        whiskey.setAbv(whiskeyDto.abv());
        whiskey.setVolume(whiskeyDto.volume());
        whiskey.setDescription(whiskeyDto.description());

        whiskey.getCasks().clear();
        List<Cask> newCasks = whiskeyDto.casks().stream()
            .map(CaskDto -> {
                Cask cask = new Cask();
                cask.setType(CaskDto.type());
                return cask;
            }).toList();

        whiskey.getCasks().addAll(newCasks);
    }

    @Transactional
    public void delete(Long id) {
        if(!whiskeyRepository.existsById(id)) {
            throw ErrorCode.NOT_FOUND.exception("해당 위스키가 존재하지 않습니다.");
        }

        whiskeyRepository.deleteById(id);
    }

    public WhiskeyInfo findById(Long id) {
        Whiskey whiskey = whiskeyRepository.findById(id).orElseThrow(() -> ErrorCode.NOT_FOUND.exception("위스키를 찾을 수 없습니다."));
        return WhiskeyInfo.from(whiskey);
    }

    public WhiskeyCursorResponse<WhiskeyInfo> searchWhiskeys(WhiskeySearchCondition whiskeyDto) {
        List<Whiskey> whiskeys = whiskeyRepository.searchWhiskeys(whiskeyDto);

        boolean hasNext = whiskeys.size() > whiskeyDto.size();
        if(hasNext) {
            whiskeys.remove(whiskeys.size() - 1);
        }

        String nextCursor = null;
        if(hasNext && !whiskeys.isEmpty()) {
            Whiskey lastWhiskey = whiskeys.get(whiskeys.size() - 1);
            nextCursor = String.valueOf(lastWhiskey.getId());
        }

        List<WhiskeyInfo> whiskeyInfos = whiskeys.stream().map(WhiskeyInfo::from).toList();
        return WhiskeyCursorResponse.of(whiskeyInfos, nextCursor, hasNext);
    }
}
