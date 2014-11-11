/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * TODO: document me.
 *
 * Used in get methods to get request params to the filter. Encapuslates the
 * request into a wrapper to do so.
 *
 * @author tiry
 */
public class FancyURLRequestWrapper extends HttpServletRequestWrapper {

    private DocumentView docView;

    public FancyURLRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    public FancyURLRequestWrapper(HttpServletRequest request,
            DocumentView docView) {
        super(request);
        this.docView = docView;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> getParameterMap() {
        Map<String, String> result = super.getParameterMap();
        if (result == null) {
            result = new HashMap<String, String>();
        } else {
            // copy parameter map from parent class to avoid modifying the
            // original
            result = new HashMap<String, String>(result);
        }
        if (docView != null) {
            result.putAll(docView.getParameters());
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
