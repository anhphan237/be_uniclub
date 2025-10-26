package com.example.uniclub.service.impl;

import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.EventCoClub;
import com.example.uniclub.enums.EventCoHostStatusEnum;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.EventCoClubRepository;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.service.EventCoClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventCoClubServiceImpl implements EventCoClubService {

    private final EventCoClubRepository eventCoClubRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public void updateStatus(Long eventId, Long clubId, EventCoHostStatusEnum newStatus) {
        // 1️⃣ Tìm mối quan hệ giữa event và club
        EventCoClub relation = eventCoClubRepository.findByEvent_EventIdAndClub_ClubId(eventId, clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy mối quan hệ đồng tổ chức"));

        // 2️⃣ Kiểm tra trạng thái hiện tại
        if (relation.getStatus() == newStatus) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Trạng thái đã là " + newStatus);
        }

        // 3️⃣ Cập nhật trạng thái mới
        relation.setStatus(newStatus);
        eventCoClubRepository.save(relation);

        // 4️⃣ Nếu tất cả cohost đều APPROVED → event chuyển sang READY
        if (newStatus == EventCoHostStatusEnum.APPROVED) {
            Event event = relation.getEvent();
            List<EventCoClub> allRelations = eventCoClubRepository.findAllByEvent_EventId(eventId);

            boolean allApproved = allRelations.stream()
                    .allMatch(rel -> rel.getStatus() == EventCoHostStatusEnum.APPROVED);

            if (allApproved) {
                event.setStatus(EventStatusEnum.APPROVED);
                eventRepository.save(event);
            }
        }
    }
}
