/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ide.project.wiz;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class ProjectEntry {

    protected boolean exists;
    protected File file;
    protected IProjectDescription description;

    public ProjectEntry(File file) throws CoreException {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        this.file = file;
        IPath path = new Path(new File(file, ".project").getAbsolutePath());
        description = workspace.loadProjectDescription(path);
        IProject project = workspace.getRoot().getProject(description.getName());
        exists = project.exists();
    }

    public boolean exists() {
        return exists;
    }

    public File getFile() {
        return file;
    }

    public IProjectDescription getDescription() {
        return description;
    }

    public String getLabel() {
        return description.getName();
    }

    @Override
    public String toString() {
        return getLabel();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof ProjectEntry) {
            return ((ProjectEntry)obj).file.equals(file);
        }
        return false;
    }
}
