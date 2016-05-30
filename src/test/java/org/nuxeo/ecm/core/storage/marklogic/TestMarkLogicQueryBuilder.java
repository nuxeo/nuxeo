/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import static org.mockito.Mockito.mock;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PRIMARY_TYPE;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.IntegerLiteral;
import org.nuxeo.ecm.core.query.sql.model.LiteralList;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.storage.dbs.DBSExpressionEvaluator;
import org.nuxeo.ecm.core.storage.dbs.DBSSession;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.marklogic.client.query.RawQueryDefinition;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.schema")
@LocalDeploy("org.nuxeo.ecm.core.storage.marklogic.tests:OSGI-INF/test-types-contrib.xml")
public class TestMarkLogicQueryBuilder extends AbstractTest {

    @Test
    public void testStartsWithOperatorOnEcmPath() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));

        Expression expression = new Expression(new Reference(NXQL.ECM_PATH), Operator.STARTSWITH, new StringLiteral(
                "/default-domain"));

        // Mock session
        DBSSession session = mock(DBSSession.class, new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if ("getDocumentIdByPath".equals(invocation.getMethod().getName())) {
                    return "12345678-1234-1234-1234-123456789ABC";
                }
                return invocation.callRealMethod();
            }

        });
        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(session, selectClause, expression, null, null,
                false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/starts-with-operator-on-ecm-path.xml", query.getHandle()
                                                                                                 .toString());
    }

    @Test
    public void testStartsWithOperatorOnPath() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));

        Expression expression = new Expression(new Reference("dc:title"), Operator.STARTSWITH, new StringLiteral(
                "/default-domain"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null, false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/starts-with-operator-on-path.xml", query.getHandle().toString());
    }

    @Test
    public void testEqOperatorOnBoolean() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));

        Expression expression = new Expression(new Reference(NXQL.ECM_ISPROXY), Operator.EQ, new IntegerLiteral(0));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null, false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/eq-operator-on-boolean.xml", query.getHandle().toString());
    }

    @Test
    public void testEqOperatorOnArray() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));

        Expression expression = new Expression(new Reference("dc:contributors"), Operator.EQ, new StringLiteral("bob"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null, false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/eq-operator-on-array.xml", query.getHandle().toString());
    }

    @Test
    public void testEqOperatorOnArrayWildcard() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));

        Expression expression = new Expression(new Reference("dc:contributors/*"), Operator.EQ,
                new StringLiteral("bob"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null, false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/eq-operator-on-array.xml", query.getHandle().toString());
    }

    @Test
    public void testNoteqOperatorOnArray() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));

        Expression expression = new Expression(new Reference("dc:contributors"), Operator.NOTEQ, new StringLiteral(
                "bob"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null, false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/noteq-operator-on-array.xml", query.getHandle().toString());
    }

    @Test
    public void testNoteqOperatorOnArrayWildcard() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));

        Expression expression = new Expression(new Reference("dc:contributors/*"), Operator.NOTEQ, new StringLiteral(
                "bob"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null, false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/noteq-operator-on-array.xml", query.getHandle().toString());
    }

    @Test
    public void testLtOperator() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));

        Expression expression = new Expression(new Reference(NXQL.ECM_NAME), Operator.LT, new IntegerLiteral(10L));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null, false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/lt-operator.xml", query.getHandle().toString());
    }

    @Test
    public void testLikeOperator() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));

        Expression expression = new Expression(new Reference("dc:title"), Operator.LIKE, new StringLiteral("Docu%_"));
        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null, false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/like-operator.xml", query.getHandle().toString());
    }

    @Test
    public void testIlikeOperator() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));

        Expression expression = new Expression(new Reference("dc:title"), Operator.ILIKE, new StringLiteral("Docu%_"));
        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null, false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/ilike-operator.xml", query.getHandle().toString());
    }

    @Test
    public void testInOperator() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));

        LiteralList inPrimaryTypes = new LiteralList();
        inPrimaryTypes.add(new StringLiteral("Document"));
        inPrimaryTypes.add(new StringLiteral("Folder"));
        Expression expression = new Expression(new Reference(KEY_PRIMARY_TYPE), Operator.IN, inPrimaryTypes);
        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null, false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/in-operator.xml", query.getHandle().toString());
    }

    @Test
    public void testNotInOperatorOnArray() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));

        LiteralList inContributors = new LiteralList();
        inContributors.add(new StringLiteral("bob"));
        inContributors.add(new StringLiteral("pete"));
        Expression expression = new Expression(new Reference("dc:contributors"), Operator.NOTIN, inContributors);
        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null, false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/not-in-operator-on-array.xml", query.getHandle().toString());
    }

    @Test
    public void testIsNullOperator() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));

        Expression expression = new Expression(new Reference(NXQL.ECM_LOCK_CREATED), Operator.ISNULL, null);

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null, false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/is-null-operator.xml", query.getHandle().toString());
    }

    @Test
    public void testNotOperator() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));

        Expression expression = new Expression(new Expression(new Reference("dc:title"), Operator.EQ,
                new StringLiteral("Document 1")), Operator.NOT, null);

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null, false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/not-operator.xml", query.getHandle().toString());
    }

    @Test
    public void testNotOperatorOnComposition() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));

        Expression orExpression = new Expression( //
                new Expression(new Reference("dc:title"), Operator.EQ, new StringLiteral("Document 1")), //
                Operator.OR, //
                new Expression(new Reference("dc:description"), Operator.EQ, new StringLiteral("Description 1")));
        Expression expression = new Expression(orExpression, Operator.NOT, null);

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null, false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/not-operator-on-composition.xml", query.getHandle().toString());
    }

    @Test
    public void testWildcardReference() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));

        Expression expression = new Expression(new Reference("picture:views/*/title"), Operator.EQ, new StringLiteral(
                "Original"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null, false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/wildcard-reference.xml", query.getHandle().toString());
    }

    @Test
    public void testCorrelatedWildcardReference() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));

        Expression expression = new MultiExpression(Operator.AND, Arrays.asList( //
                new Expression(new Reference("picture:views/*1/width"), Operator.EQ, new IntegerLiteral(640)), //
                new Expression(new Reference("picture:views/*1/height"), Operator.EQ, new IntegerLiteral(480))));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null, false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/correlated-wildcard-reference.xml", query.getHandle().toString());
    }

    @Test
    public void testACPReference() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));
        selectClause.add(new Reference("ecm:acl/*1/name"));

        LiteralList inPermissions = new LiteralList();
        inPermissions.add(new StringLiteral("Read"));
        inPermissions.add(new StringLiteral("Browse"));
        Expression expression = new MultiExpression(Operator.AND, Arrays.asList( //
                new Expression(new Reference("ecm:acl/*1/permission"), Operator.IN, inPermissions), //
                new Expression(new Reference("ecm:acl/*1/grant"), Operator.EQ, new IntegerLiteral(1))));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null, false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/acp-reference.xml", query.getHandle().toString());
    }

    @Test
    public void testCoreFeatureQuery() throws Exception {
        // Init parameters
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));

        LiteralList inPrimaryTypes = new LiteralList();
        inPrimaryTypes.addAll(Stream.of("OrderedFolder", "HiddenFile", "DocWithAge", "TemplateRoot", "TestDocument2",
                "TestDocumentWithDefaultPrefetch", "SectionRoot", "Document", "Folder", "WorkspaceRoot",
                "HiddenFolder", "Section", "TestDocument", "Relation", "FolderWithSearch", "MyDocType", "Book", "Note",
                "ComplexDoc", "Domain", "File", "Workspace")
                                    .map(StringLiteral::new)
                                    .collect(Collectors.toList()));
        MultiExpression expression = new MultiExpression(Operator.AND, Collections.singletonList(new Expression(
                new Reference(KEY_PRIMARY_TYPE), Operator.IN, inPrimaryTypes)));
        // SELECT 'ecm:id' WHERE ecm:primaryType IN 'OrderedFolder', 'HiddenFile', 'DocWithAge', 'TemplateRoot',
        // 'TestDocument2', 'TestDocumentWithDefaultPrefetch', 'SectionRoot', 'Document', 'Folder', 'WorkspaceRoot',
        // 'HiddenFolder', 'Section', 'TestDocument', 'Relation', 'FolderWithSearch', 'MyDocType', 'Book', 'Note',
        // 'ComplexDoc', 'Domain', 'File', 'Workspace'
        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null, false);

        // Test
        RawQueryDefinition query = new MarkLogicQueryBuilder(CLIENT.newQueryManager(), evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled, false).buildQuery();
        assertXMLFileAgainstString("query-expression/core-feature.xml", query.getHandle().toString());
    }

}
