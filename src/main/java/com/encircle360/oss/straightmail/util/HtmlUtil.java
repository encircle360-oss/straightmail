package com.encircle360.oss.straightmail.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUtil {

    final private static Pattern linkFinderPattern = Pattern.compile("<a[\\s]+([^>]+)>((?:.(?!\\<\\/a\\>))*.)<\\/a>", Pattern.CASE_INSENSITIVE);
    final private static Pattern hrefFinderPattern = Pattern.compile("href=\"(?:(.*))\"", Pattern.CASE_INSENSITIVE);

    public static String replaceHtmlLinkToPlainText(String htmlLike) {
        Matcher matcher = linkFinderPattern.matcher(htmlLike);
        while (matcher.find()) {
            String completeLink = matcher.group();
            String href = matcher.group(1);
            String text = matcher.group(2);
            Matcher hrefMatcher = hrefFinderPattern.matcher(href);
            if (hrefMatcher.find()) {
                href = hrefMatcher.group(1);
            }

            htmlLike = htmlLike.replace(completeLink, text + " ( " + href + " )");
        }
        return htmlLike;
    }

}
