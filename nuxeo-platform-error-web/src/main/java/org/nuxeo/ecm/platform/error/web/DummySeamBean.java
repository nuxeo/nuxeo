/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
