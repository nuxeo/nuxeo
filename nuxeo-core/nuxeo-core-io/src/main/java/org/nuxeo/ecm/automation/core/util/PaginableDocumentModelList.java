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
package org.nuxeo.ecm.automation.core.util;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface PaginableDocumentModelList extends DocumentModelList, Paginable<DocumentModel> {

    public static final String CODEC_PARAMETER_NAME = "URLCodecName";

    /**
     * Returns the name of what will be used to compute the document URLs, usually a codec name.
     *
     * @since 5.6
     */
    String getDocumentLinkBuilder();

}
