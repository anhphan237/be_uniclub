package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.RedeemOrderRequest;
import com.example.uniclub.dto.request.RedeemQrRequest;
import com.example.uniclub.dto.request.RefundRequest;
import com.example.uniclub.dto.request.ScanQrRequest;
import com.example.uniclub.dto.response.OrderResponse;
import com.example.uniclub.dto.response.RedeemScanResponse;
import com.example.uniclub.dto.response.ReturnImageResponse;
import com.example.uniclub.entity.ProductOrder;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.RedeemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','STAFF','STUDENT')")
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




    @Operation(
            summary = "Upload ảnh lỗi sản phẩm khi hoàn hàng",
            description = """
            Dùng khi xử lý **refund**.<br>
            FE upload tối đa 5 ảnh, BE trả về danh sách URL để dùng cho refund.<br>
            Ảnh được lưu trên Cloudinary theo folder từng order.
            """
    )
    @PostMapping(value = "/order/{orderId}/refund/upload-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','STAFF')")
    public ResponseEntity<ApiResponse<List<String>>> uploadRefundImages(
            @PathVariable Long orderId,
            @RequestPart("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                redeemService.uploadRefundImages(orderId, files)
        ));
    }

    @Operation(
            summary = "Lấy danh sách ảnh lỗi của order",
            description = """
            Trả về toàn bộ ảnh lỗi đã upload cho 1 order (theo đúng thứ tự hiển thị).
            FE gọi API này để hiển thị danh sách ảnh trước khi bấm Refund.
        """
    )
    @GetMapping("/order/{orderId}/refund/images")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','STAFF')")
    public ResponseEntity<ApiResponse<List<ReturnImageResponse>>> listRefundImages(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                redeemService.listRefundImages(orderId)
        ));
    }


    @Operation(
            summary = "Xoá 1 ảnh lỗi hoàn hàng",
            description = """
            Xoá ảnh lỗi trong DB và trên Cloudinary.<br>
            Chỉ STAFF/LEADER của CLB đang sở hữu đơn mới được xoá.
            """
    )
    @DeleteMapping("/order/{orderId}/refund/image/{imageId}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','STAFF')")
    public ResponseEntity<ApiResponse<String>> deleteRefundImage(
            @PathVariable Long orderId,
            @PathVariable Long imageId
    ) {
        redeemService.deleteRefundImage(orderId, imageId);
        return ResponseEntity.ok(ApiResponse.msg("Image deleted"));
    }



    @Operation(
            summary = "Hoàn điểm toàn phần (FULL REFUND)",
            description = """
            Áp dụng khi sản phẩm lỗi hoàn toàn.<br>
            BE hoàn lại toàn bộ điểm, trả stock, lưu ảnh lỗi và log lịch sử ví.<br>
            Cần FE gửi: reason + danh sách URL ảnh lỗi sau khi upload.
            """
    )
    @PutMapping("/order/refund")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','STAFF')")
    public ResponseEntity<ApiResponse<OrderResponse>> refund(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody RefundRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                redeemService.refund(
                        req.orderId(),
                        principal.getUser().getUserId(),
                        req.reason()
                )
        ));
    }



    @Operation(
            summary = "Hoàn điểm một phần (PARTIAL REFUND)",
            description = """
            Dùng khi chỉ một phần sản phẩm bị lỗi.<br>
            BE hoàn lại điểm theo số lượng bị lỗi, cập nhật stock và log ví.<br>
            FE phải upload ảnh lỗi rồi gửi URL vào API này.
            """
    )
    @PutMapping("/order/refund-partial")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','STAFF')")
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
    @PreAuthorize("hasAnyRole('MEMBER','STUDENT','CLUB_LEADER','VICE_LEADER','STAFF')")
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
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','STAFF')")
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
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','CLUB_LEADER','STAFF')")
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



    @Operation(
            summary = "Generate QR for redeeming",
            description = """
                Member tạo mã QR chứa thông tin membership để đem lên quầy redeem.
                Mã QR có thời hạn 60 giây để tăng bảo mật.
                """
    )
    @PostMapping("/generate-qr")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> generateQr(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody RedeemQrRequest req
    ) {
        String qr = redeemService.generateMemberQr(
                principal.getUser().getUserId(),
                req.clubId()
        );
        return ResponseEntity.ok(ApiResponse.ok(qr));
    }

    @Operation(
            summary = "Scan member QR at redeem booth",
            description = """
                Leader / Vice Leader / Staff quét QR của member để kiểm tra:
                - Member có thuộc CLB hay không
                - Membership còn ACTIVE không
                - Wallet balance hiện tại
                - Các đơn hàng pending chưa lấy quà
                - Thông tin user (fullName, studentCode)
                """
    )
    @PostMapping("/scan-qr")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','STAFF')")
    public ResponseEntity<ApiResponse<RedeemScanResponse>> scanQr(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody ScanQrRequest req
    ) {
        RedeemScanResponse data = redeemService.scanMemberQr(
                req.qrToken(),
                principal.getUser().getUserId()
        );
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/event/club/{clubId}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','STAFF','ADMIN','STUDENT')")
    @Operation(
            summary = "Get all event redeem orders for a club",
            description = "Return list of EVENT_ITEM orders belonging to a specific club"
    )
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getEventRedeemOrders(
            @PathVariable Long clubId
    ) {
        List<OrderResponse> data = redeemService.getEventOrdersByClub(clubId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
    @Operation(
            summary = "Xem tất cả redeem của event mà bạn đã từng xác nhận",
            description = """
            API này trả về tất cả các đơn hàng (EVENT_ITEM) mà người dùng hiện tại 
            (dựa trên token) đã từng xác nhận trước đây.

            - Không yêu cầu user còn là STAFF
            - Không yêu cầu user còn thuộc CLB
            - Không yêu cầu user còn trong event
            - Không phụ thuộc vào membership hiện tại
            - Chỉ cần token là hợp lệ
            
            Đây là nghiệp vụ STAFF HISTORY LOG.
            """
    )
    @GetMapping("/event/{eventId}/my-approvals")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyApprovedRedeems(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long eventId,
            Pageable pageable
    ) {
        Long staffId = principal.getUser().getUserId();

        return ResponseEntity.ok(
                ApiResponse.ok(
                        redeemService.getStaffApprovedOrders(staffId, eventId, pageable)
                )
        );
    }
    @Operation(
            summary = "Xem toàn bộ đơn hàng mà bạn đã từng xác nhận",
            description = "Không cần nhập tham số. Nhấn Execute để xem toàn bộ lịch sử xử lý đơn của bạn."
    )
    @GetMapping("/my-approvals")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyAllApprovedOrders(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long staffId = principal.getUser().getUserId();

        return ResponseEntity.ok(
                ApiResponse.ok(
                        redeemService.getStaffAllApprovedOrders(staffId)
                )
        );
    }



}
