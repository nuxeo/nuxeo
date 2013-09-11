/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 *
 */
public class XMLFileSourceNode extends FileSourceNode {

    public static final Log log = LogFactory.getLog(XMLFileSourceNode.class);

    public XMLFileSourceNode(File file) {
        super(file);
    }

    @Override
    public List<SourceNode> getChildren() {
        List<SourceNode> children = new ArrayList<SourceNode>();

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
