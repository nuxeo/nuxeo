/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.impl.localfs;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.publisher.api.AbstractPublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FSPublicationNode extends AbstractPublicationNode implements PublicationNode {

    private static final long serialVersionUID = 1L;

    protected File folder;

    protected String sid;

    public FSPublicationNode(String path, String treeName, String sid) {
        this(new File(path), treeName, sid);
    }

    public FSPublicationNode(File folder, String treeName, String sid) {
        if (!folder.exists()) {
            throw new IllegalArgumentException("Root publication folder does not exist");
        }
        this.folder = folder;
        this.treeName = treeName;
        this.sid = sid;
    }

    public List<PublishedDocument> getChildrenDocuments() {

        List<PublishedDocument> childrenDocs = new ArrayList<PublishedDocument>();
        List<File> children = Arrays.asList(folder.listFiles());
        Collections.sort(children);
        for (File child : children) {
            if (!child.isDirectory()) {
                try {
                    childrenDocs.add(new FSPublishedDocument(child));
                } catch (NotFSPublishedDocumentException e) {
                    throw new NuxeoException("Error whild creating PublishedDocument from file", e);
                }
            }
        }
        return childrenDocs;
    }

    public List<PublicationNode> getChildrenNodes() {
        List<PublicationNode> childrenNodes = new ArrayList<PublicationNode>();
        List<File> children = Arrays.asList(folder.listFiles());
        Collections.sort(children);
        for (File child : children) {
            if (child.isDirectory()) {
                childrenNodes.add(new FSPublicationNode(child, getTreeConfigName(), sid));
            }
        }
        return childrenNodes;
    }

    public String getName() {
        return folder.getName();
    }

    public PublicationNode getParent() {
        String parentPath = new Path(getPath()).removeLastSegments(1).toString();
        File parentFolder = new File(parentPath);
        return new FSPublicationNode(parentFolder, getTreeConfigName(), sid);
    }

    public String getPath() {
        return folder.getAbsolutePath();
    }

    public String getTitle() {
        return getName();
    }

    public String getSessionId() {
        return sid;
    }

}
