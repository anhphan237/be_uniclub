package com.example.uniclub.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QrService {

    private final Cloudinary cloudinary;

    /**
     * 🧩 Sinh mã QR và trả về Base64 (chỉ dùng cho test hoặc hiển thị local)
     */
    public String generateQrAsBase64(String data) {
        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(data, BarcodeFormat.QR_CODE, 250, 250);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (WriterException | IOException e) {
            throw new RuntimeException("QR generation failed", e);
        }
    }

    /**
     * ☁️ Sinh mã QR, upload lên Cloudinary và trả về link HTTPS
     * (dùng cho email, Gmail sẽ hiển thị được ảnh này)
     */
    public String generateQrAndUpload(String data) {
        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(data, BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            byte[] qrBytes = baos.toByteArray();

            String publicId = "qr_" + System.currentTimeMillis(); // 🔥 Không chứa ký tự đặc biệt

            Map uploadResult = cloudinary.uploader().upload(qrBytes, ObjectUtils.asMap(
                    "folder", "uniclub_qr",
                    "public_id", publicId,
                    "overwrite", true
            ));

            return uploadResult.get("secure_url").toString();
        } catch (Exception e) {
            throw new RuntimeException("QR generation/upload failed", e);
        }
    }

}
