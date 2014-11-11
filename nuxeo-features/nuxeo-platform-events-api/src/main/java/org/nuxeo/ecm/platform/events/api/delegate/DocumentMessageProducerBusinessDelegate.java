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
 * $Id:ContentHistoryBusinessDelegate.java 3895 2006-10-11 19:12:47Z janguenot $
 */

package org.nuxeo.ecm.platform.events.api.delegate;

import java.io.Serializable;

import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducerException;
import org.nuxeo.runtime.api.Framework;

/**
 * Document message producer service delegate.
 * <p>
 * Utility class giving access to the <code>DocumentMessageProducer</code>
 * service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class DocumentMessageProducerBusinessDelegate implements
        Serializable {

    private static final long serialVersionUID = 1L;

    private DocumentMessageProducerBusinessDelegate() {
    }

    /**
     * Returns the remote document message producer service.
     *
     * @return a <code>DocumentMessageProducer</code> service instance.
     * @throws DocumentMessageProducerException if an error occured at lookup
     *             time.
     */
    public static DocumentMessageProducer getRemoteDocumentMessageProducer()
            throws DocumentMessageProducerException {
        DocumentMessageProducer producer;
        try {
            producer = Framework.getService(DocumentMessageProducer.class);
        } catch (Exception e) {
            throw new DocumentMessageProducerException(e);
        }
        return producer;
    }

    /**
     * Returns a local document message producer service.
     *
     * @return a <code>DocumentMessageProducer</code> service instance.
     */
    public static DocumentMessageProducer getLocalDocumentMessageProducer() {
        return Framework.getLocalService(DocumentMessageProducer.class);
    }

}
