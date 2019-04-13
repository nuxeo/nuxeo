/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.jsf;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.faces.context.ExternalContext;

/**
 * Mock external context
 *
 * @since 5.7.3
 */
public class MockExternalContext extends ExternalContext {

    @Override
    public void dispatch(String path) throws IOException {
    }

    @Override
    public String encodeActionURL(String url) {
        return null;
    }

    @Override
    public String encodeNamespace(String name) {
        return null;
    }

    @Override
    public String encodeResourceURL(String url) {
        return null;
    }

    @Override
    public Map<String, Object> getApplicationMap() {
        return null;
    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public String getInitParameter(String name) {
        return null;
    }

    @Override
    public Map<?, ?> getInitParameterMap() {
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public Object getRequest() {
        return null;
    }

    @Override
    public String getRequestContextPath() {
        return null;
    }

    @Override
    public Map<String, Object> getRequestCookieMap() {
        return null;
    }

    @Override
    public Map<String, String> getRequestHeaderMap() {
        return null;
    }

    @Override
    public Map<String, String[]> getRequestHeaderValuesMap() {
        return null;
    }

    @Override
    public Locale getRequestLocale() {
        return null;
    }

    @Override
    public Iterator<Locale> getRequestLocales() {
        return null;
    }

    @Override
    public Map<String, Object> getRequestMap() {
        return new HashMap<>();
    }

    @Override
    public Map<String, String> getRequestParameterMap() {
        return null;
    }

    @Override
    public Iterator<String> getRequestParameterNames() {
        return null;
    }

    @Override
    public Map<String, String[]> getRequestParameterValuesMap() {
        return null;
    }

    @Override
    public String getRequestPathInfo() {
        return null;
    }

    @Override
    public String getRequestServletPath() {
        return null;
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        return null;
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        return null;
    }

    @Override
    public Object getResponse() {
        return null;
    }

    @Override
    public Object getSession(boolean create) {
        return null;
    }

    @Override
    public Map<String, Object> getSessionMap() {
        return null;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public void log(String message) {
    }

    @Override
    public void log(String message, Throwable exception) {
    }

    @Override
    public void redirect(String url) throws IOException {
    }

}
