package org.nuxeo.elasticsearch.test.commands;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.elasticsearch.commands.IndexingCommand.Type;
import org.nuxeo.elasticsearch.commands.IndexingCommands;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

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
        cmds.add(Type.INSERT, true, false);
        cmds.add(Type.UPDATE_SECURITY, false, false);
        Assert.assertEquals(1, cmds.getCommands().size());

        String json = cmds.toJSON();
        IndexingCommands cmds2 = IndexingCommands.fromJSON(json);
        Assert.assertEquals(1, cmds2.getCommands().size());
        Assert.assertEquals(cmds.toJSON(), cmds2.toJSON());
    }

    @Test
    public void shouldMarshalToJSONAndBackNoMerge() throws Exception {

        DocumentModel doc1 = session.createDocumentModel("/", "testDoc1", "File");
        doc1.setPropertyValue("dc:title", "Test1");
        doc1 = session.createDocument(doc1);

        IndexingCommands cmds = new IndexingCommands(doc1);
        cmds.add(Type.UPDATE, true, false);
        cmds.add(Type.UPDATE_SECURITY, false, true);
        Assert.assertEquals(2, cmds.getCommands().size());

        String json = cmds.toJSON();
        IndexingCommands cmds2 = IndexingCommands.fromJSON(json);
        Assert.assertEquals(2, cmds2.getCommands().size());
        Assert.assertEquals(cmds.toJSON(), cmds2.toJSON());
    }

}
