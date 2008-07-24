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
 * $Id: SampleLifeCycleManager.java 16046 2007-04-12 14:34:58Z fguillaume $
 */

package org.nuxeo.ecm.core.lifecycle;

import org.nuxeo.ecm.core.model.Document;

/**
 * @author Julien Anguenot
 *
 */
public class SampleLifeCycleManager implements LifeCycleManager {

    public String getState(Document doc) {
        return null;
    }

    public void setState(Document doc, String stateName) {
    }

    public String getPolicy(Document doc) {
        return null;
    }

    public void setPolicy(Document doc, String policy) {
    }

}
