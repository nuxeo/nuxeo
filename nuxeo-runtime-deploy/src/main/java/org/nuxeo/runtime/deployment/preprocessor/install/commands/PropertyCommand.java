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

package org.nuxeo.runtime.deployment.preprocessor.install.commands;

import java.io.IOException;

import org.nuxeo.runtime.deployment.preprocessor.install.Command;
import org.nuxeo.runtime.deployment.preprocessor.install.CommandContext;

/**
 * Sets a property that is valid along the current preprocessing context.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PropertyCommand implements Command {

    protected final String name;
    protected final String value;

    public PropertyCommand(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public void exec(CommandContext ctx) throws IOException {
        if (value == null || value.length() == 0) {
            ctx.remove(name);
        } else {
            ctx.put(name, value);
        }
    }

    @Override
    public String toString() {
        return "set " + name + " = \"" + value + '"';
    }

    @Override
    public String toString(CommandContext ctx) {
        return toString();
    }

}
