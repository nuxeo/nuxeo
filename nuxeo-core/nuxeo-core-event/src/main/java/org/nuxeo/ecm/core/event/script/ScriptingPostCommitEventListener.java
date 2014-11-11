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
package org.nuxeo.ecm.core.event.script;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ScriptingPostCommitEventListener implements
        PostCommitEventListener {

    protected final Script script;

    public ScriptingPostCommitEventListener(Script script) {
        this.script = script;
    }

    @Override
    public void handleEvent(EventBundle bundle) throws ClientException {
        Bindings bindings = new SimpleBindings();
        bindings.put("bundle", bundle);
        try {
            script.run(bindings);
        } catch (Exception e) {
            throw ClientException.wrap(e);
        }
    }

}
