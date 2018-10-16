/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.restAPI.service.PluggableRestletService;
import org.nuxeo.ecm.platform.ui.web.restAPI.service.RestletPluginDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.restlet.Restlet;
import org.restlet.ext.servlet.ServletAdapter;
import org.restlet.routing.Filter;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

/**
 * Servlet used to run a Restlet inside Nuxeo.
 * <p>
 * Setup Seam Restlet filter if needed.
 * <p>
 * Ensures a transaction is started/committed.
 */
public class RestletServlet extends HttpServlet {

    private static final Log log = LogFactory.getLog(RestletServlet.class);

    private static final long serialVersionUID = 1764653653643L;

    protected ServletAdapter adapter;

    @Override
    public synchronized void init() throws ServletException {
        super.init();

        adapter = new ServletAdapter(getServletContext());

        // init the router
        Router restletRouter = new Router();

        // get the service
        PluggableRestletService service = (PluggableRestletService) Framework.getRuntime().getComponent(
                PluggableRestletService.NAME);
        if (service == null) {
            log.error("Unable to get Service " + PluggableRestletService.NAME);
            throw new ServletException("Can't initialize Nuxeo Pluggable Restlet Service");
        }

        for (String restletName : service.getContributedRestletNames()) {
            RestletPluginDescriptor plugin = service.getContributedRestletDescriptor(restletName);

            Restlet restletToAdd;
            if (plugin.getUseSeam()) {
                Filter seamFilter = new SeamRestletFilter(plugin.getUseConversation());

                Restlet seamRestlet = service.getContributedRestletByName(restletName);

                seamFilter.setNext(seamRestlet);

                restletToAdd = seamFilter;
            } else {

                if (plugin.isSingleton()) {
                    restletToAdd = service.getContributedRestletByName(restletName);
                } else {
                    Filter threadSafeRestletFilter = new ThreadSafeRestletFilter();

                    Restlet restlet = service.getContributedRestletByName(restletName);

                    threadSafeRestletFilter.setNext(restlet);
                    restletToAdd = threadSafeRestletFilter;
                }
            }

            for (String urlPattern : plugin.getUrlPatterns()) {
                restletRouter.attach(urlPattern, restletToAdd, Template.MODE_STARTS_WITH);
            }
        }

        adapter.setNext(restletRouter);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        boolean tx = false;
        if (!TransactionHelper.isTransactionActive()) {
            tx = TransactionHelper.startTransaction();
        }
        boolean ok = false;
        try {
            adapter.service(req, res);
            ok = true;
        } finally {
            if (!ok) {
                TransactionHelper.setTransactionRollbackOnly();
            }
            if (tx) {
                if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
                    // SeamRestletFilter might have done an early commit to
                    // avoid race condition on the core session on restlets
                    // who rely upon the conversation lock to fetch it
                    // thread-safely
                    TransactionHelper.commitOrRollbackTransaction();
                }
            }
        }
    }

}
