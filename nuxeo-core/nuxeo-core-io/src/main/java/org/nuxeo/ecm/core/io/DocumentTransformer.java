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
 *
 * $Id: DocumentTransformer.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io;

import java.io.IOException;

/**
 * A document transformer.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// Not used.
public interface DocumentTransformer {

    /**
     * Transforms the given document and returns true to pass to the next
     * transformer or false to exit from the transformation chain.
     *
     * @param doc the document to transform
     * @return true to continue with the next transformer or false to exit
     *         transformation chain
     */
    boolean transform(ExportedDocument doc) throws IOException;

}
