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

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Actually a stripped down copy of TreeCacheListener - suitable only for our
 * needs.
 *
 * @author DM
 */
public interface CacheListener {

    /**
     * Invoked before the document model internal data is altered and after it was
     * changed for an existing Document Model.
     * Given Document Model exists in the cache before and after the notification.
     *
     * @param docModel
     * @param pre
     */
    void documentUpdate(DocumentModel docModel, boolean pre);

    /**
     * The method is invoked just before the document is removed
     *
     * @param docModel the document that is about to be removed. It is a non null
     * document
     */
    void documentRemove(DocumentModel docModel);

    /**
     * Notification sent after the document with specified fqn in cache has
     * been removed.
     *
     * @param fqn
     */
    void documentRemoved(String fqn);

    // future
    // documentListUpdate....
}
