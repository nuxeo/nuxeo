/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel <ogrisel@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.security.SecurityException;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

import com.google.common.collect.MapMaker;

/**
 * Manage list of NuxeoDrive synchronization roots and devices for a given nuxeo
 * user.
 */
public class NuxeoDriveManagerImpl extends DefaultComponent implements
        NuxeoDriveManager {

    public static final String NUXEO_DRIVE_FACET = "DriveSynchronized";

    public static final String DRIVE_SUBSCRIBERS_PROPERTY = "drv:subscribers";

    // TODO: upgrade to latest version of google collections to be able to limit
    // the size with a LRU policy
    ConcurrentMap<String, Set<IdRef>> cache = new MapMaker().concurrencyLevel(4).softKeys().softValues().expiration(
            10, TimeUnit.MINUTES).makeMap();

    @Override
    public void synchronizeRoot(String userName, DocumentModel newRootContainer)
            throws PropertyException, ClientException, SecurityException {
        if (!newRootContainer.hasFacet(NUXEO_DRIVE_FACET)) {
            newRootContainer.addFacet(NUXEO_DRIVE_FACET);
        }
        if (!newRootContainer.isFolder() || newRootContainer.isProxy()
                || newRootContainer.isVersion()) {
            throw new ClientException(String.format(
                    "Document '%s' (%s) is not a suitable synchronization root"
                            + " as it is either not folderish or is a readonly"
                            + " proxy or archived version.",
                    newRootContainer.getTitle(), newRootContainer.getRef()));
        }
        CoreSession session = newRootContainer.getCoreSession();
        UserManager userManager = Framework.getLocalService(UserManager.class);
        if (!session.hasPermission(userManager.getPrincipal(userName),
                newRootContainer.getRef(), SecurityConstants.ADD_CHILDREN)) {
            throw new SecurityException(String.format(
                    "%s has no permission to create content in '%s' (%s).",
                    userName, newRootContainer.getTitle(),
                    newRootContainer.getRef()));
        }
        String[] subscribers = (String[]) newRootContainer.getPropertyValue(DRIVE_SUBSCRIBERS_PROPERTY);
        if (subscribers == null) {
            subscribers = new String[] { userName };
        } else {
            if (Arrays.binarySearch(subscribers, userName) < 0) {
                String[] old = subscribers;
                subscribers = new String[old.length + 1];
                for (int i = 0; i < old.length; i++) {
                    subscribers[i] = old[i];
                }
                subscribers[old.length] = userName;
                Arrays.sort(subscribers);
            }
        }
        newRootContainer.setPropertyValue(DRIVE_SUBSCRIBERS_PROPERTY,
                (Serializable) subscribers);
        session.saveDocument(newRootContainer);
        session.save();
        cache.clear();
    }

    @Override
    public void unsynchronizeRoot(String userName, DocumentModel rootContainer)
            throws PropertyException, ClientException {
        if (!rootContainer.hasFacet(NUXEO_DRIVE_FACET)) {
            rootContainer.addFacet(NUXEO_DRIVE_FACET);
        }
        String[] subscribers = (String[]) rootContainer.getPropertyValue(DRIVE_SUBSCRIBERS_PROPERTY);
        if (subscribers == null) {
            subscribers = new String[0];
        } else {
            if (Arrays.binarySearch(subscribers, userName) >= 0) {
                String[] old = subscribers;
                subscribers = new String[old.length - 1];
                for (int i = 0; i < old.length; i++) {
                    if (!userName.equals(old[i])) {
                        subscribers[i] = old[i];
                    }
                }
            }
        }
        rootContainer.setPropertyValue(DRIVE_SUBSCRIBERS_PROPERTY,
                (Serializable) subscribers);
        CoreSession session = rootContainer.getCoreSession();
        session.saveDocument(rootContainer);
        session.save();
        cache.clear();
    }

    @Override
    public void handleFolderDeletion(IdRef deleted) throws ClientException {
        cache.clear();
    }

    @Override
    public Set<IdRef> getSynchronizationRootReferences(String userName,
            CoreSession session) throws ClientException {
        // cache uses soft keys hence physical equality: intern key before
        // lookup
        userName = userName.intern();
        Set<IdRef> references = cache.get(userName);
        if (references == null) {
            references = new LinkedHashSet<IdRef>();
            String q = String.format(
                    "SELECT ecm:uuid FROM Document WHERE %s = %s"
                            + " AND ecm:currentLifeCycleState <> 'deleted'"
                            + " ORDER BY dc:title, dc:created DESC",
                    DRIVE_SUBSCRIBERS_PROPERTY,
                    NXQLQueryBuilder.prepareStringLiteral(userName, true, true));
            IterableQueryResult results = session.queryAndFetch(q, NXQL.NXQL);
            for (Map<String, Serializable> result : results) {
                references.add(new IdRef(result.get("ecm:uuid").toString()));
            }
            results.close();
            cache.put(userName, references);
        }
        return references;
    }

}
