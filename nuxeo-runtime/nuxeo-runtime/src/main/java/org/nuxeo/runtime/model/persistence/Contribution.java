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
     * Gets the contribution name.
     */
    String getName();

    /**
     * Gets the contribution description.
     */
    String getDescription();

    /**
     * Sets the contribution description.
     */
    void setDescription(String description);

    /**
     * Whether this contribution should be automatically installed at startup.
     */
    boolean isDisabled();

    /**
     * Sets the auto install flag for this contribution.
     */
    void setDisabled(boolean isAutoStart);

    /**
     * Gets the contribution XML content. The content should be in Nuxeo XML
     * component format.
     */
    @Override
    InputStream getStream();

    /**
     * Gets the contribution XML content. The content should be in Nuxeo XML
     * component format.
     */
    String getContent();

}
