/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
