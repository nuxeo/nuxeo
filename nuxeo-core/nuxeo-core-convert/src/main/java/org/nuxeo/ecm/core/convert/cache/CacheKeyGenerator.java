/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.core.convert.cache;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * Simple helper to handle cache key generation.
 *
 * @author tiry
 */
public class CacheKeyGenerator {

    // Utility class.
    private CacheKeyGenerator() {
    }

    public static String computeKey(String converterName, BlobHolder blobHolder,
            Map<String, Serializable> parameters) {

        StringBuilder sb = new StringBuilder();

        sb.append(converterName);
        sb.append(":");
        try {
            sb.append(blobHolder.getHash());
        } catch (ClientException e) {
            throw new IllegalStateException("Can not fetch Hash from BlobHolder", e);
        }

        if (parameters != null) {
            for (String key : parameters.keySet()) {
                sb.append(":").append(key);
                sb.append(":").append(String.valueOf(parameters.get(key)));
            }
        }
        return sb.toString();
    }

}
