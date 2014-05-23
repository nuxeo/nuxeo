/*
 * Copyright (c) 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.storage.binary.Binary;

/**
 * This is a compatibility interface for older code using SQLBlob explicitly.
 * <p>
 * New code should be migrated to StorageBlob.
 */
public interface SQLBlob extends Blob, Serializable {

    /**
     * Gets the {@link Binary} attached to this blob.
     *
     * @since 5.9.4
     * @return the binary
     */
    Binary getBinary();

}
