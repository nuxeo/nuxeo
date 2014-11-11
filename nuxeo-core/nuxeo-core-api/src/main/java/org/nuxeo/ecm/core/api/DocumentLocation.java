/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: DocumentLocation.java 25074 2007-09-18 14:23:08Z atchertchian $
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;

/**
 * Document server name with its unique identifier within this server.
 */
public interface DocumentLocation extends Serializable {

    /**
     * Returns the document server name.
     */
    String getServerName();

    /**
     * Returns the document reference.
     */
    DocumentRef getDocRef();

    /**
     * Returns the document path reference
     */
    PathRef getPathRef();

    /**
     * Returns the document id reference
     */
    IdRef getIdRef();

}
