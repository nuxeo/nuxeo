/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.source;

import java.io.IOException;
import java.util.List;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * Interface for Source Nodes for the importer
 *
 * @author Thierry Delprat
 */
public interface SourceNode extends Node {

    boolean isFolderish();

    BlobHolder getBlobHolder() throws IOException;

    List<SourceNode> getChildren() throws IOException;

    String getSourcePath();

    default String getPath() {
        return getName();
    }
}
