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
package org.nuxeo.build.maven;

import java.util.ArrayList;

import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RepositoryList extends ArrayList<ArtifactRepository> {

    private static final long serialVersionUID = -3936591417689223204L;

    @Override
    public boolean add(ArtifactRepository o) {
        for (int i=0,len=size(); i<len; i++) {
            if (o.getUrl().equals(get(i).getUrl())) {
                return false;
            }
        }
        return super.add(o);
    }

}
