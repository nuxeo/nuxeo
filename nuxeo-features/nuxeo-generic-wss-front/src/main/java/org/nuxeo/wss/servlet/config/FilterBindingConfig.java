/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thierry Delprat
 */

package org.nuxeo.wss.servlet.config;

import java.util.regex.Pattern;

public class FilterBindingConfig {

    public static final String FP_REQUEST_TYPE = "FP";

    public static final String WS_REQUEST_TYPE = "WS";

    public static final String FAKEWS_REQUEST_TYPE = "FakeWS";

    public static final String GET_REQUEST_TYPE = "GET";

    public static final String RESOURCES_REQUEST_TYPE = "RESOURCES";

    /*
     * url binding
     */
    protected String url;

    /**
     * requestType : FP-RPC or WebService
     */
    protected String requestType;

    /**
     * name of the targetService for FP-RPC requests
     */
    protected String targetService;

    protected Pattern urlPattern;

    /**
     * target URL for WebService endpoints
     */
    protected String redirectURL;

    protected String siteName;

    public FilterBindingConfig() {
        //
    }

    public FilterBindingConfig(FilterBindingConfig binding, String siteName) {
        this.requestType = binding.requestType;
        this.redirectURL = binding.redirectURL;
        this.targetService = binding.targetService;
        this.url = binding.url;
        this.siteName = siteName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        urlPattern = Pattern.compile(url);
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getTargetService() {
        return targetService;
    }

    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }

    public String getRedirectURL() {
        return redirectURL;
    }

    public void setRedirectURL(String redirectURL) {
        this.redirectURL = redirectURL;
    }

    public Pattern getUrlPattern() {
        return urlPattern;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

}
