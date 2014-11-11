/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine;

import java.util.Arrays;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.session.UserSession;
import org.nuxeo.ecm.webengine.util.PathMatcher;

/**
 * Configure how a given path is handled by the WebEngine filter.
 * <p>
 * If <b>autoTx</b> is true (which is the default) then a transaction will be
 * started each time a path matching the given path specification is requested.
 * (the transaction is started in a filter before the JAX-RS resource is called
 * and closed after the response is sent to the output stream). If false then no
 * transaction handling is done. The default is to start a transaction for any
 * path but: [^/]+/skin/.*
 * <p>
 * If <b>stateful</b> flag is set (the default is false) then the core session
 * which is provided to the JAX-RS resource (through
 * {@link UserSession#getCoreSession()}) will be reused for each request in the
 * same HTPP session (i.e. the core session is stored in the HTTP Session and
 * closed when the session expires). By default the provided core session has a
 * REQUEST scope (it is closed automatically when request ends).
 * <p>
 * The <b>value</b> attribute is required and must be used to specify the path
 * pattern. The path pattern is either a prefix or a regular expression. If the
 * <b>regex</b> parameter is true (the default is false) then the value will be
 * expected to be a regular expression. A prefix denotes a path starting with
 * 'prefix'. Paths are relative to the webengine servlet (i.e. they correspond
 * to the servlet path info in the JAX-RS servlet) - and always begin with a
 * '/'.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("path")
public class PathDescriptor implements Comparable<PathDescriptor> {

    @XNode("@value")
    protected String value;

    @XNode("@regex")
    protected boolean regex = false;

    @XNode("@autoTx")
    protected Boolean autoTx;

    @XNode("@stateful")
    protected Boolean stateful;

    private PathMatcher matcher;

    public PathDescriptor() {

    }

    public PathDescriptor(String value, boolean regex, boolean autoTx,
            boolean stateful) {
        this.value = value;
        this.regex = regex;
        this.autoTx = autoTx;
        this.stateful = stateful;
    }

    public PathMatcher getMatcher() {
        return matcher;
    }

    public String getValue() {
        return value;
    }

    public Boolean getAutoTx() {
        return autoTx;
    }

    public Boolean getStateful() {
        return stateful;
    }

    public boolean isAutoTx(boolean defaultValue) {
        return autoTx == null ? defaultValue : autoTx.booleanValue();
    }

    public boolean isStateful(boolean defaultValue) {
        return stateful == null ? defaultValue : stateful.booleanValue();
    }

    public PathMatcher createMatcher() {
        if (value != null) {
            if (!value.startsWith("/")) {
                value = "/" + value;
            }
            matcher = regex ? PathMatcher.getRegexMatcher(value)
                    : PathMatcher.getPrefixMatcher(value);
        } else {
            throw new IllegalArgumentException("Path value is required");
        }
        return matcher;
    }

    public boolean match(String path) {
        return matcher.match(path);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof PathDescriptor) {
            PathDescriptor pd = ((PathDescriptor) obj);
            return value != null && value.equals(pd.value) || value == pd.value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value + "; autoTx: " + autoTx + "; stateful: " + stateful;
    }

    @Override
    public int compareTo(PathDescriptor o) {
        if (regex != o.regex) {
            return regex ? 1 : -1;
        }
        int len1 = value.length();
        int len2 = o.value.length();
        if (len1 == len2) {
            return value.compareTo(o.value);
        }
        return len2 - len1;
    }

    public static void main(String[] args) {
        PathDescriptor[] pds = new PathDescriptor[] {
                new PathDescriptor("/a/b/d/.*\\.gif", true, false, false),
                new PathDescriptor("/a", false, false, false),
                new PathDescriptor("/a/b/c", false, false, false),
                new PathDescriptor("/b", false, false, false),
                new PathDescriptor("/b/c", false, false, false) };
        Arrays.sort(pds);
        for (PathDescriptor pd : pds) {
            System.out.println(pd.value);
        }
    }
}
