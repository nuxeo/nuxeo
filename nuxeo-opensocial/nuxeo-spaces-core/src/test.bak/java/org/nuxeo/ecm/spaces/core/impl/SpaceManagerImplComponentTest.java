package org.nuxeo.ecm.spaces.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Inject;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

/**
 * Unit test classes concerning class SpaceManagerImpl as a Nuxeo Component So
 * we will tests here component comportment like contribution aspect
 *
 * @author 10044893
 *
 */
@RunWith(NuxeoRunner.class)
public class SpaceManagerImplComponentTest {

    private static SpaceManager spaceManager = null;

    private TestRuntimeHarness harness;

    @Inject
    private CoreSession session;

    private static DocumentModel myUniversRoot = null;
    private static DocumentModel myUniversDocument = null;
    private static DocumentModel mySpaceDocument = null;

    @Inject
    public SpaceManagerImplComponentTest(TestRuntimeHarness harness)
            throws Exception {
        this.harness = harness;
        harness.deployBundle("org.nuxeo.ecm.spaces.core");
        // Service init
        spaceManager = Framework.getService(SpaceManager.class);

    }

    /**
     * Build a set of documents for testing
     *
     * @throws ClientException
     */
    @Before
    public void reinitDatas() throws ClientException {
        createDomainAndSite(session);
    }

    /**
     * Remove the set of documents
     *
     * @throws ClientException
     */
    @After
    public void clearDatas() throws ClientException {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();
    }

    /**
     * Check the possibility to contribute to the 'spaceContrib' extension point
     */
    @Test
    public void spaceContribsCanBeAddedAndRemoved() throws Exception {

        // on a deux espaces
        Univers intralm = spaceManager.getUnivers("intralm", session);
        List<Space> spaces = spaceManager.getSpacesForUnivers(intralm, session);
        assertNotNull(spaces);
        assertEquals(2, spaces.size());

        // on ajoute la contribution pagePerso puis la page d'accueil => l'ordre
        // 0 de la derniere contrib implique que la page d'accueil sera en
        // premiere position
        harness.deployContrib(
                "com.leroymerlin.corp.fr.nuxeo.portal.spaces.core.test",
                "OSGI-INF/mySpace-test-contrib.xml");
        // harness.deployContrib("com.leroymerlin.corp.fr.nuxeo.portal.spaces.core.test","OSGI-INF/welcomeSpace-test-contrib.xml");

        // on doit en avoir 4 dont la premiere est "my Space"
        spaces = spaceManager.getSpacesForUnivers(intralm, session);
        assertNotNull(spaces);
        assertEquals(3, spaces.size());
        assertEquals("my Space", spaces.get(0).getTitle());
        assertEquals("space1", spaces.get(1).getTitle());
        assertEquals("space2", spaces.get(2).getTitle());

        // on supprime la contrib my Space
        harness.undeployContrib(
                "com.leroymerlin.corp.fr.nuxeo.portal.spaces.core.test",
                "OSGI-INF/mySpace-test-contrib.xml");
        spaces = spaceManager.getSpacesForUnivers(intralm, session);
        assertNotNull(spaces);
        assertEquals(2, spaces.size());
        assertEquals("space1", spaces.get(0).getTitle());
        assertEquals("space2", spaces.get(1).getTitle());



        harness.deployContrib(
            "com.leroymerlin.corp.fr.nuxeo.portal.spaces.core.test",
            "OSGI-INF/mySpace-filtered-test-contrib.xml");
        spaces = spaceManager.getSpacesForUnivers(intralm, session);
        assertNotNull(spaces);
        assertEquals(2, spaces.size());




    }

    private static final String DEFAULT_DOMAIN = "default-domain";

    /**
     * Creation of a tree of documents in the repository
     * /default-domain/workspaces/galaxy
     * /default-domain/workspaces/galaxy/intralm [Univers]
     * /default-domain/workspaces/galaxy/intralm/space1 [Space]
     * /default-domain/workspaces/galaxy/intralm/space1/g1 [Gadget]
     * /default-domain/workspaces/galaxy/intralm/space1/g2 [Gadget]
     * /default-domain/workspaces/galaxy/intralm/space2 [Space]
     * /default-domain/workspaces/galaxy/intralm/otherContent [Gadget]
     */
    private static void createDomainAndSite(CoreSession session) throws ClientException {

        DocumentModel domain = createDocument(session.getRootDocument(),
                DEFAULT_DOMAIN, "Domain",session);
        DocumentModel workspaces = createDocument(domain, "workspaces",
                "WorkspaceRoot",session);
        myUniversRoot = createDocument(workspaces, "galaxy", "Workspace",session);
        myUniversDocument = createDocument(myUniversRoot, "intralm", "Univers",session);
        mySpaceDocument = createDocument(myUniversDocument, "space1", "Space",session);
        createDocument(myUniversDocument, "space2", "Space",session);
        createDocument(myUniversDocument, "otherContent", "Gadget",session);
        createDocument(mySpaceDocument, "g1", "Gadget",session);
        createDocument(mySpaceDocument, "g2", "Gadget",session);

    }

    /**
     * Helper to create a document
     *
     * @param session
     *            The Session in which to create the document
     * @param parent
     *            The parent Document Model
     * @param id
     *            The id of the newly created doc
     * @param type
     *            The type of the created doc
     * @return the created document
     * @throws ClientException
     */
    private static DocumentModel createDocument(DocumentModel parent,
            String id, String type, CoreSession session) throws ClientException {
        DocumentModel doc = session.createDocumentModel(parent
                .getPathAsString(), id, type);
        doc = session.createDocument(doc);
        doc.setPropertyValue("dc:title", id);
        doc.setPropertyValue("dc:created", new Date());
        session.saveDocument(doc);
        session.save();
        return doc;
    }

}
