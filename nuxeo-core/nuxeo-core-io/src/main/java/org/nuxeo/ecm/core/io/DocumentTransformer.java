/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
