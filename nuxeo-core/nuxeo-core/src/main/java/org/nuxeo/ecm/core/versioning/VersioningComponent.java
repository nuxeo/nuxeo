/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.versioning;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Versioning service component and implementation.
 */
public class VersioningComponent extends DefaultComponent implements
        VersioningService {

    private static final Log log = LogFactory.getLog(VersioningComponent.class);

    public static final String XP = "versioningService";

    public VersioningService service;

    protected LinkedList<Class<? extends VersioningService>> contribs;

    protected boolean recompute;

    @Override
    public void activate(ComponentContext context) throws Exception {
        contribs = new LinkedList<Class<? extends VersioningService>>();
        recompute = true;
        service = null;
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        contribs.clear();
        service = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerContribution(Object contrib, String xp,
            ComponentInstance contributor) throws Exception {
        if (!XP.equals(xp)) {
            log.error("Unknown extension point " + xp);
            return;
        }
        if (!(contrib instanceof VersioningServiceDescriptor)) {
            log.error("Invalid contribution: " + contrib.getClass().getName());
            return;
        }
        VersioningServiceDescriptor desc = (VersioningServiceDescriptor) contrib;
        Class<?> klass;
        try {
            klass = Class.forName(desc.className);
        } catch (ClassNotFoundException e) {
            log.error("Invalid contribution class: " + desc.className);
            return;
        }
        if (!(VersioningService.class.isAssignableFrom(klass))) {
            log.error("Invalid contribution class: " + desc.className);
            return;
        }
        contribs.add((Class<VersioningService>) klass);
        log.info("Registered versioning service: " + desc.className);
        recompute = true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterContribution(Object contrib, String xp,
            ComponentInstance contributor) throws Exception {
        if (!XP.equals(xp)) {
            return;
        }
        if (!(contrib instanceof VersioningServiceDescriptor)) {
            return;
        }
        VersioningServiceDescriptor desc = (VersioningServiceDescriptor) contrib;
        Class<?> klass;
        try {
            klass = Class.forName(desc.className);
        } catch (ClassNotFoundException e) {
            return;
        }
        if (!(klass.isAssignableFrom(VersioningService.class))) {
            return;
        }
        contribs.remove((Class<VersioningService>) klass);
        log.info("Unregistered versioning service: " + desc.className);
        recompute = true;
    }

    protected void recompute() {
        Class<? extends VersioningService> klass;
        if (contribs.size() == 0) {
            klass = StandardVersioningService.class;
        } else {
            klass = contribs.getLast();
        }
        if (service == null || klass != service.getClass()) {
            try {
                service = klass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } // else keep old service instance
    }

    public VersioningService getService() {
        if (recompute) {
            recompute();
            recompute = false;
        }
        return service;
    }

    @Override
    public String getVersionLabel(DocumentModel doc) {
        return getService().getVersionLabel(doc);
    }

    @Override
    public void doPostCreate(Document doc) throws DocumentException {
        getService().doPostCreate(doc);
    }

    @Override
    public List<VersioningOption> getSaveOptions(DocumentModel docModel)
            throws ClientException {
        return getService().getSaveOptions(docModel);
    }

    @Override
    public VersioningOption doPreSave(Document doc, boolean isDirty,
            VersioningOption option, String checkinComment)
            throws DocumentException {
        return getService().doPreSave(doc, isDirty, option, checkinComment);
    }

    @Override
    public void doPostSave(Document doc, VersioningOption option,
            String checkinComment) throws DocumentException {
        getService().doPostSave(doc, option, checkinComment);
    }

    @Override
    public Document doCheckIn(Document doc, VersioningOption option,
            String checkinComment) throws DocumentException {
        return getService().doCheckIn(doc, option, checkinComment);
    }

    @Override
    public void doCheckOut(Document doc) throws DocumentException {
        getService().doCheckOut(doc);
    }

}
