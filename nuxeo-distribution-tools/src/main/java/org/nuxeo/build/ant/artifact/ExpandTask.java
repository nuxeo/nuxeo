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
package org.nuxeo.build.ant.artifact;

import java.util.Collection;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.nuxeo.build.maven.MavenClient;
import org.nuxeo.build.maven.MavenClientFactory;
import org.nuxeo.build.maven.filter.AndFilter;
import org.nuxeo.build.maven.filter.CompositeFilter;
import org.nuxeo.build.maven.filter.NotFilter;
import org.nuxeo.build.maven.graph.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ExpandTask extends Task {

    public String key;
    public int depth = 1;
    public AndFilter filter;

    
    public void setKey(String key) {
        this.key = key;
    }
    
    public void setDepth(String depth) {
        if ("all".equals(depth)) {
            this.depth = Integer.MAX_VALUE;
        } else {
            this.depth = Integer.parseInt(depth);
        }
    }
    
    public void addExcludes(Excludes excludes) {
        if (filter == null) {
            filter = new AndFilter();
        }
        filter.addFilter(new NotFilter(excludes.filter));
    }

    public void addIncludes(Includes includes) {
        if (filter == null) {
            filter = new AndFilter();
        }
        filter.addFilter(includes.filter);
    }


    @Override
    public void execute() throws BuildException {
        Collection<Node> nodes = null;
        MavenClient maven = MavenClientFactory.getInstance();
        if (key == null) {
            nodes = maven.getGraph().getRoots();
        } else {
            nodes = maven.getGraph().find(key); 
        }
        for (Node node : nodes) {
            if (filter != null) {
                node.expand(depth, CompositeFilter.compact(filter));
            } else {
                node.expand(depth, null);
            }
        }
    }
        
}
