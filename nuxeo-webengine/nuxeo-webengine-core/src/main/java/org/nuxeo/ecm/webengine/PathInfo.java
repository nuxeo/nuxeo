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
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.webengine.util.Attributes;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PathInfo {

    public static final Path EMPTY_PATH = new Path("");
    public static final Path ROOT_PATH = new Path("/");

    protected DocumentRef document; // the root document if any
    protected final Path path; // original path (without action)
    protected final Path applicationPath; // the application path
    protected Path traversalPath; // original path - application path
    protected Path trailingPath = EMPTY_PATH; // trailing segments that were not traversed
    protected String action;
    protected String script;
    protected Attributes attrs;

    public PathInfo(Path path, Path applicationPath, Attributes attrs) {
        this.path = path;
        this.applicationPath = applicationPath;
        traversalPath = this.path;
        int cnt = applicationPath.segmentCount();
        if (cnt > 0) {
            traversalPath = this.path.removeFirstSegments(cnt).makeAbsolute();
        } else {
            traversalPath = this.path;
        }
        setAttributes(attrs);
    }

    public PathInfo(Path path) {
        this(path, ROOT_PATH, Attributes.EMPTY_ATTRS);
    }

    public PathInfo(Path path, Path applicationPath) {
        this(path, applicationPath, Attributes.EMPTY_ATTRS);
    }

    public Path getApplicationPath() {
        return applicationPath;
    }

    public void setAttributes(Attributes attrs) {
        this.attrs = attrs == null ? Attributes.EMPTY_ATTRS : attrs;
    }

    public Path getPath() {
        return path;
    }

    public void setTrailingPath(Path trailingPath) {
        this.trailingPath = trailingPath == null ? EMPTY_PATH : trailingPath.makeAbsolute();
    }

    public Path getTrailingPath() {
        return trailingPath;
    }

    public Path getTraversalPath() {
        return traversalPath;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getScript() {
        return script;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public void setDocument(DocumentRef root) {
        this.document = root;
    }

    public DocumentRef getDocument() {
        return document;
    }

    /**
     * Tests whether this path info has a traversal path
     * (i.e. the traversal path contains at least one segment).
     * @return
     */
    public boolean hasTraversalPath() {
        return traversalPath.segmentCount() > 0;
    }

    /**
     * Tests whether this path info has a trailing path
     * (i.e. the trailing path contains at least one segment).
     *
     * @return
     */
    public boolean hasTrailingPath() {
        return trailingPath.segmentCount() > 0;
    }


    /**
     * Tests whether pathInfo is empty (input path is either null, "" or "/").
     *
     * @return true if this path info is empty false otherwise
     */
    public boolean isEmpty() {
        return path.segmentCount() == 0;
    }

    /**
     * Tests whether this path info specify a document mapping
     * (i.e. the root property is a non empty string).
     *
     * @return
     */
    public boolean hasDocumentMapping() {
        return document != null;
    }

    public Attributes getAttributes() {
        return attrs;
    }

    @Override
    public String toString() {
        return path.toString() + " [traversal: "
            +traversalPath.toString()+"; trailing: "
            +trailingPath.toString()+"; root: "+document+"; script: "+script+"; action: "+action+"]";
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
