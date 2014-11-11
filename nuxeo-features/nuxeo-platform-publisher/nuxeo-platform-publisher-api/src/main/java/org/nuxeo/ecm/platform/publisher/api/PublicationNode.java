/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.api;

import org.nuxeo.ecm.core.api.ClientException;

import java.io.Serializable;
import java.util.List;

/**
 * Interface for a Node inside the publication tree. The Node is abstract, the
 * implementation could be : a Core Folder, a FileSystem directory, a Folder on a
 * remote core ...
 *
 * @author tiry
 */
public interface PublicationNode extends Serializable {

    String getTitle() throws ClientException;

    String getName() throws ClientException;

    PublicationNode getParent();

    List<PublicationNode> getChildrenNodes() throws ClientException;

    List<PublishedDocument> getChildrenDocuments() throws ClientException;

    String getNodeType();

    String getType();

    String getPath();

    String getTreeConfigName();

    String getSessionId();

}
