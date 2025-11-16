//package com.example.uniclub.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//@Entity
//@Table(name = "event_allowed_clubs",
//        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id","club_id"}))
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class EventAllowedClub {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(optional = false)
//    @JoinColumn(name = "event_id")
//    private Event event;
//
//    @ManyToOne(optional = false)
//    @JoinColumn(name = "club_id")
//    private Club club;
//}
