package com.example.uniclub.service.impl;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.CardRequest;
import com.example.uniclub.dto.response.CardResponse;
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

    // 🟢 Tạo hoặc cập nhật Card cho CLB
    @Override
    public ApiResponse<CardResponse> saveOrUpdate(Long clubId, CardRequest req) {
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        // 🔎 Nếu CLB đã có Card thì cập nhật (overwrite)
        Card card = cardRepo.findFirstByClub_ClubId(clubId).orElse(new Card());
        card.setClub(club);

        // 🧱 Ghi đè các field
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

        return ApiResponse.ok(toResponse(card));
    }

    // 🔵 Lấy Card theo clubId
    @Override
    public CardResponse getByClubId(Long clubId) {
        Card card = cardRepo.findFirstByClub_ClubId(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Card not found for this club"));
        return toResponse(card);
    }

    // 🟣 Lấy Card theo cardId
    @Override
    public CardResponse getById(Long id) {
        Card card = cardRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Card not found"));
        return toResponse(card);
    }

    // ⚪ Lấy tất cả Card (cho ADMIN & STAFF)
    @Override
    public List<CardResponse> getAll() {
        return cardRepo.findAll().stream().map(this::toResponse).toList();
    }

    // 🔴 Xóa Card
    @Override
    public ApiResponse<String> delete(Long id) {
        if (!cardRepo.existsById(id))
            throw new ApiException(HttpStatus.NOT_FOUND, "Card not found");
        cardRepo.deleteById(id);
        return ApiResponse.ok("Card deleted successfully");
    }

    // 🧩 Hàm mapper: Entity → DTO
    private CardResponse toResponse(Card card) {
        return CardResponse.builder()
                .cardId(card.getCardId())
                .clubId(card.getClub().getClubId())
                .clubName(card.getClub().getName())
                .borderRadius(card.getBorderRadius())
                .cardColorClass(card.getCardColorClass())
                .cardOpacity(card.getCardOpacity())
                .colorType(card.getColorType())
                .gradient(card.getGradient())
                .logoSize(card.getLogoSize())
                .pattern(card.getPattern())
                .patternOpacity(card.getPatternOpacity())
                .qrPosition(card.getQrPosition())
                .qrSize(card.getQrSize())
                .qrStyle(card.getQrStyle())
                .showLogo(card.getShowLogo())
                .logoUrl(card.getLogoUrl())
                .createdAt(card.getCreatedAt())
                .build();
    }
}
