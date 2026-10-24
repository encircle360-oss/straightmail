package com.encircle360.oss.straightmail;

import com.encircle360.oss.straightmail.config.MailpitTestContainer;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@AutoConfigureMockMvc
public abstract class AbstractTest {

    protected static final MailpitTestContainer mailpitContainer;

    static {
        mailpitContainer = new MailpitTestContainer();
        mailpitContainer.start();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", mailpitContainer::getSmtpHost);
        registry.add("spring.mail.port", mailpitContainer::getSmtpPort);
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:file::memory:?cache=shared&mode=memory");
        registry.add("spring.datasource.driver-class-name", () -> "org.sqlite.JDBC");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "1");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.community.dialect.SQLiteDialect");
    }

    protected String getMailpitApiUrl() {
        return mailpitContainer.getApiUrl();
    }

    @Autowired
    protected ObjectMapper mapper;

    @Autowired
    protected MockMvc mock;

    @Value("${api.key:}")
    private String testApiKey;

    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder builder) {
        if (testApiKey != null && !testApiKey.isBlank()) {
            builder.header("X-API-KEY", testApiKey).header("X-Tenant-ID", "default");
        }
        return builder;
    }

    protected MvcResult get(String url, ResultMatcher resultMatcher) throws Exception {
        return mock.perform(withAuth(MockMvcRequestBuilders.get(url))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(resultMatcher)
                .andReturn();
    }

    protected MvcResult delete(String url, ResultMatcher resultMatcher) throws Exception {
        return mock.perform(withAuth(MockMvcRequestBuilders.delete(url))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(resultMatcher)
                .andReturn();
    }

    protected <T> MvcResult post(String url, T body, ResultMatcher resultMatcher) throws Exception {
        return mock.perform(withAuth(MockMvcRequestBuilders.post(url))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(resultMatcher)
                .andReturn();
    }

    protected <T> MvcResult emptyPost(String url, ResultMatcher resultMatcher) throws Exception {
        return mock.perform(withAuth(MockMvcRequestBuilders.post(url))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(resultMatcher)
                .andReturn();
    }

    protected <T> MvcResult put(String url, T body, ResultMatcher resultMatcher) throws Exception {
        return mock.perform(withAuth(MockMvcRequestBuilders.put(url))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(resultMatcher)
                .andReturn();
    }

    protected <T> T resultToObject(MvcResult result, Class<T> tClass) throws Exception {
        MockHttpServletResponse response = result.getResponse();
        assertNotNull(response);
        assertNotNull(response.getContentAsString());
        return mapper.readValue(response.getContentAsString(), tClass);
    }
}
