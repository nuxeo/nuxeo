/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
