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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core.search.client.opensearch1.aggregate;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_CARDINALITY;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_DATE_HISTOGRAM;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_DATE_RANGE;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_HISTOGRAM;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_RANGE;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_SIGNIFICANT_TERMS;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_TERMS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.schema.CoreSchemaFeature;
import org.nuxeo.ecm.core.search.SearchIndex;
import org.nuxeo.ecm.core.search.SearchQuery;
import org.nuxeo.ecm.core.search.client.opensearch1.OpenSearchQueryTransformer;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.AggregateRangeDateDefinition;
import org.nuxeo.ecm.platform.query.api.AggregateRangeDefinition;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.core.AggregateCardinality;
import org.nuxeo.ecm.platform.query.core.AggregateDateHistogram;
import org.nuxeo.ecm.platform.query.core.AggregateDateRange;
import org.nuxeo.ecm.platform.query.core.AggregateDescriptor;
import org.nuxeo.ecm.platform.query.core.AggregateHistogram;
import org.nuxeo.ecm.platform.query.core.AggregateRange;
import org.nuxeo.ecm.platform.query.core.AggregateRangeDateDescriptor;
import org.nuxeo.ecm.platform.query.core.AggregateRangeDescriptor;
import org.nuxeo.ecm.platform.query.core.AggregateSignificantTerm;
import org.nuxeo.ecm.platform.query.core.AggregateTerm;
import org.nuxeo.ecm.platform.query.core.FieldDescriptor;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.common.Strings;

@RunWith(FeaturesRunner.class)
@Features(CoreSchemaFeature.class)
@Deploy("org.nuxeo.ecm.core:OSGI-INF/SecurityService.xml")
@Deploy("org.nuxeo.ecm.core.query.test:OSGI-INF/test-aggregate-schemas-contrib.xml")
public class TestOpenSearchAggregates {

    protected static final SearchIndex SEARCH_INDEX = SearchIndex.of("test", "opensearch", "enhanced");

    protected static final OpenSearchQueryTransformer TRANSFORMER = new OpenSearchQueryTransformer(
            Map.of("enhanced", "nxutest"), Map.of());

    protected static final Function<SearchQuery, String> BUILDER = TRANSFORMER.andThen(
            SearchRequest::source).andThen(source -> Strings.toString(source, true, true));

