/**
 * 
 */

package org.nuxeo.drive.service.impl;

import java.io.Serializable;
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
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.runtime.model.DefaultComponent;

import com.google.common.collect.MapMaker;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Manage list of NuxeoDrive synchronization roots and devices for a given nuxeo
 * user.
 */
public class NuxeoDriveManagerImpl extends DefaultComponent implements
        NuxeoDriveManager {

    public static final String NUXEO_DRIVE_FACET = "DriveSynchronized";

    public static final String DRIVE_SUBSCRIBERS_PROPERTY = "drv:subscribers";

    // TODO: upgrade to latest version of google collections to be able to limit the size with a LRU policy
    ConcurrentMap<String, Set<IdRef>> cache = new MapMaker().concurrencyLevel(4).softKeys().softValues().expiration(
            10, TimeUnit.MINUTES).makeMap();

    @Override
    public void synchronizeRoot(String userName, DocumentModel newRootContainer)
            throws PropertyException, ClientException {
        if (!newRootContainer.hasFacet(NUXEO_DRIVE_FACET)) {
            newRootContainer.addFacet(NUXEO_DRIVE_FACET);
        }
        String[] subscribers = (String[]) newRootContainer.getPropertyValue(DRIVE_SUBSCRIBERS_PROPERTY);
        if (subscribers == null) {
            subscribers = new String[] { userName };
        } else {
            if (Arrays.binarySearch(subscribers, userName) == -1) {
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
        CoreSession session = newRootContainer.getCoreSession();
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
            if (Arrays.binarySearch(subscribers, userName) != -1) {
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
