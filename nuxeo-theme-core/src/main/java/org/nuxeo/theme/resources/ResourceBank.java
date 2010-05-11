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
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;

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

    public byte[] getResourceContent(String typeName, String collectionName,
            String resourceId) {
        String src = String.format("%s/%s/%s/%s", connectionUrl, typeName,
                URIUtils.quoteURIPathComponent(collectionName, true),
                URIUtils.quoteURIPathComponent(resourceId, true));
        log.debug("Loading THEME " + typeName + " from: " + src);
        try {
            return Utils.fetchUrl(new URL(src));
        } catch (Exception e) {
            log.error("Could not retrieve RESOURCE: " + src
                    + " from THEME BANK: " + bankName);
        }
        return null;
    }

    public List<String> getImageIndex() {
        List<String> paths = new ArrayList<String>();
        String src = String.format("%s/images", connectionUrl);
        String list = "";
        try {
            list = Utils.fetchUrl(new URL(src)).toString();
        } catch (Exception e) {
            log.error("Could not retrieve image list: " + src
                    + " from THEME BANK: " + bankName);
            return paths;
        }
        for (Object path : JSONArray.fromObject(list)) {
            paths.add((String) path);
        }
        return paths;
    }

}
