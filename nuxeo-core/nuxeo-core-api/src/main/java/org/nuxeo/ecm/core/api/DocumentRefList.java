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
 * A serializable list of document references.
 * Use this instead of <code>List&lt;DocumentRef&gt;</code> when a list of document references should be returned.
 * <p>
 * This object is type safe and can help services which need to dynamically discover which type of object is returned.
 * (see operation framework for this)
 * <p>
 * This class is the equivalent of {@link DocumentModelList} but for document references.
 *
 * @author Bogdan Stefanescu
 */
public interface DocumentRefList extends List<DocumentRef>, Serializable {

    /**
     * Returns the total size of the bigger list this is a part of.
     *
     * @return the total size
     */
    long totalSize();

}
