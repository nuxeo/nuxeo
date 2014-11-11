/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
    public Map getInitParameterMap() {
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
        return new HashMap<String, Object>();
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
