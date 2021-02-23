package com.encircle360.oss.straightmail;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.encircle360.oss.straightmail.service.EmailService;

@SpringBootTest
public class TestEmailService {

    @Autowired EmailService emailService;

    @Test
    public void test_email_service() {
        Assertions.assertTrue(emailService.templateExists("test"));
        Assertions.assertTrue(emailService.templateExists("test_json_node"));
        Assertions.assertTrue(emailService.templateExists("test_json_node_subject"));
        Assertions.assertTrue(emailService.templateExists("test_subject"));
        Assertions.assertFalse(emailService.templateExists("testasdf"));
    }
}
