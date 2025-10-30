package com.example.uniclub.service.impl;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.entity.Card;
import com.example.uniclub.entity.Club;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.CardRepository;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepo;
    private final ClubRepository clubRepo;

    // 🟢 Tạo hoặc cập nhật Card
    @Override
    public ApiResponse<Card> saveOrUpdate(Long clubId, Card req) {
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        // 🔎 Nếu CLB đã có Card thì cập nhật
        Card card = cardRepo.findFirstByClub_ClubId(clubId).orElse(null);

        if (card == null) {
            card = new Card();
            card.setClub(club);
        }

        card.setBorderRadius(req.getBorderRadius());
        card.setCardColorClass(req.getCardColorClass());
        card.setCardOpacity(req.getCardOpacity());
        card.setColorType(req.getColorType());
        card.setGradient(req.getGradient());
        card.setLogoSize(req.getLogoSize());
        card.setPattern(req.getPattern());
        card.setPatternOpacity(req.getPatternOpacity());
        card.setQrPosition(req.getQrPosition());
        card.setQrSize(req.getQrSize());
        card.setQrStyle(req.getQrStyle());
        card.setShowLogo(req.getShowLogo());
        card.setLogoUrl(req.getLogoUrl());

        cardRepo.save(card);

        return ApiResponse.ok(card);
    }

    // 🔵 Lấy Card theo clubId
    @Override
    public List<Card> getByClub(Long clubId) {
        return cardRepo.findByClub_ClubId(clubId);
    }

    // 🟣 Lấy Card theo id
    @Override
    public Card getById(Long id) {
        return cardRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Card not found"));
    }

    // 🔴 Xóa Card
    @Override
    public ApiResponse<String> delete(Long id) {
        if (!cardRepo.existsById(id))
            throw new ApiException(HttpStatus.NOT_FOUND, "Card not found");

        cardRepo.deleteById(id);
        return ApiResponse.ok("Card deleted successfully");
    }
}