    @Test
    public void testCustomAggregates() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setId("cardinal");
        aggDef.setType(AGG_CARDINALITY);
        aggDef.setDocumentField("dc:source");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "source_agg"));
        aggDef.setProperty("minDocCount", "10");
        aggDef.setProperty("size", "10");

        String request = BUILDER.apply(newSearchQuery(new AggregateCardinality(aggDef, null)));
        assertEqualsEvenUnderWindows("""
                {
                  "from" : 0,
                  "size" : 10,
                  "query" : {
                    "match_all" : {
                      "boost" : 1.0
                    }
                  },
                  "_source" : {
                    "includes" : [
                      "ecm:uuid",
                      "ecm:repository"
                    ],
                    "excludes" : [ ]
                  },
                  "track_total_hits" : 2147483647,
                  "aggregations" : {
                    "cardinal_filter" : {
                      "filter" : {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      },
                      "aggregations" : {
                        "cardinal" : {
                          "cardinality" : {
                            "field" : "dc:source"
                          }
                        }
                      }
                    }
                  }
                }""", request);
    }

    @Test
    public void testAggregateTermsQuery() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType(AGG_TYPE_TERMS);
        aggDef.setId("source");
        aggDef.setDocumentField("dc:source");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "source_agg"));
        aggDef.setProperty("minDocCount", "10");
        aggDef.setProperty("size", "10");
        aggDef.setProperty("exclude", "foo*");
        aggDef.setProperty("include", "bar*");
        aggDef.setProperty("order", "count asc");

        String request = BUILDER.apply(newSearchQuery(new AggregateTerm(aggDef, null)));
        assertEqualsEvenUnderWindows("""
                {
                  "from" : 0,
                  "size" : 10,
                  "query" : {
                    "match_all" : {
                      "boost" : 1.0
                    }
                  },
                  "_source" : {
                    "includes" : [
                      "ecm:uuid",
                      "ecm:repository"
                    ],
                    "excludes" : [ ]
                  },
                  "track_total_hits" : 2147483647,
                  "aggregations" : {
                    "source_filter" : {
                      "filter" : {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      },
                      "aggregations" : {
                        "source" : {
                          "terms" : {
                            "field" : "dc:source",
                            "size" : 10,
                            "min_doc_count" : 10,
                            "shard_min_doc_count" : 0,
                            "show_term_doc_count_error" : false,
                            "order" : [
                              {
                                "_count" : "asc"
                              },
                              {
                                "_key" : "asc"
                              }
                            ],
                            "include" : "bar*",
                            "exclude" : "foo*"
                          }
                        }
                      }
                    }
                  }
                }""", request);
    }

    @Test
    public void testAggregateBooleanTermsQuery() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType(AGG_TYPE_TERMS);
        aggDef.setId("trashed");
        aggDef.setDocumentField("ecm:isTrashed");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "trashed_agg"));
        aggDef.setProperty("size", "2");

        String request = BUILDER.apply(newSearchQuery(new AggregateTerm(aggDef, null)));
        assertEqualsEvenUnderWindows("""
                {
                  "from" : 0,
                  "size" : 10,
                  "query" : {
                    "match_all" : {
                      "boost" : 1.0
                    }
                  },
                  "_source" : {
                    "includes" : [
                      "ecm:uuid",
                      "ecm:repository"
                    ],
                    "excludes" : [ ]
                  },
                  "track_total_hits" : 2147483647,
                  "aggregations" : {
                    "trashed_filter" : {
                      "filter" : {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      },
                      "aggregations" : {
                        "trashed" : {
                          "terms" : {
                            "field" : "ecm:isTrashed",
                            "size" : 2,
                            "min_doc_count" : 1,
                            "shard_min_doc_count" : 0,
                            "show_term_doc_count_error" : false,
                            "order" : [
                              {
                                "_count" : "desc"
                              },
                              {
                                "_key" : "asc"
                              }
                            ]
                          }
                        }
                      }
                    }
                  }
                }""", request);
    }

    @Test
    public void testAggregateTermsFulltextQuery() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType(AGG_TYPE_TERMS);
        aggDef.setId("fulltext");
        aggDef.setDocumentField("ecm:fulltext");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "fulltext_agg"));

        String request = BUILDER.apply(newSearchQuery(new AggregateTerm(aggDef, null)));
        assertEqualsEvenUnderWindows("""
                {
                  "from" : 0,
                  "size" : 10,
                  "query" : {
                    "match_all" : {
                      "boost" : 1.0
                    }
                  },
                  "_source" : {
                    "includes" : [
                      "ecm:uuid",
                      "ecm:repository"
                    ],
                    "excludes" : [ ]
                  },
                  "track_total_hits" : 2147483647,
                  "aggregations" : {
                    "fulltext_filter" : {
                      "filter" : {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      },
                      "aggregations" : {
                        "fulltext" : {
                          "terms" : {
                            "field" : "all_field",
                            "size" : 10,
                            "min_doc_count" : 1,
                            "shard_min_doc_count" : 0,
                            "show_term_doc_count_error" : false,
                            "order" : [
                              {
                                "_count" : "desc"
                              },
                              {
                                "_key" : "asc"
                              }
                            ]
                          }
                        }
                      }
                    }
                  }
                }""", request);
    }

    @Test
    public void testAggregateSignificantTermsQuery() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType(AGG_TYPE_SIGNIFICANT_TERMS);
        aggDef.setId("source");
        aggDef.setDocumentField("dc:source");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "source_agg"));
        aggDef.setProperty("minDocCount", "10");

        String request = BUILDER.apply(newSearchQuery(new AggregateSignificantTerm(aggDef, null)));
        assertEqualsEvenUnderWindows("""
                {
                  "from" : 0,
                  "size" : 10,
                  "query" : {
                    "match_all" : {
                      "boost" : 1.0
                    }
                  },
                  "_source" : {
                    "includes" : [
                      "ecm:uuid",
                      "ecm:repository"
                    ],
                    "excludes" : [ ]
                  },
                  "track_total_hits" : 2147483647,
                  "aggregations" : {
                    "source_filter" : {
                      "filter" : {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      },
                      "aggregations" : {
                        "source" : {
                          "significant_terms" : {
                            "field" : "dc:source",
                            "size" : 10,
                            "min_doc_count" : 10,
                            "shard_min_doc_count" : 0,
                            "jlh" : { }
                          }
                        }
                      }
                    }
                  }
                }""", request);
    }

    @Test
    public void testAggregateRangeQuery() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType(AGG_TYPE_RANGE);
        aggDef.setId("source");
        aggDef.setDocumentField("common:size");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "size_agg"));
        List<AggregateRangeDefinition> ranges = new ArrayList<>();
        ranges.add(new AggregateRangeDescriptor("small", null, 2048.0));
        ranges.add(new AggregateRangeDescriptor("medium", 2048.0, 6144.0));
        ranges.add(new AggregateRangeDescriptor("big", 6144.0, null));
        aggDef.setRanges(ranges);

        String request = BUILDER.apply(newSearchQuery(new AggregateRange(aggDef, null)));
        assertEqualsEvenUnderWindows("""
                {
                  "from" : 0,
                  "size" : 10,
                  "query" : {
                    "match_all" : {
                      "boost" : 1.0
                    }
                  },
                  "_source" : {
                    "includes" : [
                      "ecm:uuid",
                      "ecm:repository"
                    ],
                    "excludes" : [ ]
                  },
                  "track_total_hits" : 2147483647,
                  "aggregations" : {
                    "source_filter" : {
                      "filter" : {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      },
                      "aggregations" : {
                        "source" : {
                          "range" : {
                            "field" : "common:size",
                            "ranges" : [
                              {
                                "key" : "small",
                                "to" : 2048.0
                              },
                              {
                                "key" : "medium",
                                "from" : 2048.0,
                                "to" : 6144.0
                              },
                              {
                                "key" : "big",
                                "from" : 6144.0
                              }
                            ],
                            "keyed" : false
                          }
                        }
                      }
                    }
                  }
                }""", request);
    }

    @Test
    public void testAggregateRangeDateQuery() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType(AGG_TYPE_DATE_RANGE);
        aggDef.setId("created");
        aggDef.setDocumentField("dc:created");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "created_agg"));
        List<AggregateRangeDateDefinition> ranges = new ArrayList<>();
        ranges.add(new AggregateRangeDateDescriptor("10monthAgo", null, "now-10M/M"));
        ranges.add(new AggregateRangeDateDescriptor("1monthAgo", "now-10M/M", "now-1M/M"));
        ranges.add(new AggregateRangeDateDescriptor("thisMonth", "now-1M/M", null));
        aggDef.setDateRanges(ranges);

        String request = BUILDER.apply(newSearchQuery(new AggregateDateRange(aggDef, null)));
        assertEqualsEvenUnderWindows("""
                {
                  "from" : 0,
                  "size" : 10,
                  "query" : {
                    "match_all" : {
                      "boost" : 1.0
                    }
                  },
                  "_source" : {
                    "includes" : [
                      "ecm:uuid",
                      "ecm:repository"
                    ],
                    "excludes" : [ ]
                  },
                  "track_total_hits" : 2147483647,
                  "aggregations" : {
                    "created_filter" : {
                      "filter" : {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      },
                      "aggregations" : {
                        "created" : {
                          "date_range" : {
                            "field" : "dc:created",
                            "ranges" : [
                              {
                                "key" : "10monthAgo",
                                "to" : "now-10M/M"
                              },
                              {
                                "key" : "1monthAgo",
                                "from" : "now-10M/M",
                                "to" : "now-1M/M"
                              },
                              {
                                "key" : "thisMonth",
                                "from" : "now-1M/M"
                              }
                            ],
                            "keyed" : false
                          }
                        }
                      }
                    }
                  }
                }""", request);
    }

    @Test
    public void testAggregateHistogramQuery() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType(AGG_TYPE_HISTOGRAM);
        aggDef.setId("size");
        aggDef.setDocumentField("common:size");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "size_agg"));
        aggDef.setProperty("interval", "1024");
        aggDef.setProperty("extendedBoundsMin", "0");
        aggDef.setProperty("extendedBoundsMax", "10240");

        String request = BUILDER.apply(newSearchQuery(new AggregateHistogram(aggDef, null)));
        assertEqualsEvenUnderWindows("""
                {
                  "from" : 0,
                  "size" : 10,
                  "query" : {
                    "match_all" : {
                      "boost" : 1.0
                    }
                  },
                  "_source" : {
                    "includes" : [
                      "ecm:uuid",
                      "ecm:repository"
                    ],
                    "excludes" : [ ]
                  },
                  "track_total_hits" : 2147483647,
                  "aggregations" : {
                    "size_filter" : {
                      "filter" : {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      },
                      "aggregations" : {
                        "size" : {
                          "histogram" : {
                            "field" : "common:size",
                            "interval" : 1024.0,
                            "offset" : 0.0,
                            "order" : {
                              "_key" : "asc"
                            },
                            "keyed" : false,
                            "min_doc_count" : 0,
                            "extended_bounds" : {
                              "min" : 0.0,
                              "max" : 10240.0
                            }
                          }
                        }
                      }
                    }
                  }
                }""", request);
    }

    @Test
    public void testAggregateHistogramQueryWithSelection() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType(AGG_TYPE_HISTOGRAM);
        aggDef.setId("size");
        aggDef.setDocumentField("common:size");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "size_agg"));
        aggDef.setProperty("interval", "1024");
        aggDef.setProperty("extendedBoundsMin", "0");
        aggDef.setProperty("extendedBoundsMax", "10240");
        var agg = new AggregateHistogram(aggDef, null);
        agg.setSelection(List.of("2048.0"));

        String request = BUILDER.apply(newSearchQuery(agg));
        assertEqualsEvenUnderWindows("""
                {
                  "from" : 0,
                  "size" : 10,
                  "query" : {
                    "match_all" : {
                      "boost" : 1.0
                    }
                  },
                  "post_filter" : {
                    "bool" : {
                      "must" : [
                        {
                          "bool" : {
                            "should" : [
                              {
                                "range" : {
                                  "common:size" : {
                                    "from" : 2048,
                                    "to" : 3072,
                                    "include_lower" : true,
                                    "include_upper" : false,
                                    "boost" : 1.0
                                  }
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
                  },
                  "_source" : {
                    "includes" : [
                      "ecm:uuid",
                      "ecm:repository"
                    ],
                    "excludes" : [ ]
                  },
                  "track_total_hits" : 2147483647,
                  "aggregations" : {
                    "size_filter" : {
                      "filter" : {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      },
                      "aggregations" : {
                        "size" : {
                          "histogram" : {
                            "field" : "common:size",
                            "interval" : 1024.0,
                            "offset" : 0.0,
                            "order" : {
                              "_key" : "asc"
                            },
                            "keyed" : false,
                            "min_doc_count" : 0,
                            "extended_bounds" : {
                              "min" : 0.0,
                              "max" : 10240.0
                            }
                          }
                        }
                      }
                    }
                  }
                }""", request);
    }

    @Test
    public void testAggregateDateHistogramQuery() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType(AGG_TYPE_DATE_HISTOGRAM);
        aggDef.setId("created");
        aggDef.setDocumentField("dc:created");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "created_agg"));
        aggDef.setProperty("interval", "month");
        aggDef.setProperty("format", "yyy-MM");
        aggDef.setProperty("timeZone", "UTC");
        aggDef.setProperty("order", "count desc");
        aggDef.setProperty("minDocCounts", "5");
        var agg = new AggregateDateHistogram(aggDef, null);
        agg.setSelection(List.of("2016-08"));

        String request = BUILDER.apply(newSearchQuery(agg));
        assertEqualsEvenUnderWindows("""
                {
                  "from" : 0,
                  "size" : 10,
                  "query" : {
                    "match_all" : {
                      "boost" : 1.0
                    }
                  },
                  "post_filter" : {
                    "bool" : {
                      "must" : [
                        {
                          "bool" : {
                            "should" : [
                              {
                                "range" : {
                                  "dc:created" : {
                                    "from" : 1470009600000,
                                    "to" : 1472688000000,
                                    "include_lower" : true,
                                    "include_upper" : false,
                                    "format" : "epoch_millis",
                                    "boost" : 1.0
                                  }
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
                  },
                  "_source" : {
                    "includes" : [
                      "ecm:uuid",
                      "ecm:repository"
                    ],
                    "excludes" : [ ]
                  },
                  "track_total_hits" : 2147483647,
                  "aggregations" : {
                    "created_filter" : {
                      "filter" : {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      },
                      "aggregations" : {
                        "created" : {
                          "date_histogram" : {
                            "field" : "dc:created",
                            "format" : "yyy-MM",
                            "time_zone" : "UTC",
                            "calendar_interval" : "month",
                            "offset" : 0,
                            "order" : [
                              {
                                "_count" : "desc"
                              },
                              {
                                "_key" : "asc"
                              }
                            ],
                            "keyed" : false,
                            "min_doc_count" : 0
                          }
                        }
                      }
                    }
                  }
                }""", request);
    }

    @Test
    public void testAggregateDateHistogramQueryWithoutTimeZone() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType(AGG_TYPE_DATE_HISTOGRAM);
        aggDef.setId("created");
        aggDef.setDocumentField("dc:created");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "created_agg"));
        aggDef.setProperty("interval", "month");
        aggDef.setProperty("order", "count desc");
        aggDef.setProperty("minDocCounts", "5");

        String request = BUILDER.apply(newSearchQuery(new AggregateDateHistogram(aggDef, null)));
        // The request will use the JVM time_zone
        assertEqualsEvenUnderWindows("""
                {
                  "from" : 0,
                  "size" : 10,
                  "query" : {
                    "match_all" : {
                      "boost" : 1.0
                    }
                  },
                  "_source" : {
                    "includes" : [
                      "ecm:uuid",
                      "ecm:repository"
                    ],
                    "excludes" : [ ]
                  },
                  "track_total_hits" : 2147483647,
                  "aggregations" : {
                    "created_filter" : {
                      "filter" : {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      },
                      "aggregations" : {
                        "created" : {
                          "date_histogram" : {
                            "field" : "dc:created",
                            "time_zone" : "%s",
                            "calendar_interval" : "month",
                            "offset" : 0,
                            "order" : [
                              {
                                "_count" : "desc"
                              },
                              {
                                "_key" : "asc"
                              }
                            ],
                            "keyed" : false,
                            "min_doc_count" : 0
                          }
                        }
                      }
                    }
                  }
                }""".formatted(TimeZone.getDefault().getID()), request);
    }

    @Test
    public void testAggregateMultiAggregatesQuery() {

        AggregateDefinition aggDef1 = new AggregateDescriptor();
        aggDef1.setType(AGG_TYPE_TERMS);
        aggDef1.setId("source");
        aggDef1.setDocumentField("dc:source");
        aggDef1.setSearchField(new FieldDescriptor("advanced_search", "source_agg"));

        AggregateDefinition aggDef2 = new AggregateDescriptor();
        aggDef2.setType(AGG_TYPE_TERMS);
        aggDef2.setId("nature");
        aggDef2.setDocumentField("dc:nature");
        aggDef2.setSearchField(new FieldDescriptor("advanced_search", "nature_agg"));
        aggDef2.setProperty("size", "10");

        DocumentModel model = SimpleDocumentModel.ofType("AdvancedSearch");
        String[] sources = { "foo", "bar" };
        model.setProperty("advanced_search", "source_agg", sources);
        // String[] natures = { "foobar" };
        // model.setProperty("advanced_search", "nature_agg", natures);

        String request = BUILDER.apply(
                newSearchQuery(new AggregateTerm(aggDef1, model), new AggregateTerm(aggDef2, model)));
        assertEqualsEvenUnderWindows("""
                {
                  "from" : 0,
                  "size" : 10,
                  "query" : {
                    "match_all" : {
                      "boost" : 1.0
                    }
                  },
                  "post_filter" : {
                    "bool" : {
                      "must" : [
                        {
                          "terms" : {
                            "dc:source" : [
                              "foo",
                              "bar"
                            ],
                            "boost" : 1.0
                          }
                        }
                      ],
                      "adjust_pure_negative" : true,
                      "boost" : 1.0
                    }
                  },
                  "_source" : {
                    "includes" : [
                      "ecm:uuid",
                      "ecm:repository"
                    ],
                    "excludes" : [ ]
                  },
                  "track_total_hits" : 2147483647,
                  "aggregations" : {
                    "source_filter" : {
                      "filter" : {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      },
                      "aggregations" : {
                        "source" : {
                          "terms" : {
                            "field" : "dc:source",
                            "size" : 10,
                            "min_doc_count" : 1,
                            "shard_min_doc_count" : 0,
                            "show_term_doc_count_error" : false,
                            "order" : [
                              {
                                "_count" : "desc"
                              },
                              {
                                "_key" : "asc"
                              }
                            ]
                          }
                        }
                      }
                    },
                    "nature_filter" : {
                      "filter" : {
                        "bool" : {
                          "must" : [
                            {
                              "terms" : {
                                "dc:source" : [
                                  "foo",
                                  "bar"
                                ],
                                "boost" : 1.0
                              }
                            }
                          ],
                          "adjust_pure_negative" : true,
                          "boost" : 1.0
                        }
                      },
                      "aggregations" : {
                        "nature" : {
                          "terms" : {
                            "field" : "dc:nature",
                            "size" : 10,
                            "min_doc_count" : 1,
                            "shard_min_doc_count" : 0,
                            "show_term_doc_count_error" : false,
                            "order" : [
                              {
                                "_count" : "desc"
                              },
                              {
                                "_key" : "asc"
                              }
                            ]
                          }
                        }
                      }
                    }
                  }
                }""", request);
    }

    @Test
    public void testAggregateOnComplexTypeQuery() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType(AGG_TYPE_SIGNIFICANT_TERMS);
        aggDef.setId("source");
        aggDef.setDocumentField("prefix:foo/bar");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "source_agg"));
        aggDef.setProperty("minDocCount", "10");

        String request = BUILDER.apply(newSearchQuery(new AggregateSignificantTerm(aggDef, null)));
        assertEqualsEvenUnderWindows("""
                {
                  "from" : 0,
                  "size" : 10,
                  "query" : {
                    "match_all" : {
                      "boost" : 1.0
                    }
                  },
                  "_source" : {
                    "includes" : [
                      "ecm:uuid",
                      "ecm:repository"
                    ],
                    "excludes" : [ ]
                  },
                  "track_total_hits" : 2147483647,
                  "aggregations" : {
                    "source_filter" : {
                      "filter" : {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      },
                      "aggregations" : {
                        "source" : {
                          "significant_terms" : {
                            "field" : "prefix:foo.bar",
                            "size" : 10,
                            "min_doc_count" : 10,
                            "shard_min_doc_count" : 0,
                            "jlh" : { }
                          }
                        }
                      }
                    }
                  }
                }""", request);
    }

    @Test
    @WithFrameworkProperty(name = "nuxeo.search.indexing.relation.enabled", value = "true")
    public void testAggregateWithRelationEnabled() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setId("source");
        aggDef.setType(AGG_TYPE_TERMS);
        aggDef.setDocumentField("dc:source");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "source_agg"));
        aggDef.setProperty("minDocCount", "10");
        aggDef.setProperty("size", "10");
        // With relation enabled, "SELECT * FROM Document, Relation" is used and should produce match_all
        String request = BUILDER.apply(SearchQuery.builder("SELECT * FROM Document, Relation")
                                                  .searchIndex(SEARCH_INDEX)
                                                  .addAggregate(new AggregateTerm(aggDef, null))
                                                  .build());
        // Verify the query part is match_all (no type filter)
        assertEqualsEvenUnderWindows("""
                {
                  "from" : 0,
                  "size" : 10,
                  "query" : {
                    "match_all" : {
                      "boost" : 1.0
                    }
                  },
                  "_source" : {
                    "includes" : [
                      "ecm:uuid",
                      "ecm:repository"
                    ],
                    "excludes" : [ ]
                  },
                  "track_total_hits" : 2147483647,
                  "aggregations" : {
                    "source_filter" : {
                      "filter" : {
                        "match_all" : {
                          "boost" : 1.0
                        }
                      },
                      "aggregations" : {
                        "source" : {
                          "terms" : {
                            "field" : "dc:source",
                            "size" : 10,
                            "min_doc_count" : 10,
                            "shard_min_doc_count" : 0,
                            "show_term_doc_count_error" : false,
                            "order" : [
                              {
                                "_count" : "desc"
                              },
                              {
                                "_key" : "asc"
                              }
                            ]
                          }
                        }
                      }
                    }
                  }
                }""", request);
    }

    @SafeVarargs
    protected final SearchQuery newSearchQuery(Aggregate<? extends Bucket> aggregate,
            Aggregate<? extends Bucket>... aggregates) {
        return SearchQuery.builder("SELECT * FROM Document")
                          .searchIndex(SEARCH_INDEX)
                          .addAggregate(aggregate)
                          .addAggregates(List.of(aggregates))
                          .build();
    }

    protected void assertEqualsEvenUnderWindows(String expected, String actual) {
        if (SystemUtils.IS_OS_WINDOWS) {
            // make tests pass under Windows
            expected = expected.trim();
            expected = expected.replace("\n", "");
            expected = expected.replace("\r", "");
            actual = actual.trim();
            actual = actual.replace("\n", "");
            actual = actual.replace("\r", "");
        }
        assertEquals(expected, actual);
    }
}
