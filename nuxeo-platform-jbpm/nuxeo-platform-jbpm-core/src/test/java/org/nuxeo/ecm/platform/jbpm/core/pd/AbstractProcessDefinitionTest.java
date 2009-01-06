package org.nuxeo.ecm.platform.jbpm.core.pd;

import java.io.InputStream;

import junit.framework.TestCase;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;

public abstract class AbstractProcessDefinitionTest extends TestCase {

    protected JbpmConfiguration configuration;

    protected ProcessDefinition pd;

    protected JbpmContext jbpmContext;

    public AbstractProcessDefinitionTest() {
        super();
    }

    public AbstractProcessDefinitionTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        InputStream isConf = getClass().getResourceAsStream(
                getConfigurationResource());
        configuration = JbpmConfiguration.parseInputStream(isConf);
        assertNotNull(configuration);
        jbpmContext = configuration.createJbpmContext();
        InputStream is = getClass().getResourceAsStream(
                getProcessDefinitionResource());
        assertNotNull(is);
        pd = ProcessDefinition.parseXmlInputStream(is);
        assertNotNull(pd);
    }

    public String getConfigurationResource() {
        return "/config/test-jbpm.cfg.xml";
    }

    public abstract String getProcessDefinitionResource();

    @Override
    protected void tearDown() throws Exception {
        jbpmContext.close();
    }
}