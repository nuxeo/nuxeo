package org.nuxeo.template.processors;

public class HtmlBodyExtractor {

    protected final static String BODY_DELIMITER = "</{0,1}[bB][oO][dD][yY][^>]*>";

    public static String extractHtmlBody(String htmlContent) {

        if (htmlContent != null) {
            String[] parts = htmlContent.split(BODY_DELIMITER);
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return htmlContent;
    }

}
