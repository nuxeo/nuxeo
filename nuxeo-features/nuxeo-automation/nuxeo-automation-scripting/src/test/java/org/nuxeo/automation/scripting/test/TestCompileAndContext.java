/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 */
package org.nuxeo.automation.scripting.test;

import java.io.InputStream;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import static junit.framework.TestCase.assertNotNull;

/**
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@Deploy({ "org.nuxeo.ecm.automation.core" })
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy({ "org.nuxeo.ecm.automation.scripting:OSGI-INF/automation-scripting-service.xml" })
public class TestCompileAndContext {

    @Test
    public void serviceShouldBeDeclared() throws Exception {
        AutomationScriptingService automationScriptingService = Framework.getService(AutomationScriptingService.class);
        assertNotNull(automationScriptingService);

        String jsWrapper = automationScriptingService.getJSWrapper();
        assertNotNull(jsWrapper);

        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByName("Nashorn");
        assertNotNull(engine);

        engine.eval(jsWrapper);

        InputStream stream = this.getClass().getResourceAsStream("/checkWrapper.js");
        assertNotNull(stream);
        engine.eval(IOUtils.toString(stream));

    }

}
