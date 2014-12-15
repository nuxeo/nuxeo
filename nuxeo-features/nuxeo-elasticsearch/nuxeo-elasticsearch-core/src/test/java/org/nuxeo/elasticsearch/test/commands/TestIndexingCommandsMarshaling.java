package org.nuxeo.elasticsearch.test.commands;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.commands.IndexingCommands;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import org.nuxeo.elasticsearch.commands.IndexingCommand.Name;
import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy({ "org.nuxeo.elasticsearch.core:disable-listener-contrib.xml",
        "org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml" })
public class TestIndexingCommandsMarshaling {

    @Inject
    CoreSession session;

    @Test
    public void shouldMarshalToJSONAndBack() throws Exception {

        DocumentModel doc1 = session.createDocumentModel("/", "testDoc1", "File");
        doc1.setPropertyValue("dc:title", "Test1");
        doc1 = session.createDocument(doc1);

        IndexingCommands cmds = new IndexingCommands(doc1);
        IndexingCommand cmd1 = cmds.add(Name.INSERT, true, false);
        IndexingCommand cmd2 = cmds.add(Name.UPDATE_SECURITY, false, false);
        Assert.assertNotNull(cmd1);
        // command 2 should be ignored
        Assert.assertNull(cmd2);

        String json = cmds.toJSON();

        IndexingCommands cmds2 = IndexingCommands.fromJSON(session, json);
        Assert.assertEquals(1, cmds.getCommands().size());
        Assert.assertEquals(cmd1.getId(), cmds2.getCommands().get(0).getId());

        Assert.assertEquals(json, cmds2.toJSON());
    }

    @Test
    public void shouldMarshalToJSONAndBackNoMerge() throws Exception {

        DocumentModel doc1 = session.createDocumentModel("/", "testDoc1", "File");
        doc1.setPropertyValue("dc:title", "Test1");
        doc1 = session.createDocument(doc1);

        IndexingCommands cmds = new IndexingCommands(doc1);
        IndexingCommand cmd1 = cmds.add(Name.UPDATE, true, false);
        IndexingCommand cmd2 = cmds.add(Name.UPDATE_SECURITY, false, true);
        Assert.assertNotNull(cmd1);
        Assert.assertNotNull(cmd2);

        String json = cmds.toJSON();

        IndexingCommands cmds2 = IndexingCommands.fromJSON(session, json);
        Assert.assertEquals(2, cmds.getCommands().size());
        Assert.assertEquals(cmd1.getId(), cmds2.getCommands().get(0).getId());
        Assert.assertEquals(cmd2.getId(), cmds2.getCommands().get(1).getId());

        Assert.assertEquals(json, cmds2.toJSON());
    }

}
