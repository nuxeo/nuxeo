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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
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
        if (currentNuxeoPrincipal == null) {
            log.warn("Could not get the current user from the context.");
            return null;
        }
        final WidgetEntity widget = new WidgetEntity(widgetTypeName);
        widget.setScope(currentNuxeoPrincipal.getName());
        begin();
        em.persist(widget);
        commit();
        return widget;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized List<Widget> getWidgets(String regionName)
            throws ProviderException {
        if (regionName == null) {
            throw new ProviderException("Region name is undefined");
        }
        List<Widget> widgets = new ArrayList<Widget>();
        if (currentNuxeoPrincipal != null) {
            widgets.addAll(em.createNamedQuery("Widget.findByScope").setParameter(
                    "region", regionName).setParameter("scope",
                    currentNuxeoPrincipal.getName()).getResultList());
        }
        return widgets;
    }

    /*
     * Security
     */
    @Override
    public boolean canRead() {
        if (currentNuxeoPrincipal == null) {
            log.warn("Could not get the current user from the context.");
            return false;
        }
        return !((NuxeoPrincipal) currentNuxeoPrincipal).isAnonymous();
    }

    @Override
    public boolean canWrite() {
        if (currentNuxeoPrincipal == null) {
            log.warn("Could not get the current user from the context.");
            return false;
        }
        return !((NuxeoPrincipal) currentNuxeoPrincipal).isAnonymous();
    }

}
