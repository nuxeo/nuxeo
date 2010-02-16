/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.apidoc.snapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.apidoc.repository.RepositoryDistributionSnapshot;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 *  
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public class SnapshotManager {

    protected static DistributionSnapshot runtimeSnapshot = null;

    protected static Map<String, DistributionSnapshot> persistentSnapshots = new HashMap<String, DistributionSnapshot>();

    public static final String RUNTIME="current";
    
    public static DistributionSnapshot getRuntimeSnapshot() {
        if (runtimeSnapshot==null) {
            runtimeSnapshot = new RuntimeSnapshot();
        }
        return runtimeSnapshot;
    }

    public static void addPersistentSnapshot(String key, DistributionSnapshot snapshot) {    	
    	persistentSnapshots.put(key, snapshot);
    }
    
    public static DistributionSnapshot getSnapshot(String key, CoreSession session) {
    	if (key==null || RUNTIME.equals(key)) {
    		return getRuntimeSnapshot();
    	}
    	readPersistentSnapshots(session);
    	return persistentSnapshots.get(key);
    }
    
    public static List<DistributionSnapshot> readPersistentSnapshots(CoreSession session) {
    	List<DistributionSnapshot> snaps = RepositoryDistributionSnapshot.readPersistentSnapshots(session);
    
    	for (DistributionSnapshot snap : snaps) {
    		addPersistentSnapshot(snap.getKey(), snap);
    	}    	
    	return snaps;    	
    }
    
    public static Map<String, DistributionSnapshot> getPersistentSnapshots(CoreSession session) {
        if (persistentSnapshots==null || persistentSnapshots.size()==0) {
        	if (session!=null) {
        		readPersistentSnapshots(session);
        	} else {
        		persistentSnapshots = new HashMap<String, DistributionSnapshot>();
        	}
        }
        return persistentSnapshots;
    }

    public static List<String> getPersistentSnapshotNames(CoreSession session) {
        List<String> names = new ArrayList<String>();
        names.addAll(getPersistentSnapshots(session).keySet());
        return names;
    }

    public static List<String> getAvailableDistributions(CoreSession session) {
        List<String> names = new ArrayList<String>();
        names.addAll(getPersistentSnapshots(session).keySet());        
        names.add(0,RUNTIME);
        return names;
    }

}
