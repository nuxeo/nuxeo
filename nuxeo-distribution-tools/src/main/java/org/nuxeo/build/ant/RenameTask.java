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

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RenameTask extends Task {

    protected String from;
    protected String to;
    
    public void setFrom(String from) {
        this.from = from;
    }
    
    public void setTo(String to) {
        this.to = to;
    }
    
    @Override
    public void execute() throws BuildException {
        if (from.endsWith("*")) {
            String prefix = from.substring(0, from.length()-1);
            File dir = new File(from).getParentFile();
            File[] files = dir.listFiles();
            for (int k=0; k<files.length; k++) {
                File f = files[k];
                if (f.getAbsolutePath().startsWith(prefix)) {
                    f.renameTo(new File(to));
                    return;
                }
            }            
        } else {
            new File(from).renameTo(new File(to));
        }
    }
    
}
