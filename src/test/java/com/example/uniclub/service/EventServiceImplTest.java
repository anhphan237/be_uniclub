package com.example.uniclub.service;

import com.example.uniclub.dto.request.EventCreateRequest;
import com.example.uniclub.dto.response.EventResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.impl.EventServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock EventRepository eventRepo;
    @Mock ClubRepository clubRepo;
    @Mock LocationRepository locationRepo;
    @Mock MembershipRepository membershipRepo;
    @Mock WalletRepository walletRepo;
    @Mock EventStaffRepository eventStaffRepo;
    @Mock EventRegistrationRepository eventRegistrationRepo;
    @Mock WalletTransactionRepository walletTransactionRepo;
    @Mock ProductRepository productRepo;
    @Mock EventRegistrationRepository regRepo;
    @Mock EventPointsService eventPointsService;
    @Mock EmailService emailService;

    @InjectMocks
    EventServiceImpl eventService;

    Club hostClub;
    Location location;

    @BeforeEach
    void setup() {
        hostClub = Club.builder()
                .clubId(1L)
                .name("Host Club")
                .build();

        location = Location.builder()
                .locationId(10L)
                .capacity(200)
                .name("Hall A")
                .build();
    }

    private EventCreateRequest createRequest(
            Long hostClubId,
            List<Long> coHosts,
            String name,
            EventTypeEnum type,
            Long locationId
    ) {
        return new EventCreateRequest(
                hostClubId,
                coHosts,
                name,
                "desc",
                type,
                LocalDate.now().plusDays(1),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                locationId,
                120,
                0,
                null
        );
    }

    // =================================================================================
    // CREATE() — LOCATION NOT FOUND
    // =================================================================================
    @Test
    void testCreate_locationNotFound() {

        EventCreateRequest req = createRequest(
                1L,
                List.of(),
                "Event 1",
                EventTypeEnum.PUBLIC,
                999L  // WRONG
        );

        when(locationRepo.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> eventService.create(req));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Location not found.", ex.getMessage());
    }

    // =================================================================================
    // CREATE() — HOST CLUB NOT FOUND
    // =================================================================================
    @Test
    void testCreate_hostClubNotFound() {

        EventCreateRequest req = createRequest(
                999L,               // WRONG club id
                List.of(),
                "Event 1",
                EventTypeEnum.PUBLIC,
                10L
        );

        when(locationRepo.findById(10L)).thenReturn(Optional.of(location));
        when(clubRepo.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> eventService.create(req));

        assertEquals("Host club not found.", ex.getMessage());
    }

    // =================================================================================
    // CREATE() — PUBLIC EVENT — SUCCESS
    // =================================================================================
    @Test
    void testCreate_publicEvent_success() {

        EventCreateRequest req = createRequest(
                1L,
                List.of(),
                "Public Event",
                EventTypeEnum.PUBLIC,
                10L
        );

        when(locationRepo.findById(10L)).thenReturn(Optional.of(location));
        when(clubRepo.findById(1L)).thenReturn(Optional.of(hostClub));

        when(eventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventResponse res = eventService.create(req);

        assertEquals("Public Event", res.getName());
        assertEquals(120, res.getMaxCheckInCount());
        assertEquals(EventStatusEnum.PENDING_UNISTAFF, res.getStatus());

        verify(emailService, times(1))
                .sendEventAwaitingUniStaffReviewEmail(any(), eq("Public Event"), anyString());
    }
}
