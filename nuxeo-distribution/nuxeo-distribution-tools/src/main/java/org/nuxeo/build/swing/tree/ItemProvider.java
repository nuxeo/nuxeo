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
import java.awt.Font;
import java.util.List;

import javax.swing.Icon;

import org.apache.maven.project.MavenProject;
import org.nuxeo.build.maven.graph.Edge;
import org.nuxeo.build.maven.graph.Node;
import org.nuxeo.build.swing.IconUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ItemProvider {

    public static final ItemProvider DEFAULT = new ItemProvider();

    public String[] getRoots() {
        return null;
    }

    public boolean accept(Edge edge) {
        return true;
    }

    public boolean hasChildren(Node node) {
        return true;
    }

    public Color getForegroundColor(Node node, boolean isSelected) {
        return null;
    }

    public Color getBackgroundColor(Node node, boolean isSelected) {
        return null;
    }

    public Font getFont(Node node) {
        return null;
    }

    public Icon getIcon(Node node, boolean isExpanded) {
        String type = node.getArtifact().getType();
        return IconUtils.createImageIcon(ArtifactCellRenderer.class, type+".gif");
    }

    public String getName(Node node) {
        return node.getArtifact().getArtifactId();
    }

    public String getTooltip(Node node) {
        return node.getId().substring(0, node.getId().length()-1);
    }

    public String getInfo(Node node) {
        String id = node.getId().substring(0, node.getId().length()-1);
        List<Edge> edgesIn = node.getEdgesIn();
        String scopes = null;
        if (edgesIn != null && !edgesIn.isEmpty()) {
            scopes="<br>";
            for (Edge edge : edgesIn) {
                scopes+="&nbsp;&nbsp;<dd>"+edge.src.getArtifact().getArtifactId()+": <strong><i>"+(edge.scope==null?"compile":edge.scope)+"</i></strong></dd><br>";
            }
            //scopes+="</ul>";
        } else {
            scopes = "N/A";
        }
        MavenProject pom = node.getPomIfAlreadyLoaded();
        String desc = null;
        if (pom != null) {
            desc = "<i>"+pom.getDescription()+"</i>";
            String href = pom.getUrl();
            String fileRef = null;
            try {
                fileRef=node.getFile().toURI().toURL().toExternalForm();
            } catch (Exception e) {
                fileRef = "file:/"+node.getFile().getAbsolutePath();
            }
            desc+="<p><b>Url:</b> "+href+"<br><b>File:</b> <a href=\""+fileRef+"\">"+node.getFile()+"</a></p>";
        } else {
            desc = "<i><font color=\"light-gray\">Pom not loaded. Enter the artifact to load it.</font></i>";
        }
        return "<html><body><b>Artifact: </b> "+id+"<br><b>Scopes: </b>"+scopes+"<p>"+desc+"</p></body></html>";
    }


}
