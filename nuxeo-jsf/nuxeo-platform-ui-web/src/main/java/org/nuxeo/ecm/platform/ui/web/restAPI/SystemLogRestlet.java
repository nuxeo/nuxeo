/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.Method;

/**
 * Small restlet logging in the commons logging log.
 * <p>
 * Very useful when running functional tests for instance, to separate cleanly what happens in the logs.
 */
public class SystemLogRestlet extends BaseStatelessNuxeoRestlet {

    private static final Log log = LogFactory.getLog(SystemLogRestlet.class);

    public static final String LEVEL = "level";

    public static final String MESSAGE = "message";

    public static final String TOKEN = "token";

    public static final String TOKEN_PROP = "org.nuxeo.systemlog.token";

    @Override
    protected void doHandleStatelessRequest(Request req, Response res) {
        if (Method.HEAD.equals(req.getMethod())) {
            // selenium does a HEAD first, then a GET
            return;
        }
        Form form = req.getResourceRef().getQueryAsForm();
        String level = form.getFirstValue(LEVEL);
        String message = form.getFirstValue(MESSAGE);
        String token = form.getFirstValue(TOKEN);
        String tokenProp = Framework.getProperty(TOKEN_PROP);
        if (tokenProp == null || !tokenProp.equals(token)) {
            log.debug(String.format("Provided token '%s' does not match %s", token, TOKEN_PROP));
        } else if ("error".equalsIgnoreCase(level)) {
            log.error(message);
        } else if ("warn".equalsIgnoreCase(level)) {
            log.warn(message);
        } else if ("info".equalsIgnoreCase(level)) {
            log.info(message);
        } else if ("debug".equalsIgnoreCase(level)) {
            log.debug(message);
        } else {
            log.trace(message);
        }
    }

}
