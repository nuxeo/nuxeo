/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.filesystem;

import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.filesystem.FilesystemService;
import org.nuxeo.ecm.core.api.filesystem.FilesystemServiceDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Central component for {@link FilesystemService} registration and lookup.
 */
public class FilesystemServiceComponent extends DefaultComponent {

    private static final Log log = LogFactory.getLog(FilesystemServiceComponent.class);

    public static final String XP = "filesystemService";

    protected LinkedList<Class<? extends FilesystemService>> contribs;

    protected FilesystemService service;

    protected boolean recompute;

    @Override
    public void activate(ComponentContext context) throws Exception {
        contribs = new LinkedList<Class<? extends FilesystemService>>();
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
        if (!(contrib instanceof FilesystemServiceDescriptor)) {
            log.error("Invalid contribution: " + contrib.getClass().getName());
            return;
        }
        FilesystemServiceDescriptor desc = (FilesystemServiceDescriptor) contrib;
        Class<?> klass;
        try {
            klass = Class.forName(desc.className);
        } catch (ClassNotFoundException e) {
            log.error("Invalid contribution class: " + desc.className);
            return;
        }
        if (!FilesystemService.class.isAssignableFrom(klass)) {
            log.error("Invalid contribution class: " + desc.className);
            return;
        }
        contribs.add((Class<FilesystemService>) klass);
        log.info("Registered filename service: " + desc.className);
        recompute = true;
    }

    @Override
    public void unregisterContribution(Object contrib, String xp,
            ComponentInstance contributor) throws Exception {
        if (!XP.equals(xp)) {
            return;
        }
        if (!(contrib instanceof FilesystemServiceDescriptor)) {
            return;
        }
        FilesystemServiceDescriptor desc = (FilesystemServiceDescriptor) contrib;
        Class<?> klass;
        try {
            klass = Class.forName(desc.className);
        } catch (ClassNotFoundException e) {
            return;
        }
        if (!klass.isAssignableFrom(FilesystemService.class)) {
            return;
        }
        contribs.remove(klass);
        log.info("Unregistered filename service: " + desc.className);
        recompute = true;
    }

    protected synchronized void recompute() {
        if (!recompute) {
            return;
        }
        Class<? extends FilesystemService> klass;
        if (contribs.isEmpty()) {
            klass = FilesystemServiceImpl.class;
        } else {
            klass = contribs.getLast();
        }
        if (service == null || klass != service.getClass()) {
            try {
                service = klass.newInstance();
            } catch (Exception e) {
                throw new ClientRuntimeException(e);
            }
        } // else keep old service instance
        recompute = false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(FilesystemService.class)) {
            recompute();
            return (T) service;
        }
        return null;
    }

}
