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
    protected void beforeHandle(Request request, Response response) {
    }

    @Override
    protected void afterHandle(Request request, Response response) {
    }

    @Override
    protected void doHandle(Request request, Response response) {
        if (getNext() != null) {
            try {
                // get a new instance of the restlet each time it is called.
                Restlet next = getNext().getClass().newInstance();
                next.handle(request, response);
            } catch (Exception e) {
                log.error("Restlet handling error", e);
                response.setEntity(
                        "Error while getting a new Restlet instance: "
                                + e.getMessage(), MediaType.TEXT_PLAIN);
            }
        } else {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
        }
    }

}
