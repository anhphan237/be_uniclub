package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.RedeemOrderRequest;
import com.example.uniclub.dto.request.RefundRequest;
import com.example.uniclub.dto.response.OrderResponse;
import com.example.uniclub.entity.ProductOrder;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.RedeemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Redeem & Order Management",
        description = """
        Quản lý quá trình đổi quà / đặt hàng sản phẩm:
        - Sinh viên đổi quà từ kho CLB hoặc sự kiện
        - Staff/Leader xác nhận, hoàn điểm, hoặc xử lý lỗi sản phẩm
        - Tra cứu lịch sử đơn hàng của Member, CLB, hoặc Event
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/redeem")
@RequiredArgsConstructor
public class RedeemController {

    private final RedeemService redeemService;

    // ==========================================================
    // 🟢 1. MEMBER ĐẶT HÀNG TỪ KHO CLB
    // ==========================================================
    @Operation(
            summary = "Member đặt hàng từ kho CLB",
            description = """
                Dành cho **STUDENT**.<br>
                Khi thành viên đặt hàng sản phẩm từ kho CLB, điểm sẽ bị trừ ngay lập tức và đơn được tạo ở trạng thái `PENDING`.<br>
                Sau đó CLB sẽ xác nhận để hoàn tất đơn hàng.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Tạo đơn hàng thành công")
    )
    @PostMapping("/club/{clubId}/order")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<OrderResponse>> createClubOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long clubId,
            @RequestBody RedeemOrderRequest req
    ) {
        OrderResponse res = redeemService.createClubOrder(clubId, req, principal.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    // ==========================================================
    // 🟠 2. STAFF ĐỔI QUÀ TRỰC TIẾP TẠI SỰ KIỆN
    // ==========================================================
    @Operation(
            summary = "Staff đổi quà trực tiếp tại booth sự kiện",
            description = """
                Dành cho **CLUB_LEADER**, **VICE_LEADER** hoặc **STAFF**.<br>
                Khi staff đổi quà trực tiếp cho người tham gia tại sự kiện, hệ thống sẽ trừ điểm và hoàn tất đơn (`COMPLETED`) ngay lập tức.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Đổi quà thành công")
    )
    @PostMapping("/event/{eventId}/redeem")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','STAFF')")
    public ResponseEntity<ApiResponse<OrderResponse>> eventRedeem(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long eventId,
            @RequestBody RedeemOrderRequest req
    ) {
        OrderResponse res = redeemService.eventRedeem(eventId, req, principal.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    // ==========================================================
    // 🔵 3. LEADER/STAFF XÁC NHẬN ĐƠN HÀNG (COMPLETE)
    // ==========================================================
    @Operation(
            summary = "Xác nhận hoàn tất đơn hàng CLB (COMPLETE)",
            description = """
                Dành cho **CLUB_LEADER**, **VICE_LEADER**, hoặc **STAFF**.<br>
                Sau khi thành viên đến nhận quà, CLB xác nhận đơn từ `PENDING` → `COMPLETED`.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Đơn hàng đã được hoàn tất")
    )
    @PutMapping("/order/{orderId}/complete")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','STAFF')")
    public ResponseEntity<ApiResponse<OrderResponse>> complete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long orderId
    ) {
        OrderResponse res = redeemService.complete(orderId, principal.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    // ==========================================================
    // 🟤 4. HOÀN ĐIỂM ĐƠN HÀNG (FULL REFUND)
    // ==========================================================
    @Operation(
            summary = "Hoàn điểm toàn bộ cho đơn hàng (có lý do)",
            description = """
            Dành cho **CLUB_LEADER**, **VICE_LEADER**, hoặc **STAFF**.<br>
            Khi sản phẩm lỗi hoặc giao sai. Nhập lý do refund để hệ thống ghi log.
            """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Hoàn điểm thành công")
    )
    @PutMapping("/order/refund")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','STAFF')")
    public ResponseEntity<ApiResponse<OrderResponse>> refund(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody RefundRequest req
    ) {
        OrderResponse res = redeemService.refund(
                req.orderId(),
                principal.getUser().getUserId(),
                req.reason()
        );
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    // ==========================================================
    // 🟡 5. HOÀN ĐIỂM MỘT PHẦN (PARTIAL REFUND)
    // ==========================================================
    @Operation(
            summary = "Hoàn điểm một phần cho đơn hàng (có lý do)",
            description = """
            Dành cho **CLUB_LEADER** hoặc **VICE_LEADER**.<br>
            Cho phép hoàn lại một phần điểm kèm lý do hoàn hàng.
            """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Hoàn điểm một phần thành công")
    )
    @PutMapping("/order/refund-partial")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<OrderResponse>> refundPartial(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody RefundRequest req
    ) {
        OrderResponse res = redeemService.refundPartial(
                req.orderId(),
                req.quantityToRefund(),
                principal.getUser().getUserId(),
                req.reason()
        );
        return ResponseEntity.ok(ApiResponse.ok(res));
    }


    // ==========================================================
    // 🧾 6. LỊCH SỬ ĐƠN HÀNG CỦA MEMBER
    // ==========================================================
    @Operation(
            summary = "Xem lịch sử đơn hàng của thành viên",
            description = """
                Dành cho **MEMBER**, **STUDENT**, **CLUB_LEADER**, hoặc **VICE_LEADER**.<br>
                Trả về danh sách toàn bộ đơn hàng mà thành viên đã tạo (bao gồm CLB và Event).
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Lấy danh sách đơn hàng thành công")
    )
    @GetMapping("/orders/member")
    @PreAuthorize("hasAnyRole('MEMBER','STUDENT','CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByMember(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                redeemService.getOrdersByMember(principal.getUser().getUserId())
        ));
    }

    // ==========================================================
    // 📦 7. DANH SÁCH ĐƠN HÀNG THEO CLB
    // ==========================================================
    @Operation(
            summary = "Xem danh sách đơn hàng của CLB",
            description = """
                Dành cho **CLUB_LEADER** hoặc **VICE_LEADER**.<br>
                Hiển thị tất cả đơn hàng thuộc kho của CLB.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Lấy danh sách thành công")
    )
    @GetMapping("/orders/club/{clubId}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listClubOrders(
            @PathVariable Long clubId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                redeemService.getOrdersByClub(clubId)
        ));
    }

    // ==========================================================
    // 🎉 8. DANH SÁCH ĐƠN HÀNG THEO SỰ KIỆN
    // ==========================================================
    @Operation(
            summary = "Xem danh sách đơn hàng theo sự kiện",
            description = """
                Dành cho **UNIVERSITY_STAFF** hoặc **CLUB_LEADER**.<br>
                Hiển thị các đơn đổi quà được tạo trong phạm vi sự kiện (booth).
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Lấy danh sách thành công")
    )
    @GetMapping("/orders/event/{eventId}")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listEventOrders(
            @PathVariable Long eventId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                redeemService.getOrdersByEvent(eventId)
        ));
    }


    @Operation(
            summary = "Xem chi tiết đơn hàng bằng OrderCode (QR Scan)",
            description = """
            API dùng để hiển thị chi tiết đơn hàng khi quét QR.<br>
            Không yêu cầu đăng nhập.<br><br>

            - Staff/Leader có thể quét QR từ email hoặc tại booth để xem thông tin đơn hàng.<br>
            - OrderCode là mã dạng <b>UC-xxxxxx</b> hoặc <b>EV-xxxxxx</b>.<br>
            - Trả về toàn bộ chi tiết đơn hàng: sản phẩm, điểm, số lượng, club/event, trạng thái, thời gian tạo,...
            """,
            responses = @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "200", description = "Lấy chi tiết đơn hàng thành công")
    )
    @GetMapping("/orders/{orderCode}")
    @PermitAll
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByCode(
            @PathVariable String orderCode
    ) {
        OrderResponse res = redeemService.getOrderByCode(orderCode);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }
    @Operation(
            summary = "Xem chi tiết đơn hàng bằng OrderId",
            description = """
            API trả về chi tiết đơn hàng dựa trên <b>orderId</b>.<br>
            Dùng cho nội bộ hệ thống (Leader/Staff), hoặc khi cần debug từ Admin.<br>
            Không dành cho QR scan (QR dùng orderCode).<br><br>

            Trả về thông tin:
            - Sản phẩm
            - Số lượng
            - Điểm đã trừ
            - Club / Event
            - Trạng thái đơn
            - Thời gian tạo / hoàn tất
            """,
            responses = @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "200", description = "Lấy chi tiết đơn hàng theo ID thành công")
    )
    @GetMapping("/order/id/{orderId}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','STAFF','UNIVERSITY_STAFF','ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long orderId
    ) {
        OrderResponse res = redeemService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }


}
