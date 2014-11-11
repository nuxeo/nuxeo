/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tmartins
 */
package org.nuxeo.ecm.platform.usermanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7
 * 
 * @author <a href="mailto:tm@nuxeo.com">Thierry Martins</a>
 */
public class UserAdapterFactory implements DocumentAdapterFactory {

    private static final Log log = LogFactory.getLog(UserAdapterFactory.class);

    public Object getAdapter(DocumentModel doc, Class itf) {
        try {
            UserManager um = Framework.getLocalService(UserManager.class);
            return new UserAdapterImpl(doc, um);
        } catch (Exception e) {
            log.debug("failed to get user service", e);
        }
        return new UserAdapterImpl(doc, null);
    }
}
