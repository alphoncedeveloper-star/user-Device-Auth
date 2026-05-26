package com.alevo.usermodule.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsService {

    @Value("${sms.provider:console}")
    private String smsProvider;

    @Value("${sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${sms.twilio.from-number:}")
    private String twilioFromNumber;

    @Value("${sms.africas-talking.api-key:}")
    private String atApiKey;

    @Value("${sms.africas-talking.username:}")
    private String atUsername;

    @Value("${sms.africas-talking.sender-id:UserAuth}")
    private String atSenderId;

    /**
     * Send an OTP via the configured SMS provider.
     * Swap providers by changing sms.provider in application.yml.
     */
    public void sendOtp(String phoneNumber, String otpCode) {
        String message = buildOtpMessage(otpCode);
        switch (smsProvider.toLowerCase()) {
            case "twilio"          -> sendViaTwilio(phoneNumber, message);
            case "africas-talking" -> sendViaAfricasTalking(phoneNumber, message);
            default                -> sendViaConsole(phoneNumber, message, otpCode);
        }
    }

    private String buildOtpMessage(String otpCode) {
        return String.format(
                "Your verification code is: *%s*\n\nDo not share this code with anyone. It expires in 10 minutes.",
                otpCode
        );
    }

    /**
     * Development mode: log OTP to console instead of sending SMS.
     */
    private void sendViaConsole(String phoneNumber, String message, String otp) {
        log.info("╔══════════════════════════════════════╗");
        log.info("║         OTP (DEVELOPMENT MODE)       ║");
        log.info("╠══════════════════════════════════════╣");
        log.info("║  Phone : {}                   ║", phoneNumber);
        log.info("║  OTP   : {}                          ║", otp);
        log.info("╚══════════════════════════════════════╝");
    }

    /**
     * Send via Twilio. Add twilio-sdk dependency to pom.xml to enable.
     * <!-- com.twilio.sdk:twilio:9.x.x -->
     */
    private void sendViaTwilio(String phoneNumber, String message) {
        // TODO: uncomment after adding Twilio dependency
        // Twilio.init(twilioAccountSid, twilioAuthToken);
        // Message.creator(new PhoneNumber(phoneNumber),
        //                  new PhoneNumber(twilioFromNumber), message).create();
        log.info("Twilio SMS to {}: [configured but SDK not imported]", phoneNumber);
        throw new UnsupportedOperationException(
                "Add com.twilio.sdk:twilio dependency and uncomment Twilio code in SmsService");
    }

    /**
     * Send via Africa's Talking. Add SDK dependency to enable.
     * <!-- com.africastalking:core:3.x.x -->
     */
    private void sendViaAfricasTalking(String phoneNumber, String message) {
        // TODO: uncomment after adding Africa's Talking dependency
        // AfricasTalking.initialize(atUsername, atApiKey);
        // SMSService sms = AfricasTalking.getService(AfricasTalking.SERVICE_SMS);
        // List<Recipient> response = sms.send(message, atSenderId, new String[]{phoneNumber}, true);
        log.info("Africa's Talking SMS to {}: [configured but SDK not imported]", phoneNumber);
        throw new UnsupportedOperationException(
                "Add com.africastalking:core dependency and uncomment AT code in SmsService");
    }
}
