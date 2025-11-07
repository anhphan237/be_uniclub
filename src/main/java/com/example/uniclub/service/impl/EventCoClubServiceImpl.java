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

        EventCoClub relation = eventCoClubRepository.findByEvent_EventIdAndClub_ClubId(eventId, clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Co-host relationship not found"));


        if (relation.getStatus() == newStatus) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Status is already " + newStatus);
        }

        relation.setStatus(newStatus);
        eventCoClubRepository.save(relation);

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
