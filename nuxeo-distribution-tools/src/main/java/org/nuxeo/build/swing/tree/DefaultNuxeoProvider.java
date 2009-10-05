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
 */
package org.nuxeo.build.swing.tree;

import java.awt.Color;

import javax.swing.Icon;

import org.nuxeo.build.maven.graph.Node;
import org.nuxeo.build.swing.IconUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultNuxeoProvider extends ItemProvider {

    @Override
    public String[] getRoots() {
        return new String[] {
                "org.nuxeo.runtime:nuxeo-runtime-parent:1.6.0-SNAPSHOT:pom",
                "org.nuxeo.ecm.core:nuxeo-core-parent:1.6.0-SNAPSHOT:pom",
                "org.nuxeo.ecm.platform:nuxeo-platform-parent:5.3.0-SNAPSHOT:pom",
                "org.nuxeo.ecm.webengine:nuxeo-webengine-parent:5.3.0-SNAPSHOT:pom",

        };
    }

    public Color getForegroundColor(Node node, boolean isSelected) {
        if (isSelected) {
            return node.getArtifact().getArtifactId().startsWith("nuxeo-") ? null : Color.WHITE;
        } else {
            return node.getArtifact().getArtifactId().startsWith("nuxeo-") ? null : Color.GRAY;
        }
    }

    protected boolean isNuxeoNode(Node node) {
        return node.getArtifact().getArtifactId().startsWith("nuxeo-");
    }

    @Override
    public Icon getIcon(Node node, boolean isExpanded) {
        if (isNuxeoNode(node)) {
            String type = node.getArtifact().getType();
            if ("jar".equals(type)) {
                return IconUtils.createImageIcon(ItemProvider.class, "nxjar.gif");
            }
        }
        return super.getIcon(node, isExpanded);
    }

}
