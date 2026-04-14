package com.shinhan.esg_be.domain.report.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.shinhan.esg_be.domain.report.dto.response.ReportPdfFile;
import com.shinhan.esg_be.domain.report.entity.ReportIssue;
import com.shinhan.esg_be.domain.report.repository.ReportIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportPdfService {

    private static final String TEMPLATE_PATH = "templates/report/certificate-template.html";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final List<Path> FONT_CANDIDATES = List.of(
            Path.of("C:/Windows/Fonts/NanumGothic.ttf"),
            Path.of("C:/Windows/Fonts/malgun.ttf"),
            Path.of("/usr/share/fonts/truetype/nanum/NanumGothic.ttf"),
            Path.of("/usr/share/fonts/truetype/malgun/malgun.ttf")
    );

    private final ReportIssueRepository reportIssueRepository;

    @Value("${app.report-verification-base-url:http://localhost:5173}")
    private String reportVerificationBaseUrl;

    @Value("${app.report-watermark-image-url:https://solve-s3-storage.s3.ap-northeast-2.amazonaws.com/verified.webp}")
    private String reportWatermarkImageUrl;

    public ReportPdfFile download(Long userId, Long issueId) {
        ReportIssue reportIssue = reportIssueRepository.findByReportIssueIdAndUser_UserId(issueId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report issue not found."));

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            String html = buildHtml(reportIssue);
            renderPdf(html, outputStream);

            return new ReportPdfFile(
                    "ESG_certificate_" + reportIssue.getCertificateNumber() + ".pdf",
                    outputStream.toByteArray()
            );
        } catch (IOException | WriterException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate report pdf.", exception);
        }
    }

    private void renderPdf(String html, ByteArrayOutputStream outputStream) throws IOException {
        Path fontPath = resolveFontPath();

        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(html, null);
        builder.toStream(outputStream);
        builder.useFont(fontPath.toFile(), "ReportKoreanFont");
        builder.run();
    }

    private String buildHtml(ReportIssue reportIssue) throws IOException, WriterException {
        String template = loadTemplate();
        String verificationUrl = buildVerificationUrl(reportIssue);
        String watermarkImageDataUrl = buildWatermarkImageDataUrl();

        return replacePlaceholders(
                template,
                Map.ofEntries(
                        Map.entry("ISSUED_AT", escapeHtml(DATE_FORMATTER.format(reportIssue.getIssuedAt().toLocalDate()))),
                        Map.entry("RECIPIENT_NAME", escapeHtml(reportIssue.getRecipientName())),
                        Map.entry("GRADE_LABEL", escapeHtml(resolveGradeLabel(reportIssue.getGrade()))),
                        Map.entry("TOTAL_SCORE", escapeHtml(reportIssue.getTotalScore() + "점")),
                        Map.entry("VERIFIED_ACTIVITY_COUNT", escapeHtml(reportIssue.getVerifiedActivityCount() + "건")),
                        Map.entry("CONSECUTIVE_ACTIVITY", escapeHtml(resolveConsecutiveLabel(reportIssue))),
                        Map.entry("TARGET_PERIOD", escapeHtml(
                                DATE_FORMATTER.format(reportIssue.getTargetStartDate())
                                        + " ~ "
                                        + DATE_FORMATTER.format(reportIssue.getTargetEndDate())
                        )),
                        Map.entry("ISSUER", "SOLVE"),
                        Map.entry("CERTIFICATE_NUMBER", escapeHtml(reportIssue.getCertificateNumber())),
                        Map.entry("ENVIRONMENT_ACTIVITY", escapeHtml(reportIssue.getEnvironmentActivityCount() + "건")),
                        Map.entry("VOLUNTEER_ACTIVITY", escapeHtml(reportIssue.getVolunteerHours() + "시간")),
                        Map.entry("SOCIAL_CONTRIBUTION_ACTIVITY", escapeHtml(reportIssue.getSocialContributionCount() + "건")),
                        Map.entry("TRUST_ACTIVITY", escapeHtml(reportIssue.getTrustActivityCount() + "건")),
                        Map.entry("VERIFICATION_CODE", escapeHtml(reportIssue.getVerificationCode())),
                        Map.entry("VERIFICATION_URL", escapeHtml(verificationUrl)),
                        Map.entry("VERIFICATION_URL_HREF", escapeHtmlAttribute(verificationUrl)),
                        Map.entry("QR_IMAGE_DATA_URL", buildQrImageDataUrl(verificationUrl)),
                        Map.entry("WATERMARK_BACKGROUND_STYLE", buildWatermarkBackgroundStyle(watermarkImageDataUrl))
                )
        );
    }

    private String loadTemplate() throws IOException {
        ClassPathResource templateResource = new ClassPathResource(TEMPLATE_PATH);
        try (InputStream inputStream = templateResource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }

    private String replacePlaceholders(String template, Map<String, String> values) {
        String rendered = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    private Path resolveFontPath() throws IOException {
        for (Path candidate : FONT_CANDIDATES) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IOException("No Korean font file found for report pdf generation.");
    }

    private String buildQrImageDataUrl(String verificationUrl) throws WriterException, IOException {
        BufferedImage qrImage = createQrImage(verificationUrl);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(qrImage, "png", outputStream);
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + base64;
        }
    }

    private String buildWatermarkImageDataUrl() {
        try (InputStream inputStream = new URL(reportWatermarkImageUrl).openStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            BufferedImage watermarkImage = ImageIO.read(inputStream);
            if (watermarkImage == null) {
                return "";
            }

            BufferedImage transparentWatermark = new BufferedImage(
                    watermarkImage.getWidth(),
                    watermarkImage.getHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D graphics = transparentWatermark.createGraphics();
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.07f));
            graphics.drawImage(watermarkImage, 0, 0, null);
            graphics.dispose();

            ImageIO.write(transparentWatermark, "png", outputStream);
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (IOException exception) {
            return "";
        }
    }

    private String buildWatermarkBackgroundStyle(String watermarkImageDataUrl) {
        if (watermarkImageDataUrl.isBlank()) {
            return "";
        }

        return "background-image: url('" + escapeHtmlAttribute(watermarkImageDataUrl) + "');"
                + " background-repeat: no-repeat;"
                + " background-position: center center;"
                + " background-size: 500px auto;";
    }

    private BufferedImage createQrImage(String verificationUrl) throws WriterException {
        Map<EncodeHintType, Object> hints = Map.of(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new MultiFormatWriter().encode(
                verificationUrl,
                BarcodeFormat.QR_CODE,
                220,
                220,
                hints
        );
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private String buildVerificationUrl(ReportIssue reportIssue) {
        String normalizedBaseUrl = reportVerificationBaseUrl.endsWith("/")
                ? reportVerificationBaseUrl.substring(0, reportVerificationBaseUrl.length() - 1)
                : reportVerificationBaseUrl;
        return normalizedBaseUrl + "/report/verify/" + reportIssue.getVerificationToken();
    }

    private String resolveConsecutiveLabel(ReportIssue reportIssue) {
        if (!Boolean.TRUE.equals(reportIssue.getShowConsecutiveAchievementBadge())
                || reportIssue.getConsecutiveMaxAchievementMonths() == null
                || reportIssue.getConsecutiveMaxAchievementMonths() <= 0) {
            return "-";
        }
        return reportIssue.getConsecutiveMaxAchievementMonths() + "개월";
    }

    private String resolveGradeLabel(String grade) {
        return switch (grade) {
            case "SEED" -> "씨앗";
            case "SPROUT" -> "새싹";
            case "TREE" -> "나무";
            case "FOREST" -> "숲";
            case "EARTH" -> "지구";
            default -> grade;
        };
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeHtmlAttribute(String value) {
        return escapeHtml(value);
    }
}
