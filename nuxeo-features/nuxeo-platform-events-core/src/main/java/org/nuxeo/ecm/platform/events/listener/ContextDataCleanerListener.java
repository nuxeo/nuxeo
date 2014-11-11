/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: ContextDataCleanerListener.java 19078 2007-05-21 17:24:29Z sfermigier $
 */

package org.nuxeo.ecm.platform.events.listener;

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.listener.AbstractEventListener;
import org.nuxeo.ecm.core.listener.AsynchronousEventListener;
import org.nuxeo.ecm.core.listener.DocumentModelEventListener;

/**
 * Listener used to clean the context information set at request scope.
 * <p>
 * Context information is sent together with a document to be used when
 * processing an event for the document.
 * <p>
 * This listener is supposed to be called at the end of the other event
 * processings to clean this list.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class ContextDataCleanerListener extends AbstractEventListener implements
        AsynchronousEventListener, DocumentModelEventListener {

    public void notifyEvent(CoreEvent coreEvent) {
        Object source = coreEvent.getSource();
        if (source instanceof DocumentModel) {
            DocumentModel dm = (DocumentModel) source;
            ScopedMap data = dm.getContextData();
            if (data != null) {
                data.clearScope(ScopeType.REQUEST);
            }
        }
    }

}
