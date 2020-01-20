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
package org.nuxeo.retention.adapters;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 11.1
 */
public class RetentionRule {

    public enum ApplicationPolicy {
        AUTO, MANUAL
    }

    public enum StartingPointPolicy {
        IMMEDIATE, AFTER_DELAY, EVENT_BASED, METADATA_BASED;
    }

    protected DocumentModel document;

    public RetentionRule(DocumentModel doc) {
        this.document = doc;
    }

    public void disable() {
        document.setPropertyValue(RetentionConstants.ENABLED_PROP, false);
    }

    public void enable() {
        document.setPropertyValue(RetentionConstants.ENABLED_PROP, true);
    }

    public String getApplicationPolicy() {
        return (String) document.getPropertyValue(RetentionConstants.APPLICATION_POLICY_PROP);
    }

    public List<String> getBeginActions() {
        Serializable propertyValue = document.getPropertyValue(RetentionConstants.BEGIN_ACTIONS_PROP);
        if (propertyValue == null) {
            return Collections.emptyList();
        }
        return Arrays.asList((String[]) propertyValue);
    }

    public List<String> getDocTypes() {
        @SuppressWarnings("unchecked")
        List<String> propertyValue = (List<String>) document.getPropertyValue(RetentionConstants.DOC_TYPES_PROP);
        return propertyValue;
    }

    public DocumentModel getDocument() {
        return document;
    }

    public long getDurationDays() {
        Long days = (Long) document.getPropertyValue(RetentionConstants.DURATION_DAYS_PROP);
        return days == null ? 0 : days;
    }

    public long getDurationMillis() {
        Long millis = (Long) document.getPropertyValue(RetentionConstants.DURATION_MILLIS_PROP);
        return millis == null ? 0 : millis;
    }

    public long getDurationMonths() {
        Long months = (Long) document.getPropertyValue(RetentionConstants.DURATION_MONTHS_PROP);
        return months == null ? 0 : months;
    }

    public long getDurationYears() {
        Long years = (Long) document.getPropertyValue(RetentionConstants.DURATION_YEARS_PROP);
        return years == null ? 0 : years;
    }

    public List<String> getEndActions() {
        Serializable propertyValue = document.getPropertyValue(RetentionConstants.END_ACTIONS_PROP);
        if (propertyValue == null) {
            return Collections.emptyList();
        }
        return Arrays.asList((String[]) propertyValue);
    }

    public String getExpression() {
        return (String) document.getPropertyValue(RetentionConstants.EXPRESSION_PROP);
    }

    public String getMetadataXpath() {
        return (String) document.getPropertyValue(RetentionConstants.METADATA_XPATH_PROP);
    }

    public Calendar getRetainUntilDateFrom(Calendar calendar) {
        LocalDateTime datetime = LocalDateTime.ofInstant(calendar.getTime().toInstant(), ZoneId.systemDefault());
        return getRetainUntilDateFrom(datetime);
    }

    protected Calendar getRetainUntilDateFrom(LocalDateTime datetime) {
        LocalDateTime localDateTime = datetime.plusYears(getDurationYears())
                                              .plusMonths(getDurationMonths())
                                              .plusDays(getDurationDays())
                                              .plus(getDurationMillis(), ChronoUnit.MILLIS);
        return GregorianCalendar.from(localDateTime.atZone(ZoneId.systemDefault()));
    }

    public Calendar getRetainUntilDateFromNow() {
        return getRetainUntilDateFrom(LocalDateTime.now());
    }

    public String getStartingPointEvent() {
        return (String) document.getPropertyValue(RetentionConstants.STARTING_POINT_EVENT_PROP);
    }

    public String getStartingPointExpression() {
        return (String) document.getPropertyValue(RetentionConstants.STARTING_POINT_EXPRESSION_PROP);
    }

    public StartingPointPolicy getStartingPointPolicy() {
        String value = (String) document.getPropertyValue(RetentionConstants.STARTING_POINT_POLICY_PROP);
        if (value != null) {
            return StartingPointPolicy.valueOf(value.toUpperCase());
        }
        return null;

    }

    public boolean isAfterDelay() {
        return StartingPointPolicy.AFTER_DELAY.equals(getStartingPointPolicy());
    }

    public boolean isAuto() {
        return ApplicationPolicy.AUTO.name().toLowerCase().equals(getApplicationPolicy());
    }

    public boolean isDocTypeAccepted(String docType) {
        List<String> types = getDocTypes();
        return types == null || types.isEmpty() || types.contains(docType);
    }

    public boolean isEnabled() {
        Boolean enabled = (Boolean) document.getPropertyValue(RetentionConstants.ENABLED_PROP);
        return Boolean.TRUE.equals(enabled);
    }

    public boolean isEventBased() {
        return getStartingPointPolicy() == StartingPointPolicy.EVENT_BASED;
    }

    public boolean isImmediate() {
        return getStartingPointPolicy() == StartingPointPolicy.IMMEDIATE;
    }

    public boolean isManual() {
        return ApplicationPolicy.MANUAL.name().toLowerCase().equals(getApplicationPolicy());
    }

    public boolean isMetadataBased() {
        return getStartingPointPolicy() == StartingPointPolicy.METADATA_BASED;
    }

    public void setApplicationPolicy(ApplicationPolicy policy) {
        document.setPropertyValue(RetentionConstants.APPLICATION_POLICY_PROP, policy.name().toLowerCase());
    }

    public void setBeginActions(List<String> actions) {
        document.setPropertyValue(RetentionConstants.BEGIN_ACTIONS_PROP, (Serializable) actions);
    }

    public void setDocTypes(List<String> types) {
        document.setPropertyValue(RetentionConstants.DOC_TYPES_PROP, (Serializable) types);
    }

    public void setDurationDays(long days) {
        document.setPropertyValue(RetentionConstants.DURATION_DAYS_PROP, days);
    }

    public void setDurationMillis(long millis) {
        document.setPropertyValue(RetentionConstants.DURATION_MILLIS_PROP, millis);
    }

    public void setDurationMonths(long months) {
        document.setPropertyValue(RetentionConstants.DURATION_MONTHS_PROP, months);
    }

    public void setDurationYears(long years) {
        document.setPropertyValue(RetentionConstants.DURATION_YEARS_PROP, years);
    }

    public void setEndActions(List<String> actions) {
        document.setPropertyValue(RetentionConstants.END_ACTIONS_PROP, (Serializable) actions);
    }

    public void setExpression(String expression) {
        document.setPropertyValue(RetentionConstants.EXPRESSION_PROP, expression);
    }

    public void setMetadataXpath(String xpath) {
        if (xpath != null) {
            SchemaManager schemaManager = Framework.getService(SchemaManager.class);
            if (!(schemaManager.getField(xpath).getType() instanceof DateType)) {
                throw new IllegalArgumentException("xpath must be of type DateType");
            }
        }
        document.setPropertyValue(RetentionConstants.METADATA_XPATH_PROP, xpath);
    }

    public void setStartingPointEvent(String eventId) {
        document.setPropertyValue(RetentionConstants.STARTING_POINT_EVENT_PROP, eventId);
    }

    public void setStartingPointExpression(String expression) {
        document.setPropertyValue(RetentionConstants.STARTING_POINT_EXPRESSION_PROP, expression);
    }

    public void setStartingPointPolicy(StartingPointPolicy policy) {
        document.setPropertyValue(RetentionConstants.STARTING_POINT_POLICY_PROP, policy.name().toLowerCase());
    }

}
