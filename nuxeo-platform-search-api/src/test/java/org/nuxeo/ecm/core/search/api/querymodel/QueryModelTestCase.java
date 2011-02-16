/*
 * (C) Copyright 2002 - 2006 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.querymodel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.DateLiteral;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModelService;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.QueryModelDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="ogrisel@nuxeo.com">Olivier Grisel</a>
 * @deprecated use content views instead
 */
@Deprecated
public class QueryModelTestCase extends RepositoryOSGITestCase {

    private static final String QM_SCHEMA = "querymodel_test";

    protected QueryModel statelessModel;

    protected QueryModel statelessModelWithSort;

    private QueryModel statelessModelWithListParam;

    private QueryModel statelessModelWithBooleanParam;

    private QueryModel statelessModelWithIntegerParam;

    private QueryModel statelessModelWithFloatParam;

    protected QueryModel statefulModel;

    protected DocumentModel documentModel;

    protected QueryModel statefulModel2;

    protected DocumentModel documentModel2;

    private QueryModel statefulModelWithFixedPart;

    private DocumentModel documentModelWithFixedPart;

    private QueryModel statefulModelWithSingleStartswith;

    private DocumentModel documentModelWithSingleStartswith;

    private QueryModel statefulModelWithMultiStartswith;

    private DocumentModel documentModelWithMultiStartswith;

    private QueryModel statelessModelWithLongPattern;

    private QueryModelService service;

    protected QueryModel statelessRedefinedModel;

    protected QueryModel statefullRedefinedModel;


    private static final String TEST_BUNDLE = "org.nuxeo.ecm.platform.search.api.tests";

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.platform.search.api"); // ourselves !
        deployContrib(TEST_BUNDLE, "querymodel-components-test-setup.xml");
        deployContrib(TEST_BUNDLE, "querymodel-components-test-setup-redefine.xml");

        openRepository();

        // GR old-style lookup kept to ensure BBB, see NXP-2161
        service = (QueryModelService) Framework.getRuntime().getComponent(
                QueryModelService.NAME);

        statefulModel = initializeStatefulQueryModel(
                service.getQueryModelDescriptor("statefulModel"));
        documentModel = statefulModel.getDocumentModel();

        statefulModel2 = initializeStatefulQueryModel(
                service.getQueryModelDescriptor("statefulModel2"));
        documentModel2 = statefulModel2.getDocumentModel();

        statefulModelWithFixedPart = initializeStatefulQueryModel(
                service.getQueryModelDescriptor("statefulModelWithFixedPart"));
        documentModelWithFixedPart = statefulModelWithFixedPart.getDocumentModel();

        statefulModelWithSingleStartswith = initializeStatefulQueryModel(
                service.getQueryModelDescriptor("statefulModelWithSingleStartswith"));
        documentModelWithSingleStartswith = statefulModelWithSingleStartswith.getDocumentModel();

        statefulModelWithMultiStartswith = initializeStatefulQueryModel(
                service.getQueryModelDescriptor("statefulModelWithMultiStartswith"));
        documentModelWithMultiStartswith = statefulModelWithMultiStartswith.getDocumentModel();

        statelessModel = new QueryModel(
                service.getQueryModelDescriptor("statelessModel"));

        statelessModelWithLongPattern = new QueryModel(
                service.getQueryModelDescriptor("statelessModelWithLongPattern"));

        statelessModelWithSort = new QueryModel(
                service.getQueryModelDescriptor("statelessModelWithSort"));

        statelessModelWithListParam = new QueryModel(
                service.getQueryModelDescriptor("statelessModelWithListParam"));

        statelessModelWithBooleanParam = new QueryModel(
                service.getQueryModelDescriptor("statelessModelWithBooleanParam"));

        statelessModelWithIntegerParam = new QueryModel(
                service.getQueryModelDescriptor("statelessModelWithIntegerParam"));

        statelessModelWithFloatParam = new QueryModel(
                service.getQueryModelDescriptor("statelessModelWithFloatParam"));

        statelessRedefinedModel = new QueryModel(
                service.getQueryModelDescriptor("statelessRedefinedModel"));

