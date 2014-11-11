/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.model;

import org.nuxeo.ecm.core.api.DocumentException;

/**
 * Methods specific to a proxy.
 */
public interface DocumentProxy extends Document {

    /**
     * Gets the document (version or live document) to which this proxy points.
     */
    Document getTargetDocument();

    /**
     * Sets the document (version or live document) to which this proxy points.
     */
    void setTargetDocument(Document target) throws DocumentException;

}
