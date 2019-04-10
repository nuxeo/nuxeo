/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.platform.error.web;

import static org.jboss.seam.ScopeType.PAGE;

import java.io.Serializable;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 * Dummy seam bean to test page sope.
 *
 * @since 5.9.2
 */
@Name("dummySeamBean")
@Scope(PAGE)
public class DummySeamBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean dummyBoolean;

    public void setDummyBoolean(final boolean dummyBoolean) {
        this.dummyBoolean = dummyBoolean;
    }

    public boolean getDummyBoolean() {
        return dummyBoolean;
    }

    public void dummyAction() {
        dummyBoolean = !dummyBoolean;
    }
}
