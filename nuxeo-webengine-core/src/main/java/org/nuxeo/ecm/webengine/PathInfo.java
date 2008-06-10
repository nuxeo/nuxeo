/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.webengine.servlet.WebConst;
import org.nuxeo.ecm.webengine.util.Attributes;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PathInfo {

    public final static Path EMPTY_PATH = new Path("");

    protected Path path;
    protected Path leadingPath = EMPTY_PATH;
    protected Path traversalPath;
    protected Path trailingPath = EMPTY_PATH;
    protected String action;
    protected String root;
    protected String script;
    protected Attributes attrs;

    public PathInfo(String path) {
        this (path, null, Attributes.EMPTY_ATTRS);
    }

    public PathInfo(String path, String leadingPath, Attributes attrs) {
        if (path == null || path.length() == 0
                || (path.length() == 1 && path.charAt(0) == '/')) {
            this.path = EMPTY_PATH;
        } else {
            int p = path.lastIndexOf(WebConst.ACTION_SEPARATOR);
            if (p > -1) {
                this.action = path.substring(p+WebConst.ACTION_SEPARATOR.length());
                path = path.substring(0, p);
            }
            this.path = new Path(path).makeAbsolute().removeTrailingSeparator();
        }
        if (leadingPath == null || leadingPath.length() ==0
                || (path.length() == 1 && path.charAt(0) == '/')) {
            this.leadingPath = EMPTY_PATH;
            traversalPath = this.path;
        } else {
            this.leadingPath = new Path(leadingPath).makeAbsolute().removeTrailingSeparator();
            // TODO check whether the leading path is valid?
//            int p = this.path.matchingFirstSegments(this.leadingPath);
//            if (p != this.leadingPath.segmentCount()) {
//                throw new IllegalArgumentException("Not a valid trailingPath: "+trailingPath+"for path: "+path);
//            }
            traversalPath = this.path.removeFirstSegments(this.leadingPath.segmentCount()).makeAbsolute();
            this.attrs = attrs;
        }
    }

    /**
     * @param trailingPath the trailingPath to set.
     */
    public void setTrailingPath(Path trailingPath) {
        this.trailingPath = trailingPath == null ? EMPTY_PATH : trailingPath.makeAbsolute();
    }

    /**
     * @return the trailingPath.
     */
    public Path getTrailingPath() {
        return trailingPath;
    }

    /**
     * @param traversalPath the traversalPath to set.
     */
    public void setTraversalPath(Path traversalPath) {
        this.traversalPath = traversalPath == null ? EMPTY_PATH : traversalPath.makeAbsolute();
    }

    /**
     * @return the traversalPath.
     */
    public Path getTraversalPath() {
        return traversalPath;
    }

    /**
     * @param leadingPath the leadingPath to set.
     */
    public void setLeadingPath(Path leadingPath) {
        this.leadingPath = leadingPath == null ? EMPTY_PATH : leadingPath.makeAbsolute();
    }

    /**
     * @return the leadingPath.
     */
    public Path getLeadingPath() {
        return leadingPath;
    }

    /**
     * @param script the script to set.
     */
    public void setScript(String script) {
        this.script = script;
    }

    /**
     * @return the script.
     */
    public String getScript() {
        return script;
    }

    /**
     * @param action the action to set.
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * @return the action.
     */
    public String getAction() {
        return action;
    }

    /**
     * @param root the root to set.
     */
    public void setRoot(String root) {
        this.root = root;
    }

    /**
     * @return the root.
     */
    public String getRoot() {
        return root;
    }

    /**
     * Tests whether this path info has a traversal path
     * (i.e. the traversal path contains at least one segment)
     * @return
     */
    public boolean hasTraversalPath() {
        return traversalPath.segmentCount() > 0;
    }

    /**
     * Tests whether this path info has a traversal path
     * (i.e. the trailing path contains at least one segment)
     * @return
     */
    public boolean hasTrailingPath() {
        return trailingPath.segmentCount() > 0;
    }

    /**
     * Tests whether this path info has a traversal path
     * (i.e. the leading path contains at least a segment)
     * @return
     */
    public boolean hasLeadingPath() {
        return leadingPath.segmentCount() > 0;
    }

    /**
     * This pathInfo is empty (input path is either null, "" or "/")
     * @return true if this path info is empty false otherwise
     */
    public boolean isEmpty() {
        return path.segmentCount() == 0;
    }

    /**
     * Tests whether this path info specify a document mapping
     * (i.e. the root property is a non empty string)
     * @return
     */
    public boolean hasDocumentMapping() {
        return root != null && root.length() > 0;
    }

    /**
     * @return the attrs.
     */
    public Attributes getAttributes() {
        return attrs;
    }

    @Override
    public String toString() {
        return path.toString() + " [leading: "+leadingPath.toString()+"; traversal: "
            +traversalPath.toString()+"; trailing: "
            +trailingPath.toString()+"; root: "+root+"; script: "+script+"; action: "+action+"]";
    }

    @Override
    public boolean equals(Object obj) {
        return path.equals(obj);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

}
