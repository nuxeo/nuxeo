/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.platform.picture.magick;

import java.util.List;

/**
 * Wraps the exec result of an ImageMagick command line.
 *
 * @author tiry
 * @deprecated Since 5.7.3. Not used, duplicates
 *             {@link org.nuxeo.ecm.platform.commandline.executor.api.ExecResult}
 */
@Deprecated
public class ExecResult {

    protected List<String> output;

    protected long execTime;

    public ExecResult(List<String> output, long execTime) {
        this.execTime = execTime;
        this.output = output;
    }

    public List<String> getOutput() {
        return output;
    }

    public long getExecTime() {
        return execTime;
    }

}
