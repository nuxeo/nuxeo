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
 * $Id: MimetypeEntry.java 20676 2007-06-17 15:28:28Z sfermigier $
 */
package org.nuxeo.ecm.platform.mimetype.interfaces;

import java.io.Serializable;
import java.util.List;

/**
 * MimetypeEntry entry.
 * <p>
 * Holds meta information about a mimetype.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @author <a href="mailto:lgodard@nuxeo.com">Laurent Godard</a>
 */
public interface MimetypeEntry extends Serializable {

    /**
     * Returns the mimetype's names.
     *
     * @return list of strings containing the mimetype names
     */
    List<String> getMimetypes();

    /**
     * Returns the main RFC-2046 name for this mime type.
     * <p>
     * If this mime type has several names ('text/restructured', 'text-x-rst'),
     * then this method will always return the first form.
     *
     * @return the main RFC-2046 name for this mime type
     */
    String getNormalized();

    /**
     * Returns the major part of the RFC-2046.
     *
     * @see #getNormalized()
     *
     * @return the major part of the RFC-2046 name of this mime type.
     */
    String getMajor();

    /**
     * Returns the minor part of the RFC-2046.
     *
     * @see #getNormalized()
     *
     * @return string
     */
    String getMinor();


    /**
     * Returns the path of the icon for this mimetype.
     *
     * @return String
     */
    String getIconPath();

    /**
     * Returns the list of extensions for this mimetype.
     *
     * @return list of strings that contain different mimetypes
     */
    List<String> getExtensions();

    /**
     * Is this a binary mimetype?
     * <p>
     * Might be useful to know if we can read it as human.
     *
     * @return boolean
     */
    boolean isBinary();

    /**
     * Is this mimetype supported by a LiveEdit plugin?
     * <p>
     * Might be useful to know to display the Edit online link.
     *
     * @return boolean
     */
    boolean isOnlineEditable();

    /**
     * Is this mimetype supported by a oleExtract plugin?
     * <p>
     * If the property is true, processes the ole Extraction, otherwise skip.
     *
     * @return boolean
     */
    boolean isOleSupported();

}
