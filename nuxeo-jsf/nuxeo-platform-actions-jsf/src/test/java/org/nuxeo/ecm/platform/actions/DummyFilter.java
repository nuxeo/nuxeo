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
 * $Id: DummyFilter.java 21461 2007-06-26 20:42:26Z sfermigier $
 */

package org.nuxeo.ecm.platform.actions;

import org.nuxeo.ecm.platform.actions.AbstractActionFilter;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DummyFilter extends AbstractActionFilter {

    private static final long serialVersionUID = 5590825625470688497L;

    @Override
    public boolean accept(Action action, ActionContext context) {
        // Auto-generated method stub
        return true;
    }

}
