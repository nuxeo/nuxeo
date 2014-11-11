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
package org.nuxeo.build.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Sequential;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class IfTask extends Sequential {

    protected String isSet;
    protected String isNotSet;

    public void setIsSet(String property) {
        this.isSet = property;
    }

    public void setIsNotSet(String property) {
        this.isNotSet = property;
    }

    @Override
    public void execute() throws BuildException {
        boolean test = false;
        if (isSet != null) {
            test = getProject().getProperty(isSet) != null;
        } else if (isNotSet != null) {
            test = getProject().getProperty(isNotSet) == null;
        }
        if (test) {
            super.execute();
        }
    }

}
