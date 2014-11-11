/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.api.repository.cache;

import org.nuxeo.ecm.core.api.DocumentModel;

public interface DocumentModelCacheListener {

    void documentsChanged(DocumentModel[] docs, boolean urgent);

    void subreeChanged(DocumentModel[] docs, boolean urgent);

}
