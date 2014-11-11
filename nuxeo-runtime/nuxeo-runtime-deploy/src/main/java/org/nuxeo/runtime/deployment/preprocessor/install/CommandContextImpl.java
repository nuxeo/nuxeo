/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.deployment.preprocessor.install;

import java.io.File;
import java.util.HashMap;

import org.nuxeo.common.utils.TextTemplate;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class CommandContextImpl extends HashMap<String, String> implements CommandContext {

    private static final long serialVersionUID = 3020720283855802969L;

    protected final File baseDir;

    public CommandContextImpl(File baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public File getBaseDir() {
        return baseDir;
    }

    @Override
    public String expandVars(String text) {
        return new TextTemplate(this).process(text);
    }

}
