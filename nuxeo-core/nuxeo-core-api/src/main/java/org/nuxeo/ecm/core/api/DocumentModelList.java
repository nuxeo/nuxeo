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

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.List;

/**
 * A serializable list of document models.
 * <p>
 * It may include information about which part of a bigger list it represents.
 *
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public interface DocumentModelList extends List<DocumentModel>, Serializable {

    /**
     * Returns the total size of the bigger list this is a part of.
     *
     * @return the total size
     */
    long totalSize();

}
