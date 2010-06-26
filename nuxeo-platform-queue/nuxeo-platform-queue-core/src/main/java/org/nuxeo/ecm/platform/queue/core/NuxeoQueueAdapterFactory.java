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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.queue.core;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.ecm.platform.queue.api.QueueItem;

/**
 * Adapting queue item document.
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *
 */
public class NuxeoQueueAdapterFactory implements DocumentAdapterFactory {

    @SuppressWarnings("unchecked")
    public Object getAdapter(DocumentModel doc, Class itf) {
        if (QueueItem.class.isAssignableFrom(itf)) {
            try {
                return new NuxeoQueueAdapter(doc);
            } catch (ClientException e) {
                throw new Error("Cannot adapt to queue item doc "
                        + doc.getPathAsString());
            }
        }
        return null;
    }

}
