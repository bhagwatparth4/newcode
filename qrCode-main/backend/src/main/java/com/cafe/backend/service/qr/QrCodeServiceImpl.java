package com.cafe.backend.service.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class QrCodeServiceImpl implements QrCodeService {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public byte[] generateTableQr(String qrToken) {

        String qrContent =
                frontendUrl + "/menu?qr=" + qrToken;

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        try {

            BitMatrix bitMatrix = qrCodeWriter.encode(
                    qrContent,
                    BarcodeFormat.QR_CODE,
                    400,
                    400
            );

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            MatrixToImageWriter.writeToStream(
                    bitMatrix,
                    "PNG",
                    outputStream
            );

            return outputStream.toByteArray();

        } catch (WriterException | IOException e) {

            throw new RuntimeException(
                    "Failed to generate QR code",
                    e
            );
        }
    }

    @Override
    public byte[] generateCounterQr(String qrToken) {

        String qrContent =
                frontendUrl + "/menu?qr=" + qrToken;

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        try {

            BitMatrix bitMatrix = qrCodeWriter.encode(
                    qrContent,
                    BarcodeFormat.QR_CODE,
                    400,
                    400
            );

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            MatrixToImageWriter.writeToStream(
                    bitMatrix,
                    "PNG",
                    outputStream
            );

            return outputStream.toByteArray();

        } catch (WriterException | IOException e) {

            throw new RuntimeException(
                    "Failed to generate counter QR code",
                    e
            );
        }
    }
}