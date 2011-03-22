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
