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

package org.nuxeo.ecm.platform.ui.web.restAPI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.restlet.Filter;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * Restlet Filter that ensure thread safety for seam unaware restlet.
 *
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 */
public class ThreadSafeRestletFilter extends Filter {

    private static final Log log = LogFactory.getLog(ThreadSafeRestletFilter.class);

    @Override
    protected int doHandle(Request request, Response response) {
        if (getNext() != null) {
            try {
                // get a new instance of the restlet each time it is called.
                Restlet next = getNext().getClass().newInstance();
                next.handle(request, response);
            } catch (ReflectiveOperationException e) {
                log.error("Restlet handling error", e);
                response.setEntity("Error while getting a new Restlet instance: " + e.getMessage(),
                        MediaType.TEXT_PLAIN);
            }
        } else {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
        }
        return CONTINUE;
    }

}
