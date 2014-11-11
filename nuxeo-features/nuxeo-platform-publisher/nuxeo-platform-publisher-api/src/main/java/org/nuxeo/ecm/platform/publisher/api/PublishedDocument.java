/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.api;

import org.nuxeo.ecm.core.api.DocumentRef;

import java.io.Serializable;

/**
 * Interface of a Document that was published into a PublicationNode.
 *
 * @author tiry
 */
public interface PublishedDocument extends Serializable {

    enum Type {
        REMOTE,
        LOCAL,
        FILE_SYSTEM;
    }

    DocumentRef getSourceDocumentRef();

    String getSourceRepositoryName();

    String getSourceServer();

    String getSourceVersionLabel();

    String getPath();

    String getParentPath();

    /**
     * Returns {@code true} if this document is waiting approval, {@code false}
     * otherwise.
     */
    boolean isPending();

    /**
     * Returns the {@code Type} of this PublishedDocument.
     */
    Type getType();

}
