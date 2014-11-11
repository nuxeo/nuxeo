/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
