/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tdelprat
 *
 */

package org.nuxeo.wizard.context;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Simple Context management
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.1
 */
public class Context {

    public static final String CONTEXT_ATTRIBUTE = "context";

    protected static ParamCollector collector;

    protected Map<String, String> errors = new HashMap<String, String>();

    protected static Map<String, String> connectMap;

    protected HttpServletRequest req;

    protected Context(HttpServletRequest req) {
        this.req = req;
    }

    public static Context instance(HttpServletRequest req) {
        Context ctx = (Context) req.getAttribute(CONTEXT_ATTRIBUTE);
        if (ctx == null) {
            ctx = new Context(req);
            req.setAttribute(CONTEXT_ATTRIBUTE, ctx);
        }
        return ctx;
    }

    public static void reset() {
        collector = null;
        connectMap = null;
    }

    public ParamCollector getCollector() {
        if (collector == null) {
            collector = new ParamCollector();
        }
        return collector;
    }

    public void trackError(String fieldId, String message) {
        errors.put(fieldId, message);
    }

    public boolean hasErrors() {
        return errors.size() > 0;
    }

    public Map<String, String> getErrorsMap() {
        return errors;
    }

    public String getFieldsInErrorAsJson() {
        StringBuffer sb = new StringBuffer("[");

        for (String key : errors.keySet()) {
            sb.append("'");
            sb.append(key);
            sb.append("',");
        }

        sb.append("END]");

        return sb.toString().replace(",END", "");
    }

    public void storeConnectMap(Map<String, String> map) {
        connectMap = map;
    }

    public boolean isConnectRegistrationDone() {
        return connectMap != null
                && "true".equals(connectMap.get("registrationOK"));
    }

    public static Map<String, String> getConnectMap() {
        return connectMap;
    }

}
