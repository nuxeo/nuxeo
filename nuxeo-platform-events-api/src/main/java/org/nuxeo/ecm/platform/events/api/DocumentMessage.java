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
 *
 * $Id: IDocumentMessage.java 511 2006-06-09 00:02:58Z janguenot $
 */

package org.nuxeo.ecm.platform.events.api;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Document Message.
 * <p>
 * Document model carrying info related to an event happening on a document. It
 * aims at being sent on a messaging bus.
 * <p>
 * It adds core event specifics to NXCore generic {@link DocumentModel}.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface DocumentMessage extends EventMessage, DocumentModel {

    String getDocCurrentLifeCycle();

}
