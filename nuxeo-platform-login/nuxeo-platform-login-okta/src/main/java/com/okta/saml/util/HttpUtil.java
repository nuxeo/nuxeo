package com.okta.saml.util;

import com.okta.saml.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

public class HttpUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    public static void forceRedirect(HttpServletRequest request, HttpServletResponse response, String redirUrl) {
        if (response == null) {
            return;
        }

        String reqUrl = request.getRequestURL().toString();
        String contentType = response.getContentType();
        Pattern pattern = Pattern.compile("([^\\s]+(\\.(?i)(jpg|jpeg|png|gif|bmp|css|js)))");
        //if content type is not html or was explicit defined as media, do nothing
        if ((contentType != null && !contentType.contains("html")) || pattern.matcher(reqUrl).matches() ||
                reqUrl.endsWith(redirUrl) || reqUrl.equals(redirUrl)) {
            return;
        }

        try {
            response.reset();
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", redirUrl);
            response.setHeader("Connection", "close");
            response.flushBuffer();
        } catch (IOException e) {
            logger.error("IO Error: " + e.getMessage());
        }
    }

    public static String getCurrentUrl(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        StringBuffer url = request.getRequestURL();
        if (request.getQueryString() != null) {
            url.append('?');
            url.append(request.getQueryString());
        }
        return url.toString();
    }

    public static String createRedirectUrlWithRelay(HttpServletRequest request, String url, String relayStateParam) {
        String curUrl = HttpUtil.getCurrentUrl(request);
        String redirectUrl = url;
        if (!curUrl.contains(relayStateParam) && !curUrl.endsWith(url)) {
            redirectUrl = String.format("%s" + (url.contains("?") ? "&" : "?") + "%s=%s",
                                        url, relayStateParam, curUrl);
        }
        return redirectUrl;
    }

    public static void forceRedirectWithRelayState(HttpServletRequest request, HttpServletResponse response,
                                                   String redirectUrl, String relayStateParam) throws IOException {

        HttpUtil.forceRedirect(request, response, createRedirectUrlWithRelay(request, redirectUrl, relayStateParam));
    }

    public static void redirect(HttpServletResponse response, String url) {
        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        response.setHeader("Location", url);
    }

    public static String getRedirectParam(HttpServletRequest request) {
        String redirectUrl = request.getParameter(Configuration.REDIR_PARAM);
        if (StringUtils.isBlank(redirectUrl)) {
            return null;
        }

        redirectUrl = redirectUrl.trim().replaceAll("[\\\\/]+$", "");
        try {
            String baseUrl = getBaseUrl(getCurrentUrl(request)).replaceAll("[\\\\/]+$", "");
            if (baseUrl.equalsIgnoreCase(redirectUrl)) {
                return null;
            }
        } catch (MalformedURLException e) {
            //no-op
        }
        return redirectUrl;
    }

    public static String getBaseUrl(String fullUrl) throws MalformedURLException {
        URL url = new URL(fullUrl);

        String baseUrl = String.format("%s://%s", url.getProtocol(), url.getHost());
        if (url.getPort() > 0) {
            baseUrl += ":" + url.getPort();
        }

        return baseUrl;
    }
}
