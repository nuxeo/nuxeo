package org.nuxeo.apidoc.filter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.platform.ui.web.auth.plugins.AnonymousAuthenticator;
import org.nuxeo.runtime.api.Framework;

public class CacheAndAuthFilter extends BaseApiDocFilter  {

    public static final DateFormat HTTP_EXPIRES_DATE_FORMAT = httpExpiresDateFormat();

    protected Boolean forceAnonymous = null;

    protected boolean forceAnonymous() {
        if (forceAnonymous == null) {
            forceAnonymous = Boolean.valueOf(Framework.getProperty("org.nuxeo.apidoc.forceanonymous", "false"));
        }
        return forceAnonymous;
    }

    protected void internalDoFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        boolean activateCaching = false;
        String anonymousHeader = httpRequest.getHeader("X-NUXEO-ANONYMOUS-ACCESS");
        if ("true".equals(anonymousHeader) || forceAnonymous()) {
            // activate cache
            activateCaching=true;
        } else {
            // desactivate anonymous login
            httpRequest.setAttribute(AnonymousAuthenticator.BLOCK_ANONYMOUS_LOGIN_KEY, Boolean.TRUE);
        }

        if (activateCaching) {
            addCacheHeader(httpResponse, false, "600");
        }

        chain.doFilter(httpRequest, httpResponse);

    }

    private static DateFormat httpExpiresDateFormat() {
        // formated http Expires: Thu, 01 Dec 1994 16:00:00 GMT
        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
                Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df;
    }

    public static void addCacheHeader(HttpServletResponse httpResponse, Boolean isPrivate, String cacheTime) {
        if (isPrivate){
            httpResponse.addHeader("Cache-Control", "private, max-age=" + cacheTime);
        } else {
            httpResponse.addHeader("Cache-Control", "public, max-age=" + cacheTime);
        }

        // Generating expires using current date and adding cache time.
        // we are using the format Expires: Thu, 01 Dec 1994 16:00:00 GMT
        Date date = new Date();
        long newDate = date.getTime() + new Long(cacheTime) * 1000;
        date.setTime(newDate);

        httpResponse.setHeader("Expires",
                HTTP_EXPIRES_DATE_FORMAT.format(date));
    }

}
