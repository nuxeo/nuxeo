/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.model.persistence;

import java.io.InputStream;

import org.nuxeo.runtime.model.StreamRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Contribution extends StreamRef {

    /**
     * Get the contribution name
     *
     * @return
     */
    String getName();

    /**
     * Get the contribution description.
     *
     * @return
     */
    String getDescription();

    /**
     * Set the contribution description
     *
     * @param description
     */
    void setDescription(String description);

    /**
     * Whether this contribution should be automatically installed at startup.
     *
     * @return
     */
    boolean isDisabled();

    /**
     * Set the auto install flag for this contribution.
     *
     * @param isAutoInstall
     */
    void setDisabled(boolean isAutoStart);

    /**
     * Get the contribution XML content. The content should be in Nuxeo XML
     * component format.
     *
     * @return
     */
    InputStream getStream();

    /**
     * Get the contribution XML content. The content should be in Nuxeo XML
     * component format.
     *
     * @return
     */
    String getContent();

}
