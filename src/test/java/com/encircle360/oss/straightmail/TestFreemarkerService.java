package com.encircle360.oss.straightmail;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.encircle360.oss.straightmail.service.FreemarkerService;

@SpringBootTest
public class TestFreemarkerService {

    @Autowired FreemarkerService freemarkerService;

    @Test
    public void test_email_service() {
        Assertions.assertTrue(freemarkerService.templateExists("test"));
        Assertions.assertTrue(freemarkerService.templateExists("test_json_node"));
        Assertions.assertTrue(freemarkerService.templateExists("test_json_node_subject"));
        Assertions.assertTrue(freemarkerService.templateExists("test_subject"));
        Assertions.assertFalse(freemarkerService.templateExists("testasdf"));
    }
}
