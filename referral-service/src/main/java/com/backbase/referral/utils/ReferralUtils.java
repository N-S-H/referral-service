package com.backbase.referral.utils;

import com.backbase.referral.config.AppConfiguration;
import com.backbase.referral.exceptions.ReferralCodeGenerationException;
import com.backbase.referral.repository.UserReferralRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.mifmif.common.regex.Generex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static com.backbase.referral.utils.Constants.*;
import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReferralUtils {

    private final UserReferralRepository userReferralRepository;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateReferralCode(AppConfiguration.ReferralPatternEnum referralCodePatternType, String referralCodeRegexPattern,
                                       String referralCodePrefix, int referralCodeLengthWithoutPrefix, int maxAttempts) {
        int currentAttempts = 0;
        final String prefix = referralCodePrefix == null ? "" : referralCodePrefix;
        String referralCode = null;
        do {
            currentAttempts++;
            referralCode = generateReferralCodeUtil(referralCodePatternType, referralCodeRegexPattern, prefix, referralCodeLengthWithoutPrefix);
            if(StringUtils.isNotEmpty(referralCode) && !userReferralRepository.existsByReferralCode(referralCode)) {
                return referralCode;
            }
        } while (currentAttempts < maxAttempts);
        log.error("REFERRAL: Failed to generate unique referral code after {} attempts", maxAttempts);
        throw new ReferralCodeGenerationException()
                .withKey(ErrorCodes.MAX_REFERRAL_CODE_GENERATION_ATTEMPTS_EXCEEDED)
                .withMessage(ErrorCodes.MAX_REFERRAL_CODE_GENERATION_ATTEMPTS_EXCEEDED_MESSAGE);
    }

    private String generateReferralCodeUtil(AppConfiguration.ReferralPatternEnum referralCodePatternType, String referralCodeRegexPattern,
                                            String prefix, int referralCodeLengthWithoutPrefix) {
        switch (referralCodePatternType) {
            case ALPHANUMERIC:
                return prefix + randomFromCharset(ALPHANUMERIC, referralCodeLengthWithoutPrefix);

            case ALPHABETIC:
                return prefix + randomFromCharset(UPPER_ALPHA, referralCodeLengthWithoutPrefix);

            case NUMERIC:
                return prefix + randomFromCharset(DIGITS, referralCodeLengthWithoutPrefix);

            case REGEX:
                Generex generex = new Generex(referralCodeRegexPattern);
                String candidate = generex.random(referralCodeLengthWithoutPrefix, referralCodeLengthWithoutPrefix);
                return Optional.ofNullable(candidate)
                        .filter(StringUtils::isNotEmpty)
                        .map(code -> prefix + code)
                        .orElse(null);

        }
        return null;
    }

    private static String randomFromCharset(String charset, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = RANDOM.nextInt(charset.length());
            sb.append(charset.charAt(idx));
        }
        return sb.toString();
    }

    public String generateReferralQRString(String referralCode) {
        try {
            BufferedImage qrCodeImage = getQRCode(referralCode.getBytes(UTF_8));
            return bufferedImageToByteArray(qrCodeImage);
        } catch (Exception e) {
            log.error("REFERRAL: Error occurred while generating QR code for referral code: {}", referralCode, e);
            throw new ReferralCodeGenerationException()
                    .withKey(ErrorCodes.REFERRAL_QR_CODE_GENERATION_FAILED)
                    .withMessage(ErrorCodes.REFERRAL_QR_CODE_GENERATION_FAILED_MESSAGE);
        }
    }


    public String bufferedImageToByteArray(BufferedImage image) throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "png", os);
        return Base64.getEncoder()
                .encodeToString(os.toByteArray());
    }

    public BufferedImage getQRCode(byte[] bytes) throws WriterException {
        BitMatrix matrix = new MultiFormatWriter()
                .encode(new String(bytes, UTF_8),
                        BarcodeFormat.QR_CODE, Constants.QR_CODE_WIDTH, Constants.QR_CODE_HEIGHT, CONFIG_MAP
                );

        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private static final Map<EncodeHintType, Object> CONFIG_MAP = Map.of(
            EncodeHintType.ERROR_CORRECTION,
            ErrorCorrectionLevel.L,
            EncodeHintType.CHARACTER_SET,
            Constants.QR_CODE_CHARSET,
            EncodeHintType.MARGIN, 0
    );
}
