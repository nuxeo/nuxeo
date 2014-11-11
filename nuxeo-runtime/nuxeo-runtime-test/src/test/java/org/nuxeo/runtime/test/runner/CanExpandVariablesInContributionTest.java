/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin
 */
package org.nuxeo.runtime.test.runner;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.test.InlineRef;
import org.nuxeo.runtime.test.protocols.inline.InlineURLsFeature;


@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, InlineURLsFeature.class })
public class CanExpandVariablesInContributionTest {

    RuntimeService runtime = Framework.getRuntime();

    @Before public void installDataHandler() {

    }

    @Before public void deployComponent() throws Exception {
        RuntimeContext ctx = runtime.getContext();
        System.setProperty("nuxeo.test.domain", "test");
        Framework.getProperties().setProperty("nuxeo.test.contrib", "contrib");
        InlineRef contribRef = new InlineRef("test", "<component name=\"${nuxeo.test.domain}:${nuxeo.test.contrib}\"/>");
        ctx.deploy(contribRef);
    }

    @Test public void variablesAreExpanded() throws Exception {
        ComponentInstance component = runtime.getComponentInstance("test:contrib");
        assertThat("component is installed", component, notNullValue());
    }

}
