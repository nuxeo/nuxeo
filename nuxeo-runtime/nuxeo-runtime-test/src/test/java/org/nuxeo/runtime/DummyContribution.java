/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.runtime;

import org.nuxeo.common.xmap.annotation.XContext;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject(value="printer")
public class DummyContribution {

    @XNode("message")
    String message;

    @XNode("comp1")
    String comp1;

    @XNode("comp2")
    String comp2;

    @XNode("xp")
    String xp;

    @XNode("xt1")
    String xt1;

    @XNode("xt2")
    String xt2;

    @XContext("comp1")
    void injectDefaultComp1(String value) {
        comp1 = value;
    }

    @XContext("comp2")
    void injectDefaultComp2(String value) {
        comp2 = value;
    }

    @XContext("xp")
    void injectDefaultXp(String value) {
        xp = value;
    }

    @XContext("xt")
    void injectDefaultXt(String value) {
        xt1 = value;
    }

    @XContext("xt")
    void injectOverridenDefaultXt(String value) {
        xt2 = value;
    }

    public DummyContribution() {
    }

    public DummyContribution(String message) {
        this.message = message;
    }

}
