//package com.example.uniclub.dto.request;
//
//import jakarta.validation.constraints.*;
//
//public record RedeemRequest(
//        @NotNull Long productId,
//        @NotNull @Min(1) Integer quantity,
//        Long membershipId, // nếu member redeem trong flow cá nhân
//        Long eventId       // nếu redeem trong booth của event
//) {}
