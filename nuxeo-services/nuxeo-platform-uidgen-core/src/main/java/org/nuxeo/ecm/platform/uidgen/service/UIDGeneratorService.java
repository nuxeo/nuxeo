/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thierry Delprat
 *
 */

package org.nuxeo.ecm.platform.uidgen.service;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.uidgen.UIDGenerator;
import org.nuxeo.ecm.platform.uidgen.UIDSequencer;

public interface UIDGeneratorService {

    /**
     * Retrieves the default {@link UIDSequencer}
     *
     * @return the default {@link UIDSequencer}
     * @since 7.3
     */
    public UIDSequencer getSequencer();

    /**
     * Retrieves {@link UIDSequencer} by it's name
     *
     * @param name the name of the {@link UIDSequencer}
     * @return the {@link UIDSequencer} matching the name
     * @since 7.3
     */
    public UIDSequencer getSequencer(String name);

    /**
     * Returns the uid generator to use for this document.
     * <p>
     * Choice is made following the document type and the generator configuration.
     */
    public abstract UIDGenerator getUIDGeneratorFor(DocumentModel doc);

    /**
     * Creates a new UID for the given doc and sets the field configured in the generator component with this value.
     */
    public abstract void setUID(DocumentModel doc) throws DocumentException;

    /**
     * @return a new UID for the given document
     */
    public abstract String createUID(DocumentModel doc) throws DocumentException;

}