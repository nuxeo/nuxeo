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
import org.mockito.stubbing.Answer;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.DateLiteral;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.IntegerLiteral;
import org.nuxeo.ecm.core.query.sql.model.LiteralList;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.OrderByList;
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

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.schema")
@LocalDeploy("org.nuxeo.ecm.core.storage.marklogic.test:OSGI-INF/test-types-contrib.xml")
public class TestMarkLogicQueryBuilder extends AbstractTest {

    @Test
    public void testEqOperatorOnEcmPath() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference(NXQL.ECM_PATH), Operator.EQ,
                new StringLiteral("/default-domain"));

        // Mock session
        DBSSession session = mock(DBSSession.class, (Answer) invocation -> {
            if ("getDocumentIdByPath".equals(invocation.getMethod().getName())) {
                return "12345678-1234-1234-1234-123456789ABC";
            }
            return invocation.callRealMethod();
        });
        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(session, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/eq-operator-on-ecm-path.txt", query);
    }

    @Test
    public void testStartsWithOperatorOnEcmPath() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference(NXQL.ECM_PATH), Operator.STARTSWITH,
                new StringLiteral("/default-domain"));

        // Mock session
        DBSSession session = mock(DBSSession.class, (Answer) invocation -> {
            if ("getDocumentIdByPath".equals(invocation.getMethod().getName())) {
                return "12345678-1234-1234-1234-123456789ABC";
            }
            return invocation.callRealMethod();
        });
        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(session, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/starts-with-operator-on-ecm-path.txt", query);
    }

    @Test
    public void testStartsWithOperatorOnPath() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference("dc:title"), Operator.STARTSWITH,
                new StringLiteral("/default-domain"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/starts-with-operator-on-path.txt", query);
    }

    @Test
    public void testEqOperatorOnBoolean() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference(NXQL.ECM_ISPROXY), Operator.EQ, new IntegerLiteral(0));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/eq-operator-on-boolean.txt", query);
    }

    @Test
    public void testEqOperatorOnDate() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference("dc:created"), Operator.EQ,
                new DateLiteral("2007-01-01", true));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/eq-operator-on-date.txt", query);
    }

    @Test
    public void testEqOperatorOnMixinType() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference(NXQL.ECM_MIXINTYPE), Operator.EQ,
                new StringLiteral("Aged"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/eq-operator-on-mixin-type.txt", query);
    }

    @Test
    public void testEqOperatorOnRangeElementIndex() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference(NXQL.ECM_NAME), Operator.EQ, new StringLiteral("NAME"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        MarkLogicRangeElementIndexDescriptor reid = new MarkLogicRangeElementIndexDescriptor();
        reid.element = NXQL.ECM_NAME;
        reid.type = "string";

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false,
                Collections.singletonList(reid)).buildQuery().getSearchQuery();
        assertFileAgainstString("query-expression/eq-operator-on-range-element-index.txt", query);
    }

    @Test
    public void testEqOperatorOnRangeElementIndexOnArray() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference("dc:contributors"), Operator.EQ, new StringLiteral("bob"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        MarkLogicRangeElementIndexDescriptor reid = new MarkLogicRangeElementIndexDescriptor();
        reid.element = "dc:contributors";
        reid.type = "string";

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false,
                Collections.singletonList(reid)).buildQuery().getSearchQuery();
        assertFileAgainstString("query-expression/eq-operator-on-range-element-index-on-array.txt", query);
    }

    /*
     * NXP-21397
     */
    @Test
    public void testEqOperatorWithAmpersand() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference("dc:title"), Operator.EQ, new StringLiteral("bob &"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/eq-operator-with-ampersand.txt", query);
    }

    @Test
    public void testEqOperatorOnArray() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference("dc:contributors"), Operator.EQ, new StringLiteral("bob"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/eq-operator-on-array.txt", query);
    }

    @Test
    public void testEqOperatorOnArrayWildcard() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference("dc:contributors/*"), Operator.EQ,
                new StringLiteral("bob"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/eq-operator-on-array.txt", query);
    }

    @Test
    public void testNoteqOperatorOnMixinType() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference(NXQL.ECM_MIXINTYPE), Operator.NOTEQ,
                new StringLiteral("Aged"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/noteq-operator-on-mixin-type.txt", query);
    }

    @Test
    public void testNoteqOperatorOnArray() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference("dc:contributors"), Operator.NOTEQ,
                new StringLiteral("bob"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/noteq-operator-on-array.txt", query);
    }

    @Test
    public void testNoteqOperatorOnArrayWildcard() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference("dc:contributors/*"), Operator.NOTEQ,
                new StringLiteral("bob"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/noteq-operator-on-array.txt", query);
    }

    @Test
    public void testNoteqOperatorOnRangeElementIndex() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference(NXQL.ECM_NAME), Operator.NOTEQ, new StringLiteral("NAME"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        MarkLogicRangeElementIndexDescriptor reid = new MarkLogicRangeElementIndexDescriptor();
        reid.element = NXQL.ECM_NAME;
        reid.type = "string";

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false,
                Collections.singletonList(reid)).buildQuery().getSearchQuery();
        assertFileAgainstString("query-expression/noteq-operator-on-range-element-index.txt", query);
    }

    @Test
    public void testNoteqOperatorOnRangeElementIndexOnArray() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference("dc:contributors"), Operator.NOTEQ,
                new StringLiteral("bob"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        MarkLogicRangeElementIndexDescriptor reid = new MarkLogicRangeElementIndexDescriptor();
        reid.element = "dc:contributors";
        reid.type = "string";

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false,
                Collections.singletonList(reid)).buildQuery().getSearchQuery();
        assertFileAgainstString("query-expression/noteq-operator-on-range-element-index-on-array.txt", query);
    }

    @Test
    public void testLtOperator() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference(NXQL.ECM_NAME), Operator.LT, new IntegerLiteral(10L));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/lt-operator.txt", query);
    }

    @Test
    public void testBetweenOperator() throws Exception {
        SelectClause selectClause = newSelectClause();

        LiteralList literals = new LiteralList();
        literals.add(new DateLiteral("2007-01-01", true));
        literals.add(new DateLiteral("2008-01-01", true));
        Expression expression = new Expression(new Reference("dc:created"), Operator.BETWEEN, literals);

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/between-operator.txt", query);
    }

    @Test
    public void testLikeOperator() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference("dc:title"), Operator.LIKE, new StringLiteral("Docu%_"));
        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/like-operator.txt", query);
    }

    @Test
    public void testIlikeOperator() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference("dc:title"), Operator.ILIKE, new StringLiteral("Docu%_"));
        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/ilike-operator.txt", query);
    }

    @Test
    public void testInOperator() throws Exception {
        SelectClause selectClause = newSelectClause();

        LiteralList inPrimaryTypes = new LiteralList();
        inPrimaryTypes.add(new StringLiteral("Document"));
        inPrimaryTypes.add(new StringLiteral("Folder"));
        Expression expression = new Expression(new Reference(KEY_PRIMARY_TYPE), Operator.IN, inPrimaryTypes);
        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/in-operator.txt", query);
    }

    @Test
    public void testNotInOperatorOnArray() throws Exception {
        SelectClause selectClause = newSelectClause();

        LiteralList inContributors = new LiteralList();
        inContributors.add(new StringLiteral("bob"));
        inContributors.add(new StringLiteral("pete"));
        Expression expression = new Expression(new Reference("dc:contributors"), Operator.NOTIN, inContributors);
        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/not-in-operator-on-array.txt", query);
    }

    @Test
    public void testIsNullOperator() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference(NXQL.ECM_LOCK_CREATED), Operator.ISNULL, null);

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/is-null-operator.txt", query);
    }

    @Test
    public void testNotOperator() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(
                new Expression(new Reference("dc:title"), Operator.EQ, new StringLiteral("Document 1")), Operator.NOT,
                null);

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/not-operator.txt", query);
    }

    @Test
    public void testNotOperatorOnComposition() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression orExpression = new Expression( //
                new Expression(new Reference("dc:title"), Operator.EQ, new StringLiteral("Document 1")), //
                Operator.OR, //
                new Expression(new Reference("dc:description"), Operator.EQ, new StringLiteral("Description 1")));
        Expression expression = new Expression(orExpression, Operator.NOT, null);

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/not-operator-on-composition.txt", query);
    }

    @Test
    public void testWildcardReference() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference("picture:views/*/title"), Operator.EQ,
                new StringLiteral("Original"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/wildcard-reference.txt", query);
    }

    @Test
    public void testCorrelatedWildcardReference() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new MultiExpression(Operator.AND, Arrays.asList( //
                new Expression(new Reference("picture:views/*1/width"), Operator.EQ, new IntegerLiteral(640)), //
                new Expression(new Reference("picture:views/*1/height"), Operator.EQ, new IntegerLiteral(480))));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/correlated-wildcard-reference.txt", query);
    }

    @Test
    public void testCorrelatedWildcardReferenceOnArray() throws Exception {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference("dc:subjects/*1"));

        Expression expression = new Expression(new Reference("dc:subjects/*1"), Operator.LIKE,
                new StringLiteral("abc%"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/correlated-wildcard-reference-on-array.txt", query);
    }

    @Test
    public void testACPReference() throws Exception {
        SelectClause selectClause = newSelectClause();
        selectClause.add(new Reference("ecm:acl/*1/name"));

        LiteralList inPermissions = new LiteralList();
        inPermissions.add(new StringLiteral("Read"));
        inPermissions.add(new StringLiteral("Browse"));
        Expression expression = new MultiExpression(Operator.AND,
                Arrays.asList( //
                        new Expression(new Reference("ecm:acl/*1/permission"), Operator.IN, inPermissions), //
                        new Expression(new Reference("ecm:acl/*1/grant"), Operator.EQ, new IntegerLiteral(1))));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/acp-reference.txt", query);
    }

    @Test
    public void testQueryWithPrincipals() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference("dc:title"), Operator.EQ, new StringLiteral("title"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null,
                new String[] { "Everyone", "bob" }, false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/query-with-principals.txt", query);
    }

    @Test
    public void testQueryWithSort() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference(NXQL.ECM_NAME), Operator.EQ, new StringLiteral("NAME"));

        OrderByList orderBys = new OrderByList(new OrderByExpr(new Reference("dc:title"), false));
        orderBys.add(new OrderByExpr(new Reference("dc:created"), true));
        OrderByClause orderByClause = new OrderByClause(orderBys);

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, orderByClause,
                null, false);

        MarkLogicRangeElementIndexDescriptor reid1 = new MarkLogicRangeElementIndexDescriptor();
        reid1.element = "dc:title";
        reid1.type = "string";
        MarkLogicRangeElementIndexDescriptor reid2 = new MarkLogicRangeElementIndexDescriptor();
        reid2.element = "dc:created";
        reid2.type = "dateTime";

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, orderByClause, false,
                Arrays.asList(reid1, reid2)).buildQuery().getSearchQuery();
        assertFileAgainstString("query-expression/query-with-sort.txt", query);
    }

    @Test
    public void testQueryFulltext() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference(NXQL.ECM_FULLTEXT), Operator.EQ,
                new StringLiteral("NAME"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/query-fulltext.txt", query);
    }

    @Test
    public void testQueryFulltextOr() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference(NXQL.ECM_FULLTEXT), Operator.EQ,
                new StringLiteral("pete OR world"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/query-fulltext-or.txt", query);
    }

    @Test
    public void testQueryFulltextAnd() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference(NXQL.ECM_FULLTEXT), Operator.EQ,
                new StringLiteral("world Oyster"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/query-fulltext-and.txt", query);
    }

    @Test
    public void testQueryFulltextNot() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference(NXQL.ECM_FULLTEXT), Operator.EQ,
                new StringLiteral("Kangaroo -oyster"));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/query-fulltext-not.txt", query);
    }

    @Test
    public void testQueryFulltextPhrase() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference(NXQL.ECM_FULLTEXT), Operator.EQ,
                new StringLiteral("\"Learn commerce\""));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/query-fulltext-phrase.txt", query);
    }

    @Test
    public void testQueryFulltextNotPhrase() throws Exception {
        SelectClause selectClause = newSelectClause();

        Expression expression = new Expression(new Reference(NXQL.ECM_FULLTEXT), Operator.EQ,
                new StringLiteral("Bobby -\"commerce easily\""));

        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/query-fulltext-not-phrase.txt", query);
    }

    @Test
    public void testCoreFeatureQuery() throws Exception {
        // Init parameters
        SelectClause selectClause = newSelectClause();

        LiteralList inPrimaryTypes = new LiteralList();
        inPrimaryTypes.addAll(Stream.of("OrderedFolder", "HiddenFile", "DocWithAge", "TemplateRoot", "TestDocument2",
                "TestDocumentWithDefaultPrefetch", "SectionRoot", "Document", "Folder", "WorkspaceRoot", "HiddenFolder",
                "Section", "TestDocument", "Relation", "FolderWithSearch", "MyDocType", "Book", "Note", "ComplexDoc",
                "Domain", "File", "Workspace").map(StringLiteral::new).collect(Collectors.toList()));
        MultiExpression expression = new MultiExpression(Operator.AND, Collections.singletonList(
                new Expression(new Reference(KEY_PRIMARY_TYPE), Operator.IN, inPrimaryTypes)));
        // SELECT 'ecm:id' WHERE ecm:primaryType IN 'OrderedFolder', 'HiddenFile', 'DocWithAge', 'TemplateRoot',
        // 'TestDocument2', 'TestDocumentWithDefaultPrefetch', 'SectionRoot', 'Document', 'Folder', 'WorkspaceRoot',
        // 'HiddenFolder', 'Section', 'TestDocument', 'Relation', 'FolderWithSearch', 'MyDocType', 'Book', 'Note',
        // 'ComplexDoc', 'Domain', 'File', 'Workspace'
        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, selectClause, expression, null, null,
                false);

        // Test
        String query = new MarkLogicQueryBuilder(evaluator, null, false, Collections.emptyList()).buildQuery()
                                                                                                 .getSearchQuery();
        assertFileAgainstString("query-expression/core-feature.txt", query);
    }

    private SelectClause newSelectClause() {
        SelectClause selectClause = new SelectClause();
        selectClause.add(new Reference(NXQL.ECM_UUID));
        selectClause.add(new Reference(NXQL.ECM_NAME));
        selectClause.add(new Reference(NXQL.ECM_PARENTID));
        return selectClause;
    }

}
