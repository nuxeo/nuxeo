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
 * $Id: Registry.java 2531 2006-09-04 23:01:57Z janguenot $
 */
package org.nuxeo.ecm.platform.transform.interfaces;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;

/**
 * Document used by the transformation engine.
 * <p>
 * Contains a SerializableInputStream along with a mimetype information.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface TransformDocument extends Serializable {

    /**
     * Returns the document mimetype.
     *
     * @return string holding the mimetype
     * @throws Exception
     */
    String getMimetype() throws Exception;

    // support for other plain values
    /**
     * Regular properties that could be set as outcome from a transformation
     * process. These properties could be written back to the nuxeo document.
     *
     * @param name the key for the property
     * @return serializable object corresponding to the given key or
     *         <code>null</code> if not found
     */
    Serializable getPropertyValue(String name);

    /**
     * TODO comment please
     *
     * @return TODO
     */
    Map<String, Serializable> getProperties();

    /**
     * Returns a blob instance for
     *
     * @return
     */
    Blob getBlob();

}
