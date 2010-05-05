/*
 * (C) Copyright 2006-2010 Nuxeo SAS <http://nuxeo.com> and others
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.Utils;
import org.nuxeo.theme.themes.ThemeIOException;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("bank")
public class ResourceBank implements Type {
    
    private static final Log log = LogFactory.getLog(ResourceBank.class);

    private static final Pattern resourceNamePattern = Pattern.compile(
            "(.*?)\\s\\((.*?)\\)$", Pattern.DOTALL);

    private final Map<String, String> index = new HashMap<String, String>();
    
    @XNode("@name")
    public String name;

    @XNode("@connection-url")
    private String connectionUrl;

    public ResourceBank() {
        generateIndex();
    }

    public void generateIndex() { 
        System.out.println("generate index");
    }
    
    public String getConnectionUrl() {
        return connectionUrl;
    }

    public String getTypeName() {
        return name;
    }

    public TypeFamily getTypeFamily() {
        return TypeFamily.RESOURCE_BANK;
    }

    public String getResourceContent(String typeName, String resourceId) throws ThemeIOException {
        final Matcher resourceNameMatcher = resourceNamePattern.matcher(resourceId);
        if (resourceNameMatcher.find()) {
            String collectionName = resourceNameMatcher.group(2).trim();
            String resourceName = resourceNameMatcher.group(1).trim();
            String path = String.format("/%s/%s/%s", typeName, collectionName,
                    resourceName);
            String src = connectionUrl + path;
            try {
                return Utils.fetchUrl(new URL(src));
            } catch (MalformedURLException e) {
                throw new ThemeIOException(e);
            }
        }
        return null;
    }

}
