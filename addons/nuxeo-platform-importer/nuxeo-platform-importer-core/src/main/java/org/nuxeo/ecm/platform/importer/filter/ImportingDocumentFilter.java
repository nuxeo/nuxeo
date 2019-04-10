/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.importer.filter;

import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 * Interface for filters used to chose if a {@code SourceNode} should be
 * imported or not.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public interface ImportingDocumentFilter {

    /**
     * Returns {@code true} if the given {@code SourceNode} should be imported,
     * {@code false} otherwise.
     */
    public boolean shouldImportDocument(SourceNode sourceNode);

}
