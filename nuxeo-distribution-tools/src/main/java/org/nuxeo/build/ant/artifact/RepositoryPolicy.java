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

import org.apache.tools.ant.types.DataType;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RepositoryPolicy extends DataType {

    public boolean enabled;
    public String checksumPolicy;
    public String udpatePolicy;
    public String modelEncoding;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setUpdatePolicy(String udpatePolicy) {
        this.udpatePolicy = udpatePolicy;
    }

    public void setChecksumPolicy(String checksumPolicy) {
        this.checksumPolicy = checksumPolicy;
    }

    public void setModelEncoding(String modelEncoding) {
        this.modelEncoding = modelEncoding;
    }

}
