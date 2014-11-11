/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.storage.sql.Node;

/**
 * Base interface for SQL documents.
 */
public interface SQLDocument extends Document {

    String SIMPLE_TEXT_SYS_PROP = "simpleText";

    String BINARY_TEXT_SYS_PROP = "binaryText";

    String FULLTEXT_JOBID_SYS_PROP = "fulltextJobId";

    /**
     * Returns the node with info about the hierarchy location.
     */
    Node getNode();

}
