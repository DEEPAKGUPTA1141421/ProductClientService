package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.Model.Seller;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SellerQrService {

    private final Cloudinary cloudinary;
    private final ResourceLoader resourceLoader;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public String buildSellerPageUrl(Seller seller) {
        return UriComponentsBuilder
                .fromHttpUrl(frontendBaseUrl)
                .pathSegment("shop", seller.getId().toString())
                .build()
                .toUriString();
    }

    public String generateAndUploadSellerQr(Seller seller) throws IOException, WriterException {
        String sellerPageUrl = buildSellerPageUrl(seller);
        // Increased size for professional branding: 500x600 pixels
        BufferedImage qrImage = createQrImage(sellerPageUrl, 400, 400);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "png", os);

        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                os.toByteArray(),
                ObjectUtils.asMap(
                        "folder", "seller_qr",
                        "public_id", seller.getId().toString(),
                        "overwrite", true,
                        "resource_type", "image",
                        "quality", "auto",
                        "fetch_format", "auto"));

        Object secureUrl = uploadResult.get("secure_url");
        return secureUrl != null ? secureUrl.toString() : uploadResult.get("url").toString();
    }

    private BufferedImage createQrImage(String text, int width, int height) throws WriterException, IOException {
        // Generate base QR code
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

        BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints);
        MatrixToImageConfig qrConfig = new MatrixToImageConfig(0xFF1A1A1A, 0x00FFFFFF);
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix, qrConfig);

        // Create enhanced image with clean Dashly branding and logo
        BufferedImage enhancedImage = createBrandedQrImage(qrImage);

        // Add logo if available
        BufferedImage logo = loadLogo();
        if (logo != null) {
            enhancedImage = addLogoToQr(enhancedImage, logo);
        }

        return enhancedImage;
    }

    private BufferedImage createBrandedQrImage(BufferedImage qrImage) {
        int qrSize = 400;
        int topMargin = 100;
        int bottomMargin = 100;
        int sideMargin = 40;

        int totalWidth = qrSize + (2 * sideMargin);
        int totalHeight = topMargin + qrSize + bottomMargin;

        BufferedImage brandedImage = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = brandedImage.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, totalWidth, totalHeight);

        int cardX = 20;
        int cardY = 70;
        int cardWidth = totalWidth - 40;
        int cardHeight = qrSize + 40;
        RoundRectangle2D card = new RoundRectangle2D.Float(cardX, cardY, cardWidth, cardHeight, 40, 40);
        g2d.setColor(Color.WHITE);
        g2d.fill(card);
        g2d.setStroke(new BasicStroke(2f));
        g2d.setColor(new Color(230, 230, 230));
        g2d.draw(card);

        g2d.setFont(new Font("Arial", Font.BOLD, 26));
        g2d.setColor(new Color(26, 26, 26));
        String titleText = "Scan to Shop";
        FontMetrics titleMetrics = g2d.getFontMetrics();
        int titleX = (totalWidth - titleMetrics.stringWidth(titleText)) / 2;
        g2d.drawString(titleText, titleX, 40);

        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.setColor(new Color(70, 70, 70));
        String subtitleText = "Dashly";
        FontMetrics subtitleMetrics = g2d.getFontMetrics();
        int subtitleX = (totalWidth - subtitleMetrics.stringWidth(subtitleText)) / 2;
        g2d.drawString(subtitleText, subtitleX, 68);

        int qrX = sideMargin;
        int qrY = topMargin;
        g2d.drawImage(qrImage, qrX, qrY, qrSize, qrSize, null);

        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.setStroke(new BasicStroke(6f));
        g2d.drawRoundRect(qrX - 4, qrY - 4, qrSize + 8, qrSize + 8, 24, 24);

        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.setColor(new Color(36, 36, 36));
        String bottomText = "Dashly";
        FontMetrics bottomMetrics = g2d.getFontMetrics();
        int bottomTextX = (totalWidth - bottomMetrics.stringWidth(bottomText)) / 2;
        int bottomTextY = topMargin + qrSize + 55;
        g2d.drawString(bottomText, bottomTextX, bottomTextY);

        g2d.dispose();
        return brandedImage;
    }

    private BufferedImage addLogoToQr(BufferedImage brandedImage, BufferedImage logo) {
        // Find QR code position in the branded image (center area)
        int qrStartY = 100;
        int qrSize = 400;
        int logoSize = qrSize / 5; // 80px

        int logoX = (brandedImage.getWidth() - logoSize) / 2;
        int logoY = qrStartY + (qrSize - logoSize) / 2;

        // Draw white background for logo to make it stand out
        Graphics2D g2d = brandedImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(logoX - 5, logoY - 5, logoSize + 10, logoSize + 10);

        // Draw logo
        Image scaledLogo = logo.getScaledInstance(logoSize, logoSize, Image.SCALE_SMOOTH);
        g2d.drawImage(scaledLogo, logoX, logoY, null);
        g2d.dispose();

        return brandedImage;
    }

    private BufferedImage loadLogo() throws IOException {
        Resource logo = resourceLoader.getResource("classpath:static/DahlyLogo.jpg");
        if (!logo.exists()) logo = resourceLoader.getResource("classpath:static/DashlyLogo.jpg");
        if (!logo.exists()) logo = resourceLoader.getResource("classpath:static/DashlyLogo.jpeg");
        if (!logo.exists()) logo = resourceLoader.getResource("classpath:static/logo.png");
        if (!logo.exists()) {
            return null;
        }
        return ImageIO.read(logo.getInputStream());
    }
}
