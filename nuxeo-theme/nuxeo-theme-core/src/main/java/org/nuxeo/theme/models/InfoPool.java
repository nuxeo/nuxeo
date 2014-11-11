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

import org.nuxeo.theme.rendering.RenderingInfo;

public final class InfoPool {

    private static final String INFOID_PREFIX = "i";

    protected static final ThreadLocal<HashMap<String, Info>> threadInstance = new ThreadLocal<HashMap<String, Info>>() {
        @Override
        protected HashMap<String, Info> initialValue() {
            return new HashMap<String, Info>();
        }
    };

    public static Map<String, Info> getInfoMap() {
        return threadInstance.get();
    }

    public static void register(RenderingInfo info) {
        getInfoMap().put(computeInfoId(info), info);
    }

    public static Info get(String key) {
        return getInfoMap().get(key);
    }

    public static String computeInfoId(RenderingInfo info) {
        return INFOID_PREFIX + info.getUid().toString();
    }

}
