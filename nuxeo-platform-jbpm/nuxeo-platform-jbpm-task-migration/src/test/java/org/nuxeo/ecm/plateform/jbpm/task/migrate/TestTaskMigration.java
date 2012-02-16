package org.nuxeo.ecm.plateform.jbpm.task.migrate;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.JbpmTaskService;
import org.nuxeo.ecm.platform.jbpm.test.JbpmUTConstants;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;

public class TestTaskMigration extends SQLRepositoryTestCase {

    protected JbpmService jbpmService;

    protected JbpmTaskService jbpmTaskService;

    protected DocumentModel doc;

    protected NuxeoPrincipal principal;

    protected SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

    protected static final int NB_TASKS = 500;

    List<String> prefixedActorIds = new ArrayList<String>();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.platform.jbpm.api");
        deployBundle("org.nuxeo.ecm.platform.jbpm.core");

        deployBundle("org.nuxeo.ecm.platform.task.api");
        deployBundle("org.nuxeo.ecm.platform.task.core");

        deployContrib("org.nuxeo.ecm.platform.jbpm.task.migration", "OSGI-INF/task-provider-contrib.xml");

        deployBundle(JbpmUTConstants.TESTING_BUNDLE_NAME);

        jbpmService = Framework.getService(JbpmService.class);
        assertNotNull(jbpmService);
        jbpmTaskService = Framework.getService(JbpmTaskService.class);
        assertNotNull(jbpmTaskService);

        openSession();

        doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:title", "MytestDoc");
        doc = session.createDocument(doc);
        session.save();

        principal = new UserPrincipal("toto", null, false, false);

        prefixedActorIds.add("user:tit'i");

    }


    protected void createJBPMTask(String taskName) throws Exception {
        Date dueDate = sdf.parse("12/25/2012");

        Map<String, Serializable> taskVariables = new HashMap<String, Serializable>();
        taskVariables.put("v1", "value1");
        taskVariables.put("v2", "value2");

        jbpmTaskService.createTask(session, principal, doc, taskName, prefixedActorIds, true, "directive", "comment", dueDate, taskVariables);

    }


    public void testTaskMigration() throws Exception {

        TaskService taskService = Framework.getService(TaskService.class);
        assertNotNull(taskService);

        // create JBPM Tasks
        for (int i = 0; i < NB_TASKS ; i++) {
            createJBPMTask("TestTask-" + i);
        }

        // verify that the tasks are created
        List<TaskInstance> tis = jbpmService.getCurrentTaskInstances(prefixedActorIds, null);
        assertEquals(NB_TASKS, tis.size());

        DocumentModelList taskDocs = session.query("select * from TaskDoc");
        assertEquals(0, taskDocs.size());

        // call the wrapper service to triger migration
        long t0 = System.currentTimeMillis();
        List<Task> tasks = taskService.getCurrentTaskInstances( prefixedActorIds, session);
        assertEquals(NB_TASKS, tasks.size());
        long t1 = System.currentTimeMillis();
        long deltaS= (t1-t0)/1000;

        System.out.println("Migrated " + NB_TASKS + " tasks in " + deltaS + "s");
        System.out.println((NB_TASKS / deltaS) + " tasks/s");

        // check that there are no more JBPM tasks
        tis = jbpmService.getCurrentTaskInstances(prefixedActorIds, null);
        assertEquals(0, tis.size());

        // check that the Task docs were indeed created
        taskDocs = session.query("select * from TaskDoc");
        assertEquals(NB_TASKS, taskDocs.size());

        // check tasks attributes
        Task task = tasks.get(0);
        assertTrue(task.getName().startsWith("TestTask-"));
        assertEquals("directive",task.getDirective());
        assertEquals("comment",task.getComments().get(0).getText());
        assertEquals("toto",task.getInitiator());
        assertEquals("user:tit'i",task.getActors().get(0));
        assertTrue(task.getVariables().keySet().contains("v1"));
        assertTrue(task.getVariables().keySet().contains("v2"));
        assertEquals(doc.getId(), task.getTargetDocumentId());

    }

    @Override
    public void tearDown() throws Exception {
        if (session!=null) {
            CoreInstance.getInstance().close(session);
        }
        super.tearDown();
    }

}
