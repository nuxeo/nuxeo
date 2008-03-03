/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Abstract CacheListener without any implementation
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 *
 */
public abstract class AbstractCacheListener implements CacheListener {

    //private static Log log = LogFactory.getLog(AbstractCacheListener.class);

    public void documentRemove(DocumentModel docModel) {
        debug("<documentRemove> not handled: " + getDocInfo(docModel));
    }

    public void documentRemoved(String fqn) {
        debug("<documentRemoved> not handled: " + fqn);
    }

    public void documentUpdate(DocumentModel docModel, boolean pre) {
        debug("<documentUpdate> not handled: " + getDocInfo(docModel) + "; pre: " + pre);
    }

    private void debug(String msg) {
        Log log = LogFactory.getLog(getClass());
        log.info(msg);
    }

    private String getDocInfo(DocumentModel doc) {
        return (String)doc.getProperty("common", "title");
    }
}
