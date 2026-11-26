package com.encircle360.oss.straightmail.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for HTML processing operations.
 *
 * <p>Not instantiable.
 */
public class HtmlUtil {

    // Private constructor to hide implicit public one.
    private HtmlUtil() {
    }

    private static final Pattern linkFinderPattern = Pattern.compile("<a[\\s]+([^>]+)>((?:.(?!\\<\\/a\\>))*.)<\\/a>", Pattern.CASE_INSENSITIVE);
    private static final Pattern hrefFinderPattern = Pattern.compile("href=\"(.*)\"", Pattern.CASE_INSENSITIVE);

    /**
     * Converts HTML anchor ({@code <a>}) elements to plain-text link representations.
     *
     * <p>Each {@code <a href="url">text</a>} element is replaced with {@code text(url)},
     * suitable for inclusion in a plain-text email body.
     *
     * @param htmlLike the HTML-like string containing anchor elements
     * @return the string with anchor elements replaced by plain-text equivalents
     */
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

            htmlLike = htmlLike.replace(completeLink, text + "(" + href + ")");
        }
        return htmlLike;
    }

}
