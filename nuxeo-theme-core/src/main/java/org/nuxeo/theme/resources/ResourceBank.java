/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.resources;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.Utils;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("bank")
public class ResourceBank implements Type {

    private static final Log log = LogFactory.getLog(ResourceBank.class);

    private static final Pattern resourceNamePattern = Pattern.compile(
            "(.*?)\\s\\((.*?)\\)$", Pattern.DOTALL);

    @XNode("@name")
    public String bankName;

    @XNode("@url")
    private String connectionUrl;

    public ResourceBank() {
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public String getTypeName() {
        return bankName;
    }

    public TypeFamily getTypeFamily() {
        return TypeFamily.RESOURCE_BANK;
    }

    public String getResourceContent(String typeName, String resourceId) {
        final Matcher resourceNameMatcher = resourceNamePattern.matcher(resourceId);
        if (resourceNameMatcher.find()) {
            String collectionName = resourceNameMatcher.group(2);
            String resourceName = resourceNameMatcher.group(1);

            if ("style".equals(typeName)) {
                resourceName = resourceName + ".css";
            }

            String src = String.format("%s/%s/%s/%s", connectionUrl, typeName,
                    URIUtils.quoteURIPathComponent(collectionName, true),
                    URIUtils.quoteURIPathComponent(resourceName, true));

            log.debug("Loading THEME " + typeName + " from: " + src);
            try {
                return Utils.fetchUrl(new URL(src));
            } catch (Exception e) {
                log.error("Could not retrieve RESOURCE: " + resourceId
                        + " from THEME BANK: " + bankName);
            }

        }
        return null;
    }

}
