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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public interface SnapshotManager {

    DistributionSnapshot getRuntimeSnapshot();

    void addPersistentSnapshot(String key, DistributionSnapshot snapshot);

    DistributionSnapshot getSnapshot(String key, CoreSession session);

    List<DistributionSnapshot> readPersistentSnapshots(CoreSession session);

    Map<String, DistributionSnapshot> getPersistentSnapshots(CoreSession session);

    List<String> getPersistentSnapshotNames(CoreSession session);

    List<DistributionSnapshotDesc> getAvailableDistributions(CoreSession session);

    List<String> getAvailableVersions(CoreSession session, NuxeoArtifact nxItem);

    void exportSnapshot(CoreSession session, String key, OutputStream out) throws Exception ;

    void importSnapshot(CoreSession session,InputStream is) throws Exception;

    DistributionSnapshot persistRuntimeSnapshot(CoreSession session) throws ClientException;

    DistributionSnapshot persistRuntimeSnapshot(CoreSession session, String name) throws ClientException;

}
