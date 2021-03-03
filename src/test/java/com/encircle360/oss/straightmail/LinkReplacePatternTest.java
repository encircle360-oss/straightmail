package com.encircle360.oss.straightmail;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.encircle360.oss.straightmail.util.HtmlUtil;

public class LinkReplacePatternTest {

    @Test
    public void testPattern() {
        String testText = "<a target=\"_blank\" href=\"lalala.html\">asdf</a> Test text <a href=\"https://www.google.com\">test google link</a> asdasd ";
        String expectation = "asdf(lalala.html) Test text test google link(https://www.google.com) asdasd ";

        Assertions.assertEquals(expectation, HtmlUtil.replaceHtmlLinkToPlainText(testText));
    }
}
