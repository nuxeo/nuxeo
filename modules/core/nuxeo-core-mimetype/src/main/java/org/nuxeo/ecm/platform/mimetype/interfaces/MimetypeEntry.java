/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * If this mime type has several names ('text/restructured', 'text-x-rst'), then this method will always return the
     * first form.
     *
     * @return the main RFC-2046 name for this mime type
     */
    String getNormalized();

    /**
     * Returns the major part of the RFC-2046.
     *
     * @see #getNormalized()
     * @return the major part of the RFC-2046 name of this mime type.
     */
    String getMajor();

    /**
     * Returns the minor part of the RFC-2046.
     *
     * @see #getNormalized()
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
