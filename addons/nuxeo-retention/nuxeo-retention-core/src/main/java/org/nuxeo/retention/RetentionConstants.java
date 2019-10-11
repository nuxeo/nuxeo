/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume RENARD
 */
package org.nuxeo.retention;

import org.apache.commons.lang3.time.FastDateFormat;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.retention.adapters.RetentionRule;

/**
 * @since 11.1
 */
public class RetentionConstants {

    public static final String RULES_CONTAINER_TYPE = "RetentionRules";

    public static final String DURATION_DAYS_PROP = "retention_def:durationDays";

    public static final String DURATION_MONTHS_PROP = "retention_def:durationMonths";

    public static final String DURATION_YEARS_PROP = "retention_def:durationYears";

    public static final String RECORD_FACET = "Record";

    public static final String RETENTION_RULE_FACET = "RetentionRule";

    public static final String APPLICATION_POLICY_PROP = "retention_rule:applicationPolicy";

    public static final String ENABLED_PROP = "retention_rule:enabled";

    public static final String EXPRESSION_PROP = "retention_def:expression";

    public static final String BEGIN_ACTIONS_PROP = "retention_def:beginActions";

    public static final String END_ACTIONS_PROP = "retention_def:endActions";

    public static final String DURATION_MILLIS_PROP = "retention_def:durationMillis";

    public static final String EVENTS_DIRECTORY_NAME = "RetentionEvent";

    public static final String OBSOLETE_FIELD_ID = "obsolete";

    public static final String RETENTION_CHECKER_LISTENER_IGNORE = "retentionRecordIgnore";

    public static final String STARTING_POINT_POLICY_PROP = "retention_def:startingPointPolicy";

    public static final String STARTING_POINT_EXPRESSION_PROP = "retention_def:startingPointExpression";

    public static final FastDateFormat DEFAULT_DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    public static final String STARTING_POINT_EVENT_PROP = "retention_def:startingPointEvent";

    public static final String RECORD_MANAGER_GROUP_NAME = "RecordManager";

    public static final String MANAGE_LEGAL_HOLD_PERMISSION = "ManageLegalHold";

    public static final String MANAGE_RECORD_PERMISSION = "ManageRecord";

    public static final String DOC_TYPES_PROP = "retention_rule:docTypes";

    public static final String METADATA_XPATH_PROP = "retention_def:metadataXPath";

    public static final String RECORD_RULE_IDS_PROP = "record:ruleIds";

    public static final String INPUT_PROPERTY_KEY = "input";

    public static final String RETAIN_UNTIL_PROP = "record:retainUntil";

    public static final String ACTIVE_EVENT_BASED_RETENTION_RULES_QUERY = "SELECT * FROM Document" //
            + " WHERE ecm:mixinType = '" + RETENTION_RULE_FACET + "'" //
            + " AND ecm:isTrashed = 0" //
            + " AND ecm:isVersion = 0" //
            + " AND " + ENABLED_PROP + " = 1" //
            + " AND " + STARTING_POINT_POLICY_PROP + " = '"
            + RetentionRule.StartingPointPolicy.EVENT_BASED.name().toLowerCase() + "'";

    public static final String RULE_RECORD_DOCUMENT_QUERY = "SELECT * FROM Document" //
            + " WHERE ecm:mixinType = '" + RECORD_FACET + "'" //
            + " AND ecm:isRecord = 1" //
            + " AND ecm:retainUntil = TIMESTAMP '" + DEFAULT_DATE_FORMAT.format(CoreSession.RETAIN_UNTIL_INDETERMINATE)
            + "'";

    public static final String EVENT_CATEGORY = "Retention";

    public static final String RULE_ATTACHED_EVENT = "retentionRuleAttached";

}
