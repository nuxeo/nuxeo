/*
 * (C) Copyright 2014-2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.elasticsearch.test.nxql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.elasticsearch.test.ESTestUtils.assertEqualsEvenUnderWindows;
import static org.nuxeo.elasticsearch.test.ESTestUtils.getAllDocumentPrimaryTypeClauseValue;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.query.NxqlQueryConverter;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchType;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;

/**
 * Test that NXQL can be used to generate ES queries
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-hints-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-nested-contrib.xml")
public class TestNxqlConversion {

    private static final String IDX_NAME = "nxutest";

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    protected void buildDocs() throws Exception {
        for (int i = 0; i < 10; i++) {
            String name = "doc" + i;
            DocumentModel doc = session.createDocumentModel("/", name, "File");
            doc.setPropertyValue("dc:title", "File" + i);
            doc.setPropertyValue("dc:nature", "Nature" + i);
            doc.setPropertyValue("dc:rights", "Rights" + i % 2);
            doc = session.createDocument(doc);
        }
        TransactionHelper.commitOrRollbackTransaction();
        // wait for async jobs
        WorkManager wm = Framework.getService(WorkManager.class);
        assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        assertEquals(0, esa.getPendingWorkerCount());

        esa.refresh();

        TransactionHelper.startTransaction();

    }

    protected SearchResponse searchAll() {
        SearchRequest request = new SearchRequest(IDX_NAME).searchType(SearchType.DFS_QUERY_THEN_FETCH)
                                                           .source(new SearchSourceBuilder().from(0).size(60));
        return esa.getClient().search(request);
    }

    protected SearchResponse search(QueryBuilder query) {
        SearchRequest request = new SearchRequest(IDX_NAME).searchType(SearchType.DFS_QUERY_THEN_FETCH)
                                                           .source(new SearchSourceBuilder().from(0).size(60));
        request.source(new SearchSourceBuilder().query(query));
        return esa.getClient().search(request);
    }

    @Test
    public void testQuery() throws Exception {

        buildDocs();

        SearchResponse searchResponse = search(
                QueryBuilders.queryStringQuery(" dc\\:nature:\"Nature1\" AND dc\\:title:\"File1\""));
        assertEquals(1, searchResponse.getHits().getTotalHits().value);

        searchResponse = search(QueryBuilders.queryStringQuery(" dc\\:nature:\"Nature2\" AND dc\\:title:\"File1\""));
        assertEquals(0, searchResponse.getHits().getTotalHits().value);

        searchResponse = search(QueryBuilders.queryStringQuery(" NOT " + "dc\\:nature:\"Nature2\""));
        assertEquals(9, searchResponse.getHits().getTotalHits().value);

        checkNXQL("select * from Document where dc:nature='Nature2' and dc:title='File2'", 1);
        checkNXQL("select * from Document where dc:nature='Nature2' and dc:title='File1'", 0);
        checkNXQL("select * from Document where dc:nature='Nature2' or dc:title='File1'", 2);
    }

    @Test
    public void testQueryLimits() throws Exception {
        buildDocs();

        // limit does not change the total size, only the returned number of docs
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document").limit(1));
        assertEquals(10, docs.totalSize());
        assertEquals(1, docs.size());
        // default is 10
        docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document"));
        assertEquals(10, docs.totalSize());
        assertEquals(10, docs.size());
        // only interested about totalSize
        docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document").limit(0));
        assertEquals(10, docs.totalSize());
        assertEquals(0, docs.size());
    }

    @Test
    public void testQueryWithSpecialCharacters() {
        // special character should not raise syntax error
        String specialChars = "^..*+ - && || ! ( ) { } [ ] )^ \" (~ * ? : \\ / \\t$";
        checkNXQL("select * from Document where dc:title = '" + specialChars + "'", 0);
        checkNXQL("select * from Document where ecm:fulltext.dc:title = '" + specialChars + "'", 0);
        checkNXQL("select * from Document where dc:title LIKE '" + specialChars + "'", 0);
        checkNXQL("select * from Document where dc:title IN ('" + specialChars + "')", 0);
        checkNXQL("select * from Document where dc:title STARTSWITH '" + specialChars + "'", 0);
    }

    protected void checkNXQL(String nxql, int expectedNumberOfHis) {
        // System.out.println(NXQLQueryConverter.toESQueryString(nxql));
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql(nxql).limit(0));
        assertEquals(expectedNumberOfHis, docs.totalSize());
    }

    @Test
    public void testConverterSelect() {
        String es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "match_all" : {
                    "boost" : 1.0
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      }
                    ],
                    "filter" : [
                      {
                        "terms" : {
                          "ecm:primaryType" : [
                            %s
                          ],
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""".formatted(getAllDocumentPrimaryTypeClauseValue()), es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Relation").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      }
                    ],
                    "filter" : [
                      {
                        "terms" : {
                          "ecm:primaryType" : [
                            "Relation"
                          ],
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM File, Document, Relation").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "match_all" : {
                    "boost" : 1.0
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from File").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      }
                    ],
                    "filter" : [
                      {
                        "terms" : {
                          "ecm:primaryType" : [
                            "File"
                          ],
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from File, Note").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      }
                    ],
                    "filter" : [
                      {
                        "terms" : {
                          "ecm:primaryType" : [
                            "File",
                            "Note"
                          ],
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterEQUALS() {
        String es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1=1").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "f1" : {
                          "value" : "1",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 != 1").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "term" : {
                              "f1" : {
                                "value" : "1",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 <> 1").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "term" : {
                              "f1" : {
                                "value" : "1",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

    }

    @Test
    public void testConverterIN() {
        String es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 IN (1)").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "terms" : {
                        "f1" : [
                          "1"
                        ],
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 NOT IN (1, '2', 3)")
                               .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "terms" : {
                              "f1" : [
                                "1",
                                "2",
                                "3"
                              ],
                              "boost" : 1.0
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterLIKE() {
        String es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 LIKE 'foo%'")
                                      .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "match_phrase_prefix" : {
                    "f1" : {
                      "query" : "foo",
                      "slop" : 0,
                      "max_expansions" : 50,
                      "zero_terms_query" : "NONE",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 LIKE '%Foo%'").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "wildcard" : {
                    "f1" : {
                      "wildcard" : "*Foo*",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 NOT LIKE 'Foo%'")
                               .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "match_phrase_prefix" : {
                              "f1" : {
                                "query" : "Foo",
                                "slop" : 0,
                                "max_expansions" : 50,
                                "zero_terms_query" : "NONE",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        // invalid input
        NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 LIKE '(foo.*$#@^'").toString();
    }

    @Test
    public void testConverterLIKEWildcard() {
        String es;
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 LIKE '%foo'").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "wildcard" : {
                    "f1" : {
                      "wildcard" : "*foo",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 LIKE '_foo'").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "wildcard" : {
                    "f1" : {
                      "wildcard" : "?foo",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 LIKE '?foo'").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "wildcard" : {
                    "f1" : {
                      "wildcard" : "\\\\?foo",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        // * is also accepted as a wildcard (compat)
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 LIKE '*foo'").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "wildcard" : {
                    "f1" : {
                      "wildcard" : "*foo",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        // NXQL escaping
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 LIKE 'foo\\_bar\\%'")
                               .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "wildcard" : {
                    "f1" : {
                      "wildcard" : "foo_bar%",
                      "boost" : 1.0
                    }
                  }
                }""", es);
    }

    @Test
    public void testConverterILIKE() {
        String es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 ILIKE 'Foo%'")
                                      .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "match_phrase_prefix" : {
                    "f1.lowercase" : {
                      "query" : "foo",
                      "slop" : 0,
                      "max_expansions" : 50,
                      "zero_terms_query" : "NONE",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 ILIKE '%Foo%'").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "wildcard" : {
                    "f1.lowercase" : {
                      "wildcard" : "*foo*",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 NOT ILIKE 'Foo%'")
                               .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "match_phrase_prefix" : {
                              "f1.lowercase" : {
                                "query" : "foo",
                                "slop" : 0,
                                "max_expansions" : 50,
                                "zero_terms_query" : "NONE",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterIsNULL() {
        String es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 IS NULL").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "exists" : {
                              "field" : "f1",
                              "boost" : 1.0
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 IS NOT NULL").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "exists" : {
                        "field" : "f1",
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterBETWEEN() {
        String es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 BETWEEN 1 AND 2")
                                      .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "range" : {
                        "f1" : {
                          "from" : "1",
                          "to" : "2",
                          "include_lower" : true,
                          "include_upper" : true,
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 NOT BETWEEN 1 AND 2")
                               .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "range" : {
                              "f1" : {
                                "from" : "1",
                                "to" : "2",
                                "include_lower" : true,
                                "include_upper" : true,
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterSTARTSWITH() {
        String es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE ecm:path STARTSWITH '/the/path'").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must" : [
                          {
                            "term" : {
                              "ecm:path.children" : {
                                "value" : "/the/path",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "must_not" : [
                          {
                            "term" : {
                              "ecm:path" : {
                                "value" : "/the/path",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE ecm:path STARTSWITH '/'")
                               .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "exists" : {
                        "field" : "ecm:parentId",
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE ecm:path STARTSWITH '/the/path/'").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must" : [
                          {
                            "term" : {
                              "ecm:path.children" : {
                                "value" : "/the/path",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "must_not" : [
                          {
                            "term" : {
                              "ecm:path" : {
                                "value" : "/the/path",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        // for other field than ecm:path we want to match the root
        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE dc:coverage STARTSWITH 'Europe/France'").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "dc:coverage.children" : {
                          "value" : "Europe/France",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    // force NXQL.nowPlusPeriodAndDuration to return a known date/time, for tests
    @WithFrameworkProperty(name = NXQL.TEST_NXQL_NOW, value = "2001-02-03T04:05:06.007Z")
    public void testConverterNOW() {
        String es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 = NOW()").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "f1" : {
                          "value" : "2001-02-03T04:05:06.007Z",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 = NOW('-P1D')").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "f1" : {
                          "value" : "2001-02-02T04:05:06.007Z",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterAncestorId() {
        String es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE ecm:ancestorId = 'c5904f77-299a-411e-8477-81d3102a81f9'")
                                      .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "exists" : {
                        "field" : "ancestorid-without-session",
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE ecm:ancestorId != 'c5904f77-299a-411e-8477-81d3102a81f9'",
                session).toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "exists" : {
                              "field" : "ancestorid-not-found",
                              "boost" : 1.0
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterIsVersion() {
        String es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE ecm:isVersion = 1")
                                      .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "ecm:isVersion" : {
                          "value" : "true",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        String es2 = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE ecm:isCheckedInVersion = 1").toString();
        assertEqualsEvenUnderWindows(es, es2);
    }

    @Test
    public void testConverterFulltext() {
        // Given a search on a fulltext field
        String es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE ecm:fulltext='+foo -bar'").toString();
        // then we have a simple query text and not a filter
        assertEqualsEvenUnderWindows("""
                {
                  "simple_query_string" : {
                    "query" : "+foo -bar",
                    "fields" : [
                      "all_field^1.0"
                    ],
                    "analyzer" : "fulltext",
                    "flags" : -1,
                    "default_operator" : "and",
                    "analyze_wildcard" : false,
                    "auto_generate_synonyms_phrase_query" : true,
                    "fuzzy_prefix_length" : 0,
                    "fuzzy_max_expansions" : 50,
                    "fuzzy_transpositions" : true,
                    "boost" : 1.0
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE ecm:fulltext_someindex LIKE '+foo -bar'").toString();
        // don't handle nxql fulltext index definition, match to _all field
        assertEqualsEvenUnderWindows("""
                {
                  "simple_query_string" : {
                    "query" : "+foo -bar",
                    "fields" : [
                      "all_field^1.0"
                    ],
                    "analyzer" : "fulltext",
                    "flags" : -1,
                    "default_operator" : "and",
                    "analyze_wildcard" : false,
                    "auto_generate_synonyms_phrase_query" : true,
                    "fuzzy_prefix_length" : 0,
                    "fuzzy_max_expansions" : 50,
                    "fuzzy_transpositions" : true,
                    "boost" : 1.0
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE ecm:fulltext.dc:title!='+foo -bar'").toString();
        // request on field match field.fulltext
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "simple_query_string" : {
                              "query" : "+foo -bar",
                              "fields" : [
                                "dc:title.fulltext^1.0"
                              ],
                              "analyzer" : "fulltext",
                              "flags" : -1,
                              "default_operator" : "and",
                              "analyze_wildcard" : false,
                              "auto_generate_synonyms_phrase_query" : true,
                              "fuzzy_prefix_length" : 0,
                              "fuzzy_max_expansions" : 50,
                              "fuzzy_transpositions" : true,
                              "boost" : 1.0
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterFulltextElasticsearchPrefix() {
        // Given a search on a fulltext field with the
        // elasticsearch-specific prefix
        String es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE ecm:fulltext = 'es: foo bar'").toString();
        // then we have a simple query text and not a filter
        // and we have the OR operator
        assertEqualsEvenUnderWindows("""
                {
                  "simple_query_string" : {
                    "query" : "foo bar",
                    "fields" : [
                      "all_field^1.0"
                    ],
                    "analyzer" : "fulltext",
                    "flags" : -1,
                    "default_operator" : "or",
                    "analyze_wildcard" : false,
                    "auto_generate_synonyms_phrase_query" : true,
                    "fuzzy_prefix_length" : 0,
                    "fuzzy_max_expansions" : 50,
                    "fuzzy_transpositions" : true,
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-trash-service-property-override.xml")
    public void testConverterIsTrashedWithProperty() {
        String sqlNotTrashed = "SELECT * FROM Document, Relation WHERE ecm:isTrashed = 0";
        String sqlTrashed = "SELECT * FROM Document, Relation WHERE ecm:isTrashed = 1";

        String es = NxqlQueryConverter.toESQueryBuilder(sqlTrashed).toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "ecm:isTrashed" : {
                          "value" : true,
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder(sqlNotTrashed).toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "term" : {
                              "ecm:isTrashed" : {
                                "value" : true,
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-trash-service-lifecycle-override.xml")
    public void testConverterIsTrashedWithLifeCycle() {
        String sqlNotTrashed = "SELECT * FROM Document, Relation WHERE ecm:isTrashed = 0";
        String sqlTrashed = "SELECT * FROM Document, Relation WHERE ecm:isTrashed = 1";
        doTestTrashedWithLifeCycle(sqlNotTrashed, sqlTrashed);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-trash-service-lifecycle-override.xml") // for consistency
    public void testConverterLifeCycleStateDeleted() {
        String sqlNotTrashed = "SELECT * FROM Document, Relation WHERE ecm:currentLifeCycleState <> 'deleted'";
        String sqlTrashed = "SELECT * FROM Document, Relation WHERE ecm:currentLifeCycleState = 'deleted'";
        doTestTrashedWithLifeCycle(sqlNotTrashed, sqlTrashed);
    }

    protected void doTestTrashedWithLifeCycle(String sqlNotTrashed, String sqlTrashed) {
        String es = NxqlQueryConverter.toESQueryBuilder(sqlTrashed).toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "ecm:currentLifeCycleState" : {
                          "value" : "deleted",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder(sqlNotTrashed).toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "term" : {
                              "ecm:currentLifeCycleState" : {
                                "value" : "deleted",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterWhereCombination() {
        String es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1=1 AND f2=2")
                                      .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "constant_score" : {
                          "filter" : {
                            "term" : {
                              "f1" : {
                                "value" : "1",
                                "boost" : 1.0
                              }
                            }
                          },
                          "boost" : 1.0
                        }
                      },
                      {
                        "constant_score" : {
                          "filter" : {
                            "term" : {
                              "f2" : {
                                "value" : "2",
                                "boost" : 1.0
                              }
                            }
                          },
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1=1 OR f2=2").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "should" : [
                      {
                        "constant_score" : {
                          "filter" : {
                            "term" : {
                              "f1" : {
                                "value" : "1",
                                "boost" : 1.0
                              }
                            }
                          },
                          "boost" : 1.0
                        }
                      },
                      {
                        "constant_score" : {
                          "filter" : {
                            "term" : {
                              "f2" : {
                                "value" : "2",
                                "boost" : 1.0
                              }
                            }
                          },
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1=1 AND f2=2 AND f3=3")
                               .toString();
        // assertEquals("foo", es);

        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1=1 OR f2=2 OR f3=3")
                               .toString();
        // assertEquals("foo", es);

        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1=1 OR f2 LIKE 'foo' OR f3=3")
                               .toString();
        // assertEquals("foo", es);

        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE (f1=1 OR f2=2) AND f3=3")
                               .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "bool" : {
                          "should" : [
                            {
                              "constant_score" : {
                                "filter" : {
                                  "term" : {
                                    "f1" : {
                                      "value" : "1",
                                      "boost" : 1.0
                                    }
                                  }
                                },
                                "boost" : 1.0
                              }
                            },
                            {
                              "constant_score" : {
                                "filter" : {
                                  "term" : {
                                    "f2" : {
                                      "value" : "2",
                                      "boost" : 1.0
                                    }
                                  }
                                },
                                "boost" : 1.0
                              }
                            }
                          ],
                          "adjust_pure_negative" : true,
                          "boost" : 1.0
                        }
                      },
                      {
                        "constant_score" : {
                          "filter" : {
                            "term" : {
                              "f3" : {
                                "value" : "3",
                                "boost" : 1.0
                              }
                            }
                          },
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterComplex() {
        String es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE (f1 LIKE '1%' OR f2 LIKE '2%') AND f3=3").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "bool" : {
                          "should" : [
                            {
                              "match_phrase_prefix" : {
                                "f1" : {
                                  "query" : "1",
                                  "slop" : 0,
                                  "max_expansions" : 50,
                                  "zero_terms_query" : "NONE",
                                  "boost" : 1.0
                                }
                              }
                            },
                            {
                              "match_phrase_prefix" : {
                                "f2" : {
                                  "query" : "2",
                                  "slop" : 0,
                                  "max_expansions" : 50,
                                  "zero_terms_query" : "NONE",
                                  "boost" : 1.0
                                }
                              }
                            }
                          ],
                          "adjust_pure_negative" : true,
                          "boost" : 1.0
                        }
                      },
                      {
                        "constant_score" : {
                          "filter" : {
                            "term" : {
                              "f3" : {
                                "value" : "3",
                                "boost" : 1.0
                              }
                            }
                          },
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);
        // assertEquals("foo", es);
        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE ecm:fulltext='foo bar' AND ecm:path STARTSWITH '/foo/bar' OR ecm:path='/foo/'")
                               .toString();
        // assertEquals("foo", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM File, Note, Workspace WHERE f1 IN ('foo', 'bar', 'foo') AND NOT f2>=3").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "bool" : {
                          "must" : [
                            {
                              "constant_score" : {
                                "filter" : {
                                  "terms" : {
                                    "f1" : [
                                      "foo",
                                      "bar",
                                      "foo"
                                    ],
                                    "boost" : 1.0
                                  }
                                },
                                "boost" : 1.0
                              }
                            },
                            {
                              "bool" : {
                                "must_not" : [
                                  {
                                    "constant_score" : {
                                      "filter" : {
                                        "range" : {
                                          "f2" : {
                                            "from" : "3",
                                            "to" : null,
                                            "include_lower" : true,
                                            "include_upper" : true,
                                            "boost" : 1.0
                                          }
                                        }
                                      },
                                      "boost" : 1.0
                                    }
                                  }
                                ],
                                "adjust_pure_negative" : true,
                                "boost" : 1.0
                              }
                            }
                          ],
                          "adjust_pure_negative" : true,
                          "boost" : 1.0
                        }
                      }
                    ],
                    "filter" : [
                      {
                        "terms" : {
                          "ecm:primaryType" : [
                            "File",
                            "Note",
                            "Workspace"
                          ],
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConverterWhereWithoutSelect() {
        String es = NxqlQueryConverter.toESQueryBuilder("f1=1").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "f1" : {
                          "value" : "1",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder(null).toString();
        assertEqualsEvenUnderWindows("""
                {
                  "match_all" : {
                    "boost" : 1.0
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder("").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "match_all" : {
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConvertComplexProperties() {
        String es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE file:content/name = 'foo'").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "file:content.name" : {
                          "value" : "foo",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConvertComplexListProperties() {
        String es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE dc:subjects/* = 'foo'")
                                      .toString();
        // this is supported and match any element of the list
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "dc:subjects" : {
                          "value" : "foo",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE files:files/*/file/length=123")
                               .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "files:files.file.length" : {
                          "value" : "123",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

    }

    @Test
    public void testConvertComplexListPropertiesUnsupported() {
        String es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE dc:subjects/3 = 'foo'")
                                      .toString();
        // This is not supported and generate query that is going to match nothing
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "dc:subjects.3" : {
                          "value" : "foo",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE dc:subjects/*1 = 'foo'")
                               .toString();
        // This is not supported and generate query that is going to match nothing
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "dc:subjects1" : {
                          "value" : "foo",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE files:files/*1/file/length=123").toString();
        // This is not supported and generate query that is going to match nothing
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "files:files1.file.length" : {
                          "value" : "123",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

    }

    @Test
    public void testOrderByFromNxql() {
        NxQueryBuilder qb = new NxQueryBuilder(session).nxql("name='foo' ORDER BY name DESC");
        String es = qb.makeQuery().toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "name" : {
                          "value" : "foo",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
        assertEquals(1, qb.getSortInfos().size());
        assertEquals("SortInfo [sortColumn=name, sortAscending=false]", qb.getSortInfos().get(0).toString());
    }

    @Test
    public void testOrderByWithComplexProperties() {
        NxQueryBuilder qb = new NxQueryBuilder(session).nxql("SELECT * FROM File ORDER BY file:content/name DESC");
        String es = qb.makeQuery().toString();
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      }
                    ],
                    "filter" : [
                      {
                        "terms" : {
                          "ecm:primaryType" : [
                            "File"
                          ],
                          "boost" : 1.0
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);
        assertEquals(1, qb.getSortInfos().size());
        assertEquals("SortInfo [sortColumn=file:content.name, sortAscending=false]",
                qb.getSortInfos().get(0).toString());
    }

    @Test
    public void testConvertHint() {
        String es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: INDEX(some:field) */ dc:title = 'foo'").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "term" : {
                        "some:field" : {
                          "value" : "foo",
                          "boost" : 1.0
                        }
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: INDEX(some:field) */ dc:title != 'foo'").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "bool" : {
                        "must_not" : [
                          {
                            "term" : {
                              "some:field" : {
                                "value" : "foo",
                                "boost" : 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConvertHintOperator() {
        String es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: INDEX(some:field) ANALYZER(my_analyzer) OPERATOR(match) */ dc:subjects = 'foo'")
                                      .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "match" : {
                    "some:field" : {
                      "query" : "foo",
                      "operator" : "OR",
                      "analyzer" : "my_analyzer",
                      "prefix_length" : 0,
                      "max_expansions" : 50,
                      "fuzzy_transpositions" : true,
                      "lenient" : false,
                      "zero_terms_query" : "NONE",
                      "auto_generate_synonyms_phrase_query" : true,
                      "boost" : 1.0
                    }
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: OPERATOR(match_phrase) */ dc:title = 'foo'").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "match_phrase" : {
                    "dc:title" : {
                      "query" : "foo",
                      "slop" : 0,
                      "zero_terms_query" : "NONE",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: OPERATOR(match_phrase_prefix) */ dc:title = 'this is a test'")
                               .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "match_phrase_prefix" : {
                    "dc:title" : {
                      "query" : "this is a test",
                      "slop" : 0,
                      "max_expansions" : 50,
                      "zero_terms_query" : "NONE",
                      "boost" : 1.0
                    }
                  }
                }""", es);
        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: INDEX(dc:title^3,dc:description) OPERATOR(multi_match) */ dc:title = 'this is a test'")
                               .toString();
        // fields are not ordered
        assertIn(es, """
                {
                  "multi_match" : {
                    "query" : "this is a test",
                    "fields" : [
                      "dc:description^1.0",
                      "dc:title^3.0"
                    ],
                    "type" : "best_fields",
                    "operator" : "OR",
                    "slop" : 0,
                    "prefix_length" : 0,
                    "max_expansions" : 50,
                    "zero_terms_query" : "NONE",
                    "auto_generate_synonyms_phrase_query" : true,
                    "fuzzy_transpositions" : true,
                    "boost" : 1.0
                  }
                }""", """
                {
                  "multi_match" : {
                    "query" : "this is a test",
                    "fields" : [ "dc:description", "dc:title^3" ]
                  }
                }""");

        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: OPERATOR(regex) */ dc:title = 's.*y'").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "regexp" : {
                    "dc:title" : {
                      "value" : "s.*y",
                      "flags_value" : 255,
                      "max_determinized_states" : 10000,
                      "boost" : 1.0
                    }
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: OPERATOR(fuzzy) */ dc:title = 'ki'").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "fuzzy" : {
                    "dc:title" : {
                      "value" : "ki",
                      "fuzziness" : "AUTO",
                      "prefix_length" : 0,
                      "max_expansions" : 50,
                      "transpositions" : true,
                      "boost" : 1.0
                    }
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: OPERATOR(wildcard) */ dc:title = 'ki*y'").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "wildcard" : {
                    "dc:title" : {
                      "wildcard" : "ki*y",
                      "boost" : 1.0
                    }
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: OPERATOR(simple_query_string) */ dc:title = '\"fried eggs\" +(eggplant | potato) -frittata'")
                               .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "simple_query_string" : {
                    "query" : "\\"fried eggs\\" +(eggplant | potato) -frittata",
                    "fields" : [
                      "dc:title^1.0"
                    ],
                    "flags" : -1,
                    "default_operator" : "or",
                    "analyze_wildcard" : false,
                    "auto_generate_synonyms_phrase_query" : true,
                    "fuzzy_prefix_length" : 0,
                    "fuzzy_max_expansions" : 50,
                    "fuzzy_transpositions" : true,
                    "boost" : 1.0
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: INDEX(dc:title,dc:description) ANALYZER(fulltext) OPERATOR(query_string) */ dc:title = 'this AND that OR thus'")
                               .toString();
        // fields are not ordered
        assertEqualsEvenUnderWindows("""
                {
                  "query_string" : {
                    "query" : "this AND that OR thus",
                    "fields" : [
                      "dc:description^1.0",
                      "dc:title^1.0"
                    ],
                    "type" : "best_fields",
                    "default_operator" : "or",
                    "analyzer" : "fulltext",
                    "max_determinized_states" : 10000,
                    "enable_position_increments" : true,
                    "fuzziness" : "AUTO",
                    "fuzzy_prefix_length" : 0,
                    "fuzzy_max_expansions" : 50,
                    "phrase_slop" : 0,
                    "escape" : false,
                    "auto_generate_synonyms_phrase_query" : true,
                    "fuzzy_transpositions" : true,
                    "boost" : 1.0
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: OPERATOR(common) */ dc:title = 'this is bonsai cool'")
                               .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "common" : {
                    "dc:title" : {
                      "query" : "this is bonsai cool",
                      "high_freq_operator" : "OR",
                      "low_freq_operator" : "OR",
                      "cutoff_frequency" : 0.01,
                      "boost" : 1.0
                    }
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: INDEX(dc:title.fulltext^2,dc:description.fulltext) OPERATOR(more_like_this) */ ecm:uuid = '1234'")
                               .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "more_like_this" : {
                    "fields" : [
                      "dc:title.fulltext",
                      "dc:description.fulltext"
                    ],
                    "like" : [
                      {
                        "_index" : "nxutest",
                        "_id" : "1234"
                      }
                    ],
                    "max_query_terms" : 12,
                    "min_term_freq" : 1,
                    "min_doc_freq" : 3,
                    "max_doc_freq" : 2147483647,
                    "min_word_length" : 0,
                    "max_word_length" : 0,
                    "minimum_should_match" : "30%",
                    "boost_terms" : 0.0,
                    "include" : false,
                    "fail_on_unsupported_field" : true,
                    "boost" : 1.0
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: INDEX(all_field) OPERATOR(more_like_this) */ ecm:uuid IN ('1234', '4567')")
                               .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "more_like_this" : {
                        "fields" : [
                          "all_field"
                        ],
                        "like" : [
                          {
                            "_index" : "nxutest",
                            "_id" : "1234"
                          },
                          {
                            "_index" : "nxutest",
                            "_id" : "4567"
                          }
                        ],
                        "max_query_terms" : 12,
                        "min_term_freq" : 1,
                        "min_doc_freq" : 3,
                        "max_doc_freq" : 2147483647,
                        "min_word_length" : 0,
                        "max_word_length" : 0,
                        "minimum_should_match" : "30%",
                        "boost_terms" : 0.0,
                        "include" : false,
                        "fail_on_unsupported_field" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: OPERATOR(more_like_this) */ ecm:uuid IN ('1234', '4567')")
                               .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "more_like_this" : {
                        "fields" : [
                          "ecm:uuid"
                        ],
                        "like" : [
                          {
                            "_index" : "nxutest",
                            "_id" : "1234"
                          },
                          {
                            "_index" : "nxutest",
                            "_id" : "4567"
                          }
                        ],
                        "max_query_terms" : 12,
                        "min_term_freq" : 1,
                        "min_doc_freq" : 3,
                        "max_doc_freq" : 2147483647,
                        "min_word_length" : 0,
                        "max_word_length" : 0,
                        "minimum_should_match" : "30%",
                        "boost_terms" : 0.0,
                        "include" : false,
                        "fail_on_unsupported_field" : true,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: OPERATOR(testTermQuery) */ ecm:uuid = '1234'")
                               .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "term" : {
                    "ecm:uuid" : {
                      "value" : "1234",
                      "boost" : 1.0
                    }
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: OPERATOR(testBoolQuery) */ ecm:uuid = '1234'")
                               .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "bool" : {
                    "must" : [
                      {
                        "fuzzy" : {
                          "ecm:uuid" : {
                            "value" : "1234",
                            "fuzziness" : "AUTO",
                            "prefix_length" : 0,
                            "max_expansions" : 50,
                            "transpositions" : true,
                            "boost" : 1.0
                          }
                        }
                      },
                      {
                        "wildcard" : {
                          "ecm:uuid" : {
                            "wildcard" : "1234",
                            "boost" : 1.0
                          }
                        }
                      }
                    ],
                    "adjust_pure_negative" : true,
                    "boost" : 1.0
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: INDEX(files:files.file.encoding, files:files.file.digest) OPERATOR(nestedFilesQuery) */ nested:value IN ('myEncoding', 'anyDigest')")
                               .toString();
        assertEquals("""
                {
                  "constant_score" : {
                    "filter" : {
                      "nested" : {
                        "query" : {
                          "bool" : {
                            "must" : [
                              {
                                "term" : {
                                  "files:files.file.encoding" : {
                                    "value" : "myEncoding",
                                    "boost" : 1.0
                                  }
                                }
                              },
                              {
                                "term" : {
                                  "files:files.file.digest" : {
                                    "value" : "anyDigest",
                                    "boost" : 1.0
                                  }
                                }
                              }
                            ],
                            "adjust_pure_negative" : true,
                            "boost" : 1.0
                          }
                        },
                        "path" : "files:files.file",
                        "ignore_unmapped" : false,
                        "score_mode" : "none",
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);
    }

    @Test
    public void testConvertHintLike() {
        String es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: INDEX(some:field) ANALYZER(my_analyzer) */ dc:subjects LIKE 'foo*'")
                                      .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "match_phrase_prefix" : {
                    "some:field" : {
                      "query" : "foo",
                      "analyzer" : "my_analyzer",
                      "slop" : 0,
                      "max_expansions" : 50,
                      "zero_terms_query" : "NONE",
                      "boost" : 1.0
                    }
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: INDEX(some:field) */ dc:subjects LIKE '%foo%'")
                               .toString();
        assertEqualsEvenUnderWindows("""
                {
                  "wildcard" : {
                    "some:field" : {
                      "wildcard" : "*foo*",
                      "boost" : 1.0
                    }
                  }
                }""", es);

    }

    @Test
    public void testConvertHintFulltext() {
        // search on title and description, boost title
        String es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: INDEX(dc:title.fulltext^4,dc:description.fulltext) */ ecm:fulltext = 'foo'")
                                      .toString();
        // fields are not ordered
        assertIn(es, """
                {
                  "simple_query_string" : {
                    "query" : "foo",
                    "fields" : [
                      "dc:description.fulltext^1.0",
                      "dc:title.fulltext^4.0"
                    ],
                    "analyzer" : "fulltext",
                    "flags" : -1,
                    "default_operator" : "and",
                    "analyze_wildcard" : false,
                    "auto_generate_synonyms_phrase_query" : true,
                    "fuzzy_prefix_length" : 0,
                    "fuzzy_max_expansions" : 50,
                    "fuzzy_transpositions" : true,
                    "boost" : 1.0
                  }
                }""", """
                {
                  "simple_query_string" : {
                    "query" : "foo",
                    "fields" : [ "dc:description.fulltext", "dc:title.fulltext^3" ],
                    "analyzer" : "fulltext",
                    "default_operator" : "and"
                  }
                }""");
    }

    private String normalizeString(String str) {
        if (SystemUtils.IS_OS_WINDOWS) {
            str = str.trim();
            str = str.replace("\n", "");
            str = str.replace("\r", "");
        }
        return str;
    }

    protected void assertIn(String actual, String... expected) {
        actual = normalizeString(actual);
        for (String exp : expected) {
            exp = normalizeString(exp);
            if (exp.equals(actual)) {
                return;
            }
        }
        // fail
        assertEquals(expected[0], actual);
    }

    @Test
    public void testConvertHintGeo() {
        String es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: OPERATOR(geo_bounding_box) */ osm:location IN ('40.73, -74.1', '40.81, -71.12')")
                                      .toString();
        String response = """
                {
                  "constant_score" : {
                    "filter" : {
                      "geo_bounding_box" : {
                        "osm:location" : {
                          "top_left" : [
                            -74.1,
                            40.81
                          ],
                          "bottom_right" : [
                            -71.12,
                            40.73
                          ]
                        },
                        "validation_method" : "STRICT",
                        "type" : "MEMORY",
                        "ignore_unmapped" : false,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""";
        assertEqualsEvenUnderWindows(response, es);
        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: OPERATOR(geo_bounding_box) */ osm:location IN ('drj7tee', 'dr5r9y')")
                               .toString();
        // we cannot do this because lat and lon are not rounded to match the input
        // assertTruEqualsEvenUnderWindows(response, es);
        assertTrue(es.contains("geo_bounding_box"));
        assertTrue(es, es.contains("bottom_right"));

        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: OPERATOR(geo_distance) */ "
                        + "osm:location IN ('40.73, -74.1', '20km')").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "geo_distance" : {
                        "osm:location" : [
                          -74.1,
                          40.73
                        ],
                        "distance" : 20000.0,
                        "distance_type" : "arc",
                        "validation_method" : "STRICT",
                        "ignore_unmapped" : false,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE /*+ES: OPERATOR(geo_shape) */"
                + "osm:location IN ('FRA', 'type-unused', 'shapes', 'location')").toString();
        assertEqualsEvenUnderWindows("""
                {
                  "constant_score" : {
                    "filter" : {
                      "geo_shape" : {
                        "osm:location" : {
                          "indexed_shape" : {
                            "id" : "FRA",
                            "index" : "shapes",
                            "path" : "location"
                          },
                          "relation" : "within"
                        },
                        "ignore_unmapped" : false,
                        "boost" : 1.0
                      }
                    },
                    "boost" : 1.0
                  }
                }""", es);

    }

    @Test
    @Deploy("org.nuxeo.elasticsearch.core.test:max-expansions-contrib.xml")
    public void testMatchPhrasePrefixWithCustomMaxExpansions() {
        String es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document, Relation WHERE f1 LIKE 'foo%'")
                                      .toString();
        final String expected = """
                {
                  "match_phrase_prefix" : {
                    "f1" : {
                      "query" : "foo",
                      "slop" : 0,
                      "max_expansions" : 200,
                      "zero_terms_query" : "NONE",
                      "boost" : 1.0
                    }
                  }
                }""";
        assertEqualsEvenUnderWindows(expected, es);
        es = NxqlQueryConverter.toESQueryBuilder(
                "SELECT * FROM Document, Relation WHERE /*+ES: INDEX(f1) OPERATOR(match_phrase_prefix) */ ecm:fulltext.dc:title LIKE 'foo'")
                               .toString();
        assertEqualsEvenUnderWindows(expected, es);
    }

    @Test
    public void shouldFailWhenESHintOperatorIsUnknown() {
        try {
            String es = NxqlQueryConverter.toESQueryBuilder(
                    "SELECT * FROM Document, Relation WHERE /*+ES: OPERATOR(unExitingHint) */ ecm:uuid = '1234'")
                                          .toString();
            fail("Should raise an UnsupportedOperationException");
        } catch (UnsupportedOperationException uso) {
            assertEquals("Operator: unExitingHint is unknown", uso.getMessage());
        }
    }
}
