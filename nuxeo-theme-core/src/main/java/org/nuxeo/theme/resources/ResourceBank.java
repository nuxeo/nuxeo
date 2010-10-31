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
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.Utils;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("bank")
public class ResourceBank implements Type {

    private static final Log log = LogFactory.getLog(ResourceBank.class);

    @XNode("@name")
    public String name;

    private String connectionUrl;

    @XNode("@url")
    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = Framework.expandVars(connectionUrl);
    }

    public ResourceBank() {
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    @Override
    public String getTypeName() {
        return name;
    }

    @Override
    public TypeFamily getTypeFamily() {
        return TypeFamily.RESOURCE_BANK;
    }

    public boolean checkStatus() {
        String src = String.format("%s/status", connectionUrl);
        byte[] status;
        try {
            status = Utils.fetchUrl(new URL(src));
        } catch (Exception e) {
            return false;
        }
        return status != null && "OK".equals(new String(status));
    }

    public byte[] getResourceContent(String collectionName, String typeName,
            String resourceId) {
        String src = String.format("%s/%s/%s/%s", connectionUrl,
                URIUtils.quoteURIPathComponent(collectionName, true),
                URIUtils.quoteURIPathComponent(typeName, true),
                URIUtils.quoteURIPathComponent(resourceId, true));
        log.debug("Loading THEME " + typeName + " from: " + src);
        try {
            return Utils.fetchUrl(new URL(src));
        } catch (Exception e) {
            log.error("Could not retrieve RESOURCE: " + src
                    + " from THEME BANK: " + name);
        }
        return null;
    }

    public List<String> getImages() {
        List<String> paths = new ArrayList<String>();
        String src = String.format("%s/json/images", connectionUrl);
        String list = "";
        try {
            list = new String(Utils.fetchUrl(new URL(src)));
        } catch (Exception e) {
            log.error("Could not retrieve image list: " + src
                    + " from THEME BANK: " + name);
            return paths;
        }
        for (Object path : JSONArray.fromObject(list)) {
            paths.add((String) path);
        }
        return paths;
    }

    public List<String> getCollections() {
        List<String> paths = new ArrayList<String>();
        String src = String.format("%s/json/collections", connectionUrl);
        String list = "";
        try {
            list = new String(Utils.fetchUrl(new URL(src)));
        } catch (Exception e) {
            log.error("Could not retrieve collection list: " + src
                    + " from THEME BANK: " + name);
            return paths;
        }
        for (Object path : JSONArray.fromObject(list)) {
            paths.add((String) path);
        }
        return paths;
    }

    @SuppressWarnings("unchecked")
    public List<SkinInfo> getSkins() {
        List<SkinInfo> skins = new ArrayList<SkinInfo>();
        String src = String.format("%s/json/skins", connectionUrl);
        String list = "";
        try {
            list = new String(Utils.fetchUrl(new URL(src)));
        } catch (Exception e) {
            log.error("Could not retrieve skin list: " + src
                    + " from THEME BANK: " + name);
            return skins;
        }
        for (Object object : JSONArray.fromObject(list)) {
            Map<String, Object> skin = JSONObject.fromObject(object);
            skins.add(new SkinInfo((String) skin.get("name"),
                    (String) skin.get("bank"), (String) skin.get("collection"),
                    (String) skin.get("resource"),
                    (String) skin.get("preview"), (Boolean) skin.get("base")));
        }
        return skins;
    }

    public String getName() {
        return name;
    }

}
