/*
 * (C) Copyright 2014, 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.runtime.datasource;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;
import org.tranql.connector.ExceptionSorter;

/**
 *
 *
 * @since 8.3
 */
public class DatasourceExceptionSorter implements ExceptionSorter {

    public enum Classcode {
        NoError("00"),
        Warning("01"),
        NoData("02"),
        DynamicSQLError("07"),
        ConnectionException("08"),
        TriggeredActionException("09"),
        FeatureNotSupported("0A"),
        InvalidTransactionInitiation("0B"),
        InvalidTargetTypeSpecification("0D"),
        InvalidSchemaNameListSpecification("0E"),
        LocatorException("0F"),
        ResignalWhenHandlerNotActive("0K"),
        InvalidGrantor("0L"),
        InvalidSqlInvokedProcedureReference("0M"),
        MappingError("0N"),
        InvalidRoleSpecification("0P"),
        InvalidTransformGroupNameSpecification("0S"),
        TargetTableDisagreesWithCursorSpecification("0T"),
        AttemptToAssignNonUpdatableColumn("0U"),
        AttemptToAssignToOrderingColumn("0V"),
        ProhibitedStatementEncouteredDuringTriggerExecution("0W"),
        DiagnosticsException("0Z"),
        XQuery("10"),
        CaseNotFoundInCaseStatement("20"),
        CardinalityViolation("21"),
        DataException("22"),
        IntegrityConstraintViolation("23"),
        InvalidCursorState("24"),
        InvalidTransactionState("25"),
        InvalidSQLStatementName("26"),
        TriggeredDataChangeViolation("27"),
        InvalidAuthorizationSpeciciation("28"),
        DependentPrivilegeDescriptorsAlreadyExsist("2B"),
        InvalidConnectionName("2E"),
        InvalidCharacterSetName("2C"),
        InvalidTransactionTermination("2D"),
        SqlRoutineException("2F"),
        InvalidSessionCollationSpecication("2H"),
        InvalidSqlStatementIdentifier("30"),
        InvalidSqlDescriptorName("33"),
        InvalidCursorName("34"),
        InvalidConditionNumber("35"),
        CursorSensivityException("36"),
        SyntaxError("37"),
        ExternalRoutineException("38"),
        ExternalRoutineInvocationException("39"),
        SavepointException("3B"),
        InvalidCatalogName("3D"),
        AmbiguousCursorName("3C"),
        InvalidSchemaName("3F"),
        TransactionRollback("40"),
        SyntaxErrorOrAccessRuleViolation("42"),
        WithCheckOptionViolation("44"),
        JavaErrors("46"),
        InvalidApplicationState("51"),
        InvalidOperandOrInconsistentSpecification("53"),
        SqlOrProductLimitExcedeed("54"),
        ObjectNotInPrerequisiteState("55"),
        MiscellaneoudSqlOrProductError("56"),
        ResourceNotAvailableOrOperatorIntervention("57"),
        SystemError("58"),
        CommonUtilitiesAndTools("5U"),
        RemoteDatabaseAccess("HZ");

        public String value;

        Classcode(String code) {
            value = code;
        }

    }

    @XObject("sorter")
    public static class Configuration {

        @XNode("@id")
        String id = "";

        @XNode("@override")
        boolean override = false;

        @XNode("@path")
        String pathname;

        boolean matches(String classname) {
            return classname.startsWith(pathname);
        }

        @XNodeList(value = "code", type = String[].class, componentType = String.class)
        public void setCodes(String... values) {
            for (String value : values) {
                Classcode classcode = Classcode.valueOf(value);
                if (classcode != null) {
                    value = classcode.value;
                }
                int length = value.length();
                if (length == 2) {
                    codes.add(value);
                } else if (length == 5) {
                    states.add(value);
                } else {
                    LogFactory.getLog(DatasourceExceptionSorter.class).error("invalid code " + value);
                }
            }
        }

        final Set<String> codes = new HashSet<>();

        final Set<String> states = new HashSet<>();

        @XNodeList(value = "vendor", type = HashSet.class, componentType = Integer.class)
        Set<Integer> vendors = new HashSet<>();

        boolean isFatal(String sqlstate, Integer vendor) {
            String code = sqlstate.substring(0, 2);
            return codes.contains(code) || states.contains(sqlstate) || vendors.contains(vendor);
        }
    }

    public static class Registry extends ContributionFragmentRegistry<Configuration> {

        final Map<String, Configuration> actuals = new HashMap<>();

        @Override
        public String getContributionId(Configuration contrib) {
            return contrib.id;
        }

        @Override
        public void contributionUpdated(String id, Configuration contrib, Configuration newOrigContrib) {
            actuals.put(id, contrib);
        }

        @Override
        public void contributionRemoved(String id, Configuration origContrib) {
            actuals.put(id, origContrib);
        }

        @Override
        public Configuration clone(Configuration orig) {
            Configuration cloned = new Configuration();
            cloned.states.addAll(orig.states);
            cloned.codes.addAll(orig.codes);
            return cloned;
        }

        @Override
        public void merge(Configuration src, Configuration dst) {
            if (src.override) {
                dst.states.clear();
                dst.codes.clear();
            }
            dst.states.addAll(src.states);
            dst.codes.addAll(src.codes);
        }

        public Configuration lookup(SQLException se) {
            for (StackTraceElement frame : se.getStackTrace()) {
                for (Configuration config : actuals.values()) {
                    if ("".equals(config.id)) {
                        continue;
                    }
                    if (config.matches(frame.getClassName())) {
                        return config;
                    }
                }
            }
            return actuals.get("");
        }

    }

    Configuration configuration;

    @Override
    public boolean isExceptionFatal(Exception e) {
        if (!(e instanceof SQLException)) {
            return true;
        }
        SQLException se = (SQLException) e;
        String statuscode = se.getSQLState();
        Integer errorcode = Integer.valueOf(se.getErrorCode());
        if (configuration == null) {
            configuration = DataSourceComponent.instance.sorterRegistry.lookup(se);
        }
        return configuration.isFatal(statuscode, errorcode);
    }

    @Override
    public boolean rollbackOnFatalException() {
        return true;
    }

}
