/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benjamin JALON<bjalon@nuxeo.com>
 */
package org.nuxeo.ecm.platform.importer.xml.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 * Source Node filtering only Folder, XML and ZIP files.
 *
 * @since 5.7.3
 */
public class XMLFileSourceNode extends FileSourceNode {

    public static final Log log = LogFactory.getLog(XMLFileSourceNode.class);

    public XMLFileSourceNode(File file) {
        super(file);
    }

    @Override
    public List<SourceNode> getChildren() {
        List<SourceNode> children = new ArrayList<>();

        File[] childrenFile = file.listFiles();
        for (File child : childrenFile) {
            if (child.getName().endsWith(".xml") || child.getName().endsWith(".zip") || child.isDirectory()) {
                children.add(new XMLFileSourceNode(child));
            } else {
                log.info("File ignored as not xml or zip or directory file " + child.getAbsolutePath());
            }
        }
        return children;
    }

}
