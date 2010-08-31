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
 *     ws@nuxeo.com
 */
package store;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author ws
 *
 */
public class StoreServiceImpl implements StoreService {

    /* (non-Javadoc)
     * @see store.StoreService#retrieveKey(java.lang.String)
     */
    @Override
    public String retrieveKey(String userId) throws Exception {
        DirectoryService dm = Framework.getService(DirectoryService.class);
        Session session = dm.open(KeyDirConfigDescriptor.getDirectoryName());
        String value = null;
        try {
            DocumentModel entry = session.getEntry(userId);
            value = (String) entry.getProperty(KeyDirConfigDescriptor.getSchemaName(),
                    KeyDirConfigDescriptor.getFieldName());
        } finally {
            session.close();
        }
        return value;
    }

    /* (non-Javadoc)
     * @see store.StoreService#storeCertificate()
     */
    @Override
    public void storeCertificate() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see store.StoreService#storeKey()
     */
    @Override
    public void storeKey() {
        // TODO Auto-generated method stub

    }

}
