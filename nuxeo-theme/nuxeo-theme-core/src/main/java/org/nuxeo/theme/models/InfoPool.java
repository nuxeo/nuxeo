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

package org.nuxeo.theme.models;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.theme.Registrable;
import org.nuxeo.theme.rendering.RenderingInfo;

public final class InfoPool implements Registrable {

    private static final String INFOID_PREFIX = "i";

    private Map<String, Info> infoMap = new HashMap<String, Info>();

    public void register(RenderingInfo info) {
        infoMap.put(computeInfoId(info), info);
    }

    public Info get(String key) {
        return infoMap.get(key);
    }

    public Map<String, Info> getInfoMap() {
        return infoMap;
    }

    public void setInfoMap(Map<String, Info> map) {
        infoMap = map;
    }

    public void clear() {
        infoMap.clear();
    }

    public static String computeInfoId(RenderingInfo info) {
        return INFOID_PREFIX + info.getUid().toString();
    }

}
