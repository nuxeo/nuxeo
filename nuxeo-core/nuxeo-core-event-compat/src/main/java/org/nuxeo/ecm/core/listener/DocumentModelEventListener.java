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
 * $Id: DocumentModelEventListener.java 21640 2007-06-29 15:05:10Z atchertchian $
 */

package org.nuxeo.ecm.core.listener;

/**
 * Compatibility interface for event listeners managing document models.
 * <p>
 * See bug NXP-666: event listeners first implementations managed events holding
 * a {@link org.nuxeo.ecm.core.model.Document} source instead of a
 * {@link org.nuxeo.ecm.core.api.DocumentModel} source. Event listeners
 * implementing this interface will be seen as "new style" event listeners. We
 * will consider others are waiting for a Document.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @deprecated: compatibility code has been removed
 */
@Deprecated
public interface DocumentModelEventListener {

}
