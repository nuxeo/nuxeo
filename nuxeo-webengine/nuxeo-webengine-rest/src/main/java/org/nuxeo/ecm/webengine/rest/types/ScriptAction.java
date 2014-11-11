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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.rest.types;



import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.actions.ActionDescriptor;
import org.nuxeo.ecm.webengine.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.rest.adapters.WebObject;
import org.nuxeo.ecm.webengine.rest.scripting.ScriptFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScriptAction extends Action {

    protected ScriptFile script;


    public ScriptAction(ActionDescriptor desc, ScriptFile script) {
        super (desc);
        this.script  = script;
    }

    /**
     * @return the script.
     */
    public ScriptFile getScript() {
        return script;
    }

    public Object invoke(WebObject obj) throws WebException {
        String id = desc.getId();
        if (!desc.getGuard().check(obj)) {
            throw new WebSecurityException(id);
        }
        return obj.getContext().runScript(script, null);
    }

}
