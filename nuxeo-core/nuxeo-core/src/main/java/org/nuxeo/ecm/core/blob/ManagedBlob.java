/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import org.nuxeo.ecm.core.api.Blob;

/**
 * Interface for {@link Blob}s created and managed by the {@link BlobManager}.
 *
 * @since 7.2
 */
public interface ManagedBlob extends Blob {

    /**
     * Gets the id of the {@link BlobProvider} managing this blob.
     *
     * @return the blob provider id
     */
    String getProviderId();

    /**
     * Gets the stored representation of this blob.
     *
     * @return the stored representation
     */
    String getKey();

}
