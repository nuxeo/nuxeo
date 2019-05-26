/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.rest;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.nuxeo.ecm.platform.url.api.DocumentView;

/**
 * TODO: document me. Used in get methods to get request params to the filter. Encapuslates the request into a wrapper
 * to do so.
 *
 * @author tiry
 */
public class FancyURLRequestWrapper extends HttpServletRequestWrapper {

    private DocumentView docView;

    public FancyURLRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    public FancyURLRequestWrapper(HttpServletRequest request, DocumentView docView) {
        super(request);
        this.docView = docView;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> result = super.getParameterMap();
        if (result == null) {
            result = new HashMap<>();
        } else {
            // copy parameter map from parent class to avoid modifying the
            // original
            result = new HashMap<>(result);
        }
        if (docView != null) {
            for (Map.Entry<String, String> param : docView.getParameters().entrySet()) {
                result.put(param.getKey(), new String[] { param.getValue() });
            }
        }
        return result;
    }

    @Override
    public String[] getParameterValues(String name) {
        if (docView != null) {
            String value = docView.getParameter(name);
            if (value != null) {
                return new String[] { value };
            }
        }
        return super.getParameterValues(name);
    }

    @Override
    public String getParameter(String name) {
        if (docView != null) {
            String value = docView.getParameter(name);
            if (value != null) {
                return value;
            }
        }
        return super.getParameter(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(getParameterMap().keySet());
    }

}
