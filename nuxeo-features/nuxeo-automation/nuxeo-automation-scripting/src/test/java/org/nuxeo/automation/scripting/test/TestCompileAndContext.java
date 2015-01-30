package org.nuxeo.automation.scripting.test;

import java.io.InputStream;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.automation.scripting.AutomationScriptingService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;


@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@Deploy({ "org.nuxeo.ecm.automation.core"})
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy({"org.nuxeo.ecm.automation.scripting:OSGI-INF/automation-scripting-service.xml"})
public class TestCompileAndContext {

    @Test
    public void serviceShouldBeDeclared() throws Exception {     
        AutomationScriptingService ass = Framework.getService(AutomationScriptingService.class);
        Assert.assertNotNull(ass);
        
        String jsWrapper = ass.getJSWrapper();
        Assert.assertNotNull(jsWrapper);

        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByName("Nashorn");
        Assert.assertNotNull(engine);
        
        engine.eval(jsWrapper);
        
        InputStream stream = this.getClass().getResourceAsStream("/checkWrapper.js");
        Assert.assertNotNull(stream);        
        engine.eval(IOUtils.toString(stream));

        
    }
     
}
