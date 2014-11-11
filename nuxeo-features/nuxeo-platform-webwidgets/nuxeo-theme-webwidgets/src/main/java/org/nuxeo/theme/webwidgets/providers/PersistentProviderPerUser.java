/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.webwidgets.providers;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.persistence.PersistenceProvider.RunCallback;
import org.nuxeo.ecm.core.persistence.PersistenceProvider.RunVoid;
import org.nuxeo.theme.webwidgets.ProviderException;
import org.nuxeo.theme.webwidgets.Widget;

public class PersistentProviderPerUser extends PersistentProvider {

    private static final Log log = LogFactory.getLog(PersistentProviderPerUser.class);

    @Override
    public synchronized Widget createWidget(String widgetTypeName)
            throws ProviderException {
        if (widgetTypeName == null) {
            throw new ProviderException("Widget type name is undefined");
        }
        Principal currentNuxeoPrincipal = getCurrentPrincipal();
        if (currentNuxeoPrincipal == null) {
            log.warn("Could not get the current user from the context.");
            return null;
        }

        final WidgetEntity widget = new WidgetEntity(widgetTypeName);
        widget.setScope(currentNuxeoPrincipal.getName());
        try {
            getPersistenceProvider().run(true, new RunVoid() {
                public void runWith(EntityManager em) {
                    em.persist(widget);
                }
            });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        return widget;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized List<Widget> getWidgets(final String regionName)
            throws ProviderException {
        if (regionName == null) {
            throw new ProviderException("Region name is undefined");
        }

        try {
            return getPersistenceProvider().run(false,
                    new RunCallback<List<Widget>>() {
                        public List<Widget> runWith(EntityManager em) {
                            List<Widget> widgets = new ArrayList<Widget>();
                            Principal currentNuxeoPrincipal = getCurrentPrincipal();
                            if (currentNuxeoPrincipal != null) {
                                Query query = em.createNamedQuery("Widget.findByScope");
                                query.setParameter("region", regionName);
                                query.setParameter("scope",
                                        currentNuxeoPrincipal.getName());
                                widgets.addAll(query.getResultList());
                            }
                            return widgets;
                        }
                    });
        } catch (ClientException e) {
            throw new ProviderException(e);
        }

    }

    /*
     * Security
     */
    @Override
    public boolean canRead() {
        Principal currentNuxeoPrincipal = getCurrentPrincipal();
        if (currentNuxeoPrincipal == null) {
            log.warn("Could not get the current user from the context.");
            return false;
        }
        return !((NuxeoPrincipal) currentNuxeoPrincipal).isAnonymous();
    }

    @Override
    public boolean canWrite() {
        Principal currentNuxeoPrincipal = getCurrentPrincipal();
        if (currentNuxeoPrincipal == null) {
            log.warn("Could not get the current user from the context.");
            return false;
        }
        return !((NuxeoPrincipal) currentNuxeoPrincipal).isAnonymous();
    }

}
