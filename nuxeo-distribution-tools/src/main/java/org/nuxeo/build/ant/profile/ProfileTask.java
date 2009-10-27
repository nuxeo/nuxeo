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
package org.nuxeo.build.ant.profile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Sequential;
import org.nuxeo.build.maven.MavenClientFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ProfileTask extends Sequential {

    public String name;
    public String activate;
    public String group;
    public String defaultProfile;


    public void setName(String name) {
        if (activate != null) {
            throw new  BuildException("Name and activate properties are exclusive. You cannot specify both.");
        }
        if (group != null) {
            throw new  BuildException("group and name properties are exclusive. You cannot specify both.");
        }
        if (defaultProfile != null) {
            throw new  BuildException("Default and name properties are exclusive. You cannot specify both.");
        }
        this.name = name;
    }

    public void setActivate(String activate) {
        if (name != null) {
            throw new  BuildException("Name and activate properties are exclusive. You cannot specify both.");
        }
        if (group != null) {
            throw new  BuildException("group and activate properties are exclusive. You cannot specify both.");
        }
        if (defaultProfile != null) {
            throw new  BuildException("Default and activate properties are exclusive. You cannot specify both.");
        }
        this.activate = activate;
    }

    public void setGroup(String group) {
        if (name != null) {
            throw new  BuildException("Group and name properties are exclusive. You cannot specify both.");
        }
        if (activate != null) {
            throw new  BuildException("Group and activate properties are exclusive. You cannot specify both.");
        }
        this.group = group;
    }

    public void setDefault(String defaultProfile) {
        if (name != null) {
            throw new  BuildException("default and name properties are exclusive. You cannot specify both.");
        }
        if (activate != null) {
            throw new  BuildException("default and activate properties are exclusive. You cannot specify both.");
        }
        this.defaultProfile = defaultProfile;
    }

    @Override
    public void addTask(Task nestedTask) {
        if (activate != null) {
            throw new  BuildException("Cannot use nested elements when specifying activate attribute.");
        }
        super.addTask(nestedTask);
    }

    @Override
    public void execute() throws BuildException {
        AntProfileManager mgr = MavenClientFactory.getInstance().getAntProfileManager();
        if (mgr.isProfileActive(name)) {
            super.execute();
        } else if (group != null) {
            String[] profiles = group.split("\\s*,\\s*");
            MavenClientFactory.getInstance().getAntProfileManager().addGroup(profiles, activate);
        } else if (activate != null) {
            mgr.activateProfiles(activate);
        }
    }

}
