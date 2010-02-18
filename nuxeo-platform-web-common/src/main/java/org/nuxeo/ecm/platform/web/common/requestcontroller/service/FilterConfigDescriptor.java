/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

import java.io.Serializable;
import java.util.regex.Pattern;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Descriptor for {@link RequestFilterConfig}
 *
 * @author tiry
 * @author ldoguin
 */
@XObject(value = "filterConfig")
public class FilterConfigDescriptor implements Serializable {

    public static final String DEFAULT_CACHE_DURATION = "3599";

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    @XNode("@synchonize")
    protected boolean useSync;

    @XNode("@transactional")
    protected boolean useTx;

    @XNode("@cached")
    protected boolean cached;

    @XNode("@cacheTime")
    protected String cacheTime;

    @XNode("@private")
    protected boolean isPrivate;

    @XNode("@grant")
    protected boolean grant = true;

    protected String pattern;

    protected Pattern compiledPattern;

    public FilterConfigDescriptor() {
    }

    public FilterConfigDescriptor(String name, String pattern, boolean grant,
            boolean tx, boolean sync, boolean cached, boolean isPrivate, String cacheTime) {
        this.name = name;
        this.pattern = Framework.expandVars(pattern);
        this.grant = grant;
        this.useSync = sync;
        this.useTx = tx;
        this.cached = cached;
        this.isPrivate = isPrivate;
        this.cacheTime = cacheTime;
    }

    public String getName() {
        if (name == null) {
            return pattern;
        }
        return name;
    }

    public boolean useSync() {
        return useSync;
    }

    public boolean useTx() {
        return useTx;
    }

    public boolean isGrantRule() {
        return grant;
    }

    public boolean isCached() {
        return cached;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public String getCacheTime() {
        if (cacheTime == null || cacheTime.equals("")) {
            cacheTime = DEFAULT_CACHE_DURATION;
        }
        return cacheTime;
    }

    public String getPatternStr() {
        return pattern;
    }

    public Pattern getCompiledPattern() {
        if (compiledPattern == null) {
            compiledPattern = Pattern.compile(pattern);
        }
        return compiledPattern;
    }

    @XNode("pattern")
    public void setPattern(String pattern) {
        this.pattern = Framework.expandVars(pattern);
    }

}