        statefullRedefinedModel = new QueryModel(
                service.getQueryModelDescriptor("statefullRedefinedModel"));

    }

    protected QueryModel initializeStatefulQueryModel(
            QueryModelDescriptor descriptor) throws ClientException {
        DocumentModel documentModel = coreSession.createDocumentModel(descriptor.getDocType());
        return new QueryModel(descriptor, documentModel);
    }

    // NXP-2161
    public void testModernLookup() throws Exception {
        assertNotNull(Framework.getService(QueryModelService.class));
    }

    public void testStatelessQueryModel() throws ClientException {
        QueryModelDescriptor descriptor = statelessModel.getDescriptor();
        assertTrue(descriptor.isStateless());
        assertFalse(descriptor.isStateful());
        assertEquals(
                "SELECT * FROM Document WHERE dc:contributors = 'Administrator' AND ecm:path STARTSWITH 'somelocation'",
                descriptor.getQuery(new Object[] { "Administrator",
                        "somelocation" }));

        try {
            descriptor.getQuery(documentModel);
            fail("Should have raised an exception since stateless models need a parameters array");
        } catch (ClientException e) {
        }
    }

    public void testStatelessQueryModelWithLiteral() throws ClientException {
        QueryModelDescriptor descriptor = statelessModel.getDescriptor();
        assertTrue(descriptor.isStateless());
        assertFalse(descriptor.isStateful());
        // GR stupid query (we don't care)
        assertEquals(
                "SELECT * FROM Document WHERE dc:contributors = DATE '2008-06-25' AND ecm:path STARTSWITH 'somelocation'",
                descriptor.getQuery(new Object[] { new DateLiteral("2008-06-25", true),
                        "somelocation" }));
    }

    // NXP-2059
    public void testStatelessQueryModelWithSortAndNullParams()
            throws ClientException {
        QueryModelDescriptor descriptor = statelessModelWithSort.getDescriptor();
        assertTrue(descriptor.isStateless());
        assertFalse(descriptor.isStateful());
        String query = "SELECT * FROM Document WHERE ecm:primaryType in ('File') ORDER BY dc:modified";
        SortInfo sortInfo = descriptor.getDefaultSortInfo(documentModel);
        assertEquals(query, descriptor.getQuery(new Object[0], sortInfo));
        assertEquals(query, descriptor.getQuery((Object[]) null, sortInfo));
    }

    // NXP-2195
    public void testStatelessModelWithListParam() throws ClientException {
        QueryModelDescriptor descriptor = statelessModelWithListParam.getDescriptor();
        assertTrue(descriptor.isStateless());
        assertFalse(descriptor.isStateful());
        String query = "SELECT * FROM Document WHERE ecm:primaryType IN ('File', 'Folder')";

        // test String[] param
        String[] typeArray = { "File", "Folder" };
        assertEquals(query, descriptor.getQuery(new Object[] { typeArray }));

        // test List<String> param
        List<String> typeList = Arrays.asList(typeArray);
        assertEquals(query, descriptor.getQuery(new Object[] { typeList }));

        // test empty param
        query = "SELECT * FROM Document WHERE ecm:primaryType IN ()";
        typeArray = new String[0];
        assertEquals(query, descriptor.getQuery(new Object[] { typeArray }));
        typeList = Arrays.asList(typeArray);
        assertEquals(query, descriptor.getQuery(new Object[] { typeList }));
    }

    // NXP-2418
    public void testStratelessModelWithBooleanParam() throws ClientException {
        QueryModelDescriptor descriptor = statelessModelWithBooleanParam.getDescriptor();
        assertTrue(descriptor.isStateless());
        assertFalse(descriptor.isStateful());

        // test with false boolean
        String query = "SELECT * FROM Document WHERE ecm:booleanParameter = 0";
        assertEquals(query, descriptor.getQuery(new Object[] { false }));

        // test with true boolean
        query = "SELECT * FROM Document WHERE ecm:booleanParameter = 1";
        assertEquals(query, descriptor.getQuery(new Object[] { true }));
    }

    // NXP-2418
    public void testStratelessModelWithIntegerParam() throws ClientException {
        QueryModelDescriptor descriptor = statelessModelWithIntegerParam.getDescriptor();
        assertTrue(descriptor.isStateless());
        assertFalse(descriptor.isStateful());

        String query = "SELECT * FROM Document WHERE ecm:integerParameter = 234";
        assertEquals(query, descriptor.getQuery(new Object[] { 234 }));
    }

    // NXP-2418
    public void testStratelessModelWithFloatParam() throws ClientException {
        QueryModelDescriptor descriptor = statelessModelWithFloatParam.getDescriptor();
        assertTrue(descriptor.isStateless());
        assertFalse(descriptor.isStateful());

        String query = "SELECT * FROM Document WHERE ecm:floatParameter = 123.4";
        assertEquals(query, descriptor.getQuery(new Object[] { 123.4f }));
    }

    public void testStatelessQueryModelWithLongPattern() throws ClientException {
        QueryModelDescriptor descriptor = statelessModelWithLongPattern.getDescriptor();
        assertTrue(descriptor.isStateless());
        assertFalse(descriptor.isStateful());
        assertEquals(
                "SELECT * FROM Document WHERE dc:contributors = 'Administrator' AND ecm:path STARTSWITH 'somelocation' AND ecm:title = 'the title' dc:created >= DATE '2006-10-12' AND ecm:isProxy = 0",
                descriptor.getQuery(new Object[] { "Administrator",
                        "somelocation" }));
        try {
            descriptor.getQuery(documentModel);
            fail("Should have raised an exception since stateless models need a parameters array");
        } catch (ClientException e) {
        }
    }


    public void testSerialization() throws Exception {
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        ObjectOutputStream outStream = new ObjectOutputStream(byteOutStream);
        outStream.writeObject(statefulModel);
        ByteArrayInputStream byteInStream = new ByteArrayInputStream(
                byteOutStream.toByteArray());
        ObjectInputStream inStream = new ObjectInputStream(byteInStream);
        QueryModel sm = (QueryModel) inStream.readObject();
        assertEquals(sm.getDescriptor(), statefulModel.getDescriptor());
    }

    public void testStatefulQueryModel() throws ClientException {
        QueryModelDescriptor descriptor = statefulModel.getDescriptor();
        assertFalse(descriptor.isStateless());
        assertTrue(descriptor.isStateful());

        // by default the model is empty, thus the computed query should be very
        // simple:
        assertEquals("SELECT * FROM Document",
                descriptor.getQuery(documentModel));

        // adding a value to the text field
        documentModel.setProperty(QM_SCHEMA, "textfield", "some text");

        assertEquals(
                "SELECT * FROM Document WHERE textparameter = 'some text'",
                descriptor.getQuery(documentModel));

        // adding a value to the int field
        documentModel.setProperty(QM_SCHEMA, "intfield", 3);

        assertEquals(
                "SELECT * FROM Document WHERE textparameter = 'some text' AND intparameter < 3",
                descriptor.getQuery(documentModel));

        // same with a long
        documentModel.setProperty(QM_SCHEMA, "intfield", 123456789123L);

        assertEquals(
                "SELECT * FROM Document WHERE textparameter = 'some text' "
                        + "AND intparameter < 123456789123",
                descriptor.getQuery(documentModel));
        // get back to simpler value
        documentModel.setProperty(QM_SCHEMA, "intfield", 3);

        // setting a null or an empty value removes the corresponding predicate
        // from the where clause:

        documentModel.setProperty(QM_SCHEMA, "textfield", "");

        assertEquals("SELECT * FROM Document WHERE intparameter < 3",
                descriptor.getQuery(documentModel));

        documentModel.setProperty(QM_SCHEMA, "intfield", null);

        assertEquals("SELECT * FROM Document",
                descriptor.getQuery(documentModel));

        // partially filled BETWEEN predicates are transformed into
        // corresponding unary ones

        GregorianCalendar gc = new GregorianCalendar(2006, 9, 12);
        documentModel.setProperty(QM_SCHEMA, "date_min", gc.getTime());

        assertEquals(
                "SELECT * FROM Document WHERE dc:created >= DATE '2006-10-12'",
                descriptor.getQuery(documentModel));

        // adding the second value make the BETWEEN predicate works as expected

        gc = new GregorianCalendar(2006, 11, 15);
        documentModel.setProperty(QM_SCHEMA, "date_max", gc.getTime());

        assertEquals(
                "SELECT * FROM Document WHERE dc:created BETWEEN DATE '2006-10-12' AND DATE '2006-12-15'",
                descriptor.getQuery(documentModel));

        documentModel.setProperty(QM_SCHEMA, "date_min", null);

        assertEquals(
                "SELECT * FROM Document WHERE dc:created <= DATE '2006-12-15'",
                descriptor.getQuery(documentModel));

        // adding ordering info to the model
        // documentModel.setProperty("querymodel_common", "sortColumn",
        // "dc:modified");

        SortInfo sortInfo = new SortInfo("dc:modified", true);
        assertEquals(
                "SELECT * FROM Document WHERE dc:created <= DATE '2006-12-15' ORDER BY dc:modified",
                descriptor.getQuery(documentModel, sortInfo));

        sortInfo = new SortInfo("dc:modified", false);
        // documentModel.setProperty("querymodel_common", "sortAscending",
        // false);

        assertEquals(
                "SELECT * FROM Document WHERE dc:created <= DATE '2006-12-15' ORDER BY dc:modified DESC",
                descriptor.getQuery(documentModel, sortInfo));

        // providing only the direction is not enough to display the ordering
        // clauseOR
        documentModel.setProperty("querymodel_common", "sortColumn", "");

        assertEquals(
                "SELECT * FROM Document WHERE dc:created <= DATE '2006-12-15'",
                descriptor.getQuery(documentModel));

        // testing the use of the wrong method for a stateful qmodel
        try {
            descriptor.getQuery(new Object[] {});
            fail("Should have raised an exception since statelful models do not work with external parameters");
        } catch (ClientException e) {
        }
    }

    public void testStatefulQMBoolean() throws ClientException {
        // In NXQL/SQL there is no true boolean. One uses 0/1 instead
        QueryModelDescriptor descriptor = statefulModel.getDescriptor();
        documentModel.setProperty(QM_SCHEMA, "boolfield", true);
        assertEquals("SELECT * FROM Document WHERE boolparameter = 1",
                descriptor.getQuery(documentModel));

        documentModel.setProperty(QM_SCHEMA, "boolfield", false);
        assertEquals("SELECT * FROM Document WHERE boolparameter = 0",
                descriptor.getQuery(documentModel));
    }

    public void testStatefulQueryModelFullText() throws ClientException {
        QueryModelDescriptor descriptor = statefulModel.getDescriptor();
        assertFalse(descriptor.isStateless());
        assertTrue(descriptor.isStateful());
        // adding a value to the fulltext field

        documentModel.setProperty(QM_SCHEMA, "fulltext_all", "some text");
        assertEquals(
                "SELECT * FROM Document WHERE ecm:fulltext = '+some +text'",
                descriptor.getQuery(documentModel));
        documentModel.setProperty(QM_SCHEMA, "fulltext_all", null);

        documentModel.setProperty(QM_SCHEMA, "fulltext_all", "can't");
        assertEquals(
                "SELECT * FROM Document WHERE ecm:fulltext = '+can\\'t'",
                descriptor.getQuery(documentModel));
        documentModel.setProperty(QM_SCHEMA, "fulltext_all", null);

        // Tests the minimal lucene escaper and its registration
        documentModel.setProperty(QM_SCHEMA, "fulltext_all", "can\"t");
        assertEquals(
                "SELECT * FROM Document WHERE ecm:fulltext = '+can\\\"t'",
                descriptor.getQuery(documentModel));
        documentModel.setProperty(QM_SCHEMA, "fulltext_all", null);

        documentModel.setProperty(QM_SCHEMA, "fulltext_all", "NXP-1576");
        assertEquals(
                "SELECT * FROM Document WHERE ecm:fulltext = '+NXP\\-1576'",
                descriptor.getQuery(documentModel));

        documentModel.setProperty(QM_SCHEMA, "fulltext_all", null);

        documentModel.setProperty(QM_SCHEMA, "fulltext_my_index", "bla bla bla");
        assertEquals(
                "SELECT * FROM Document WHERE ecm:fulltext_my_index = '+bla +bla +bla'",
                descriptor.getQuery(documentModel));
    }

    public void testStatefulQueryModelWithListField() throws ClientException {
        QueryModelDescriptor descriptor = statefulModel2.getDescriptor();
        assertFalse(statefulModel2.getDescriptor().isStateless());
        assertTrue(statefulModel2.getDescriptor().isStateful());

        // by default the model is empty except for default values
        assertEquals(
                "SELECT * FROM Document WHERE dc:creator = 'default1' OR dc:creator = 'default2'",
                descriptor.getQuery(documentModel2));

        // by adding a single element to the list of options, the predicate is
        // serialized as a '=' predicate:
        documentModel2.setProperty(QM_SCHEMA, "listfield",
                new String[] { "Pedro" });

        assertEquals("SELECT * FROM Document WHERE dc:creator = 'Pedro'",
                descriptor.getQuery(documentModel2));

        // with several options the predicate is serialized as expected
        documentModel2.setProperty(QM_SCHEMA, "listfield", new String[] {
                "Pedro", "Piotr", "Pierre" });

        assertEquals(
                "SELECT * FROM Document WHERE dc:creator = 'Pedro' OR dc:creator = 'Piotr' OR dc:creator = 'Pierre'",
                descriptor.getQuery(documentModel2));

        // parentheses don't get dropped if our list field is not alone
        documentModel2.setProperty(QM_SCHEMA, "intfield", 4L);
        assertEquals(
                "SELECT * FROM Document WHERE (dc:creator = 'Pedro' OR dc:creator = 'Piotr' OR dc:creator = 'Pierre') AND someint = 4",
                descriptor.getQuery(documentModel2));
        documentModel2.setProperty(QM_SCHEMA, "intfield", null);

        // add a value for textparameter
        documentModel2.setProperty(QM_SCHEMA, "textfield", "foo");
        assertEquals(
                "SELECT * FROM Document WHERE (dc:creator = 'Pedro' OR dc:creator = 'Piotr' OR dc:creator = 'Pierre') AND textparameter ILIKE 'foo'",
                descriptor.getQuery(documentModel2));
        documentModel2.setProperty(QM_SCHEMA, "textfield", null);

        // an empty array of options is ignored as if the field was left null
        documentModel2.setProperty(QM_SCHEMA, "listfield", new String[] {});

        assertEquals("SELECT * FROM Document",
                descriptor.getQuery(documentModel2));
    }

    public void testStatefulQueryModelWithFixedPart() throws ClientException {
        QueryModelDescriptor descriptor = statefulModelWithFixedPart.getDescriptor();
        assertFalse(statefulModelWithFixedPart.getDescriptor().isStateless());
        assertTrue(statefulModelWithFixedPart.getDescriptor().isStateful());

        // by default the model is empty except for default values
        assertEquals("SELECT * FROM Document WHERE "
                + "sp:specific LIKE 'foo' OR ecm:isProxy = 1",
                descriptor.getQuery(documentModelWithFixedPart));

        documentModelWithFixedPart.setProperty(QM_SCHEMA, "intfield", 1);
        // now with a parameter from the document model
        assertEquals("SELECT * FROM Document WHERE " + "intparameter = 1 AND "
                + "(sp:specific LIKE 'foo' OR ecm:isProxy = 1)",
                descriptor.getQuery(documentModelWithFixedPart));
    }

    public void testSaveQM() throws ClientException {
        documentModel.setProperty(QM_SCHEMA, "intfield", 4L);
        // Attach and save
        documentModel.setPathInfo("/", "model");
        coreSession.createDocument(documentModel);
        coreSession.save();

        // Refetch
        documentModel = coreSession.getDocument(new PathRef("/model"));
        assertEquals(4L, documentModel.getProperty(QM_SCHEMA, "intfield"));
        QueryModelDescriptor descriptor = statefulModel.getDescriptor();
        statefulModel = new QueryModel(descriptor, documentModel);
        assertEquals("SELECT * FROM Document WHERE intparameter < 4",
                descriptor.getQuery(documentModel));
    }

    public void testStatefulWithSubClause() throws ClientException {
        QueryModelDescriptor descriptor = service.getQueryModelDescriptor("statefulModelWithSubClause");
        QueryModel qm = initializeStatefulQueryModel(descriptor);
        DocumentModel doc = qm.getDocumentModel();
        doc.setProperty(QM_SCHEMA, "subclause", "foo < 'bar' or NOT x = 1");
        assertEquals("SELECT * FROM Document WHERE foo < 'bar' or NOT x = 1",
                descriptor.getQuery(doc));
        // this is valid NXQL
        SQLQueryParser.parse(descriptor.getQuery(doc));
        doc.setProperty(QM_SCHEMA, "intfield", 3L);
        assertEquals(
                "SELECT * FROM Document WHERE intparameter = 3 AND (foo < 'bar' or NOT x = 1)",
                descriptor.getQuery(doc));
        doc.setProperty(QM_SCHEMA, "textfield", "zork");
        assertEquals(
                "SELECT * FROM Document WHERE intparameter = 3 AND (foo < 'bar' or NOT x = 1) AND textparameter = 'zork'",
                descriptor.getQuery(doc));
    }

    public void testStatefulWithSubClauseNull() throws ClientException {
        QueryModelDescriptor descriptor = service.getQueryModelDescriptor("statefulModelWithSubClause");
        QueryModel qm = initializeStatefulQueryModel(descriptor);
        DocumentModel doc = qm.getDocumentModel();
        doc.setProperty(QM_SCHEMA, "subclause", null);
        assertEquals("SELECT * FROM Document",
                descriptor.getQuery(doc));
        // this is valid NXQL
        SQLQueryParser.parse(descriptor.getQuery(doc));
        doc.setProperty(QM_SCHEMA, "intfield", 3L);
        assertEquals(
                "SELECT * FROM Document WHERE intparameter = 3",
                descriptor.getQuery(doc));
        doc.setProperty(QM_SCHEMA, "textfield", "zork");
        assertEquals(
                "SELECT * FROM Document WHERE intparameter = 3 AND textparameter = 'zork'",
                descriptor.getQuery(doc));
    }

    public void testStatefulModelWithSingleStartswith() throws ClientException {
        QueryModelDescriptor descriptor = statefulModelWithSingleStartswith.getDescriptor();
        assertFalse(statefulModelWithSingleStartswith.getDescriptor().isStateless());
        assertTrue(statefulModelWithSingleStartswith.getDescriptor().isStateful());

        documentModelWithSingleStartswith.setProperty(QM_SCHEMA, "textfield", "/to/toto");
        assertEquals(
                "SELECT * FROM Document WHERE ecm:path STARTSWITH '/to/toto'",
                descriptor.getQuery(documentModelWithSingleStartswith));
    }

    public void testStatefulModelWithMultiStartswith() throws ClientException {
        QueryModelDescriptor descriptor = statefulModelWithMultiStartswith.getDescriptor();
        assertFalse(statefulModelWithMultiStartswith.getDescriptor().isStateless());
        assertTrue(statefulModelWithMultiStartswith.getDescriptor().isStateful());

        documentModelWithMultiStartswith.setProperty(QM_SCHEMA, "listfield", new String[] { "/to/toto", "/tu/tutu" });
        assertEquals(
                "SELECT * FROM Document WHERE ecm:path STARTSWITH '/to/toto' OR ecm:path STARTSWITH '/tu/tutu'",
                descriptor.getQuery(documentModelWithMultiStartswith));
    }

    public void testStatelessRedefinedQueryModel() throws ClientException {

        QueryModelDescriptor descriptor = statelessRedefinedModel.getDescriptor();

        assertTrue(descriptor.isStateless());
        assertFalse(descriptor.isStateful());

        String query = "SELECT * FROM Document ORDER BY dc:modified";

        SortInfo sortInfo = descriptor.getDefaultSortInfo(documentModel);
        assertEquals(query, descriptor.getQuery(new Object[0], sortInfo));

    }

    public void testStatefullRedefinedQueryModel() throws ClientException {

        QueryModelDescriptor descriptor = statefullRedefinedModel.getDescriptor();
        assertFalse(statefullRedefinedModel.getDescriptor().isStateless());
        assertTrue(statefullRedefinedModel.getDescriptor().isStateful());

        documentModel2.setProperty(QM_SCHEMA, "listfield", new String[] {
                "Pedro", "Piotr", "Pierre" });

        documentModel2.setProperty(QM_SCHEMA, "intfield", 4L);
        documentModel2.setProperty(QM_SCHEMA, "textfield", "foo");
        assertEquals(
                "SELECT * FROM Document WHERE (dc:creator = 'Pedro' OR dc:creator = 'Piotr' OR dc:creator = 'Pierre') "
                + "AND someint = 4 AND textparameter ILIKE 'foo'",
                descriptor.getQuery(documentModel2));

    }


}
