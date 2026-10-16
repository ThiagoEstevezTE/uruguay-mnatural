package uy.edu.um.proyectotic2026.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

// Genera la imagen del codigo QR con el numero de reserva (Acta 1, punto 4.10).
// El prestador lo escanea el dia de la actividad para registrar la asistencia.
@Service
public class QrCodeService {

    public byte[] generatePng(String text, int size) throws WriterException, IOException {
        BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", png);
        return png.toByteArray();
    }
}
