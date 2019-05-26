/*
 * (C) Copyright 2010-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.smart.query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Smart query providing all the needed methods for UI interaction that will make it possible to build a query adding
 * clauses step by step.
 * <p>
 * Specific getters and setters have been defined instead of a generic one for a better resolution of target types.
 *
 * @since 5.4
 * @author Anahide Tchertchian
 */
public abstract class IncrementalSmartQuery implements SmartQuery {

    private static final long serialVersionUID = 1L;

    /**
     * Stores the current existing query part.
     * <p>
     * This does not need to be a valid query as it can still be refined after.
     */
    protected String existingQueryPart;

    /**
     * String containing the logical operator at start of the query part to add (for instance 'AND' or 'OR').
     */
    protected String logicalOperator;

    /**
     * Boolean indicating if the query part to add should be negated (for instance by adding the 'NOT' marker before)
     */
    protected Boolean addNotOperator;

    /**
     * Boolean indicating if an open parenthesis should be added prior to the query part to add.
     */
    protected Boolean openParenthesis;

    /**
     * Boolean indicating if an closed parenthesis should be added after the query part to add.
     */
    protected Boolean closeParenthesis;

    /**
     * Marker for layout row selection that will make it possible to display only the widgets defined in this row for
     * the rest of the query part definition.
     */
    protected String selectedRowName;

    /**
     * String typically representing the search index for the query part to add.
     */
    protected String leftExpression;

    /**
     * String representing the conditional operator to use when building the query part to add (for instance '=',
     * 'LIKE',...)
     */
    protected String conditionalOperator;

    protected Boolean escapeValue;

    /**
     * Generic set value for the new query part to add.
     * <p>
     * Holds the last value set on one of the specific value setters.
     */
    protected Object value;

    // provide a separate field for accepted expression values for a good JFS
    // resolution

    /**
     * Boolean value binding.
     */
    protected Boolean booleanValue;

    /**
     * String value binding.
     */
    protected String stringValue;

    /**
     * String list value binding.
     */
    protected List<String> stringListValue;

    /**
     * String array value binding.
     */
    protected String[] stringArrayValue;

    /**
     * Date and time value binding.
     */
    protected Date datetimeValue;

    /**
     * Another date and time value binding (useful when using the 'BETWEEN' operator for instance)
     */
    protected Date otherDatetimeValue;

    /**
     * Date value binding
     */
    protected Date dateValue;

    /**
     * Another date value binding (useful when using the 'BETWEEN' operator for instance)
     */
    protected Date otherDateValue;

    /**
     * Integer value binding.
     */
    protected Long integerValue;

    /**
     * Float value binding.
     */
    protected Double floatValue;

    public IncrementalSmartQuery(String existingQueryPart) {
        super();
        this.existingQueryPart = existingQueryPart;
    }

    public String getExistingQueryPart() {
        return existingQueryPart;
    }

    public void setExistingQueryPart(String existingQueryPart) {
        this.existingQueryPart = existingQueryPart;
    }

    /**
     * Returns true if existing query part is not empty.
     */
    public boolean getShowLogicalOperator() {
        if (existingQueryPart == null || existingQueryPart.trim().length() == 0) {
            return false;
        }
        return true;
    }

    public String getLogicalOperator() {
        return logicalOperator;
    }

    public void setLogicalOperator(String logicalOperator) {
        this.logicalOperator = logicalOperator;
    }

    public boolean getShowAddNotOperator() {
        return true;
    }

    public Boolean getAddNotOperator() {
        return addNotOperator;
    }

    public void setAddNotOperator(Boolean addNotOperator) {
        this.addNotOperator = addNotOperator;
    }

    /**
     * Returns true
     */
    public boolean getShowOpenParenthesis() {
        return true;
    }

    public Boolean getOpenParenthesis() {
        return openParenthesis;
    }

    public void setOpenParenthesis(Boolean openParenthesis) {
        this.openParenthesis = openParenthesis;
    }

    /**
     * Returns true if there are strictly more open parenthesis in the existing query part than closed ones.
     */
    public boolean getShowCloseParenthesis() {
        if (existingQueryPart != null) {
            int numberOpened = StringUtils.countMatches(existingQueryPart, "(");
            int numberClosed = StringUtils.countMatches(existingQueryPart, ")");
            if (numberOpened > numberClosed) {
                return true;
            }
        }
        return false;
    }

    public Boolean getCloseParenthesis() {
        return closeParenthesis;
    }

    public void setCloseParenthesis(Boolean closeParenthesis) {
        this.closeParenthesis = closeParenthesis;
    }

    public String getSelectedRowName() {
        return selectedRowName;
    }

    /**
     * Sets the selected row name.
     * <p>
     * Also resets the left expression, conditional operator, and all value bindings, as they may be set to values that
     * are not relevant in this new row context.
     */
    public void setSelectedRowName(String selectedRowName) {
        this.selectedRowName = selectedRowName;
        leftExpression = null;
        conditionalOperator = null;
        clearValues();
    }

    // TODO: will be useful if layout tag can use it to filter other rows, see
    // NXP-5725
    public List<String> getSelectedRowNames() {
        List<String> res = new ArrayList<>();
        if (selectedRowName != null) {
            res.add(selectedRowName);
        }
        return res;
    }

    public String getLeftExpression() {
        return leftExpression;
    }

    public void setLeftExpression(String leftExpression) {
        this.leftExpression = leftExpression;
    }

    public String getConditionalOperator() {
        return conditionalOperator;
    }

    /**
     * Sets the conditional operator.
     * <p>
     * Also resets all the value bindings, as they may be set to values that are not relevant in this new conditional
     * operator context.
     */
    public void setConditionalOperator(String conditionalOperator) {
        this.conditionalOperator = conditionalOperator;
        clearValues();
    }

    public Boolean getEscapeValue() {
        return escapeValue;
    }

    public void setEscapeValue(Boolean escapeValue) {
        this.escapeValue = escapeValue;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
        setValue(booleanValue);
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
        setValue(stringValue);
    }

    public List<String> getStringListValue() {
        return stringListValue;
    }

    public void setStringListValue(List<String> stringListValue) {
        this.stringListValue = stringListValue;
        setValue(stringListValue);
    }

    public String[] getStringArrayValue() {
        return stringArrayValue;
    }

    public void setStringArrayValue(String[] stringArrayValue) {
        this.stringArrayValue = stringArrayValue;
        setValue(stringArrayValue);
    }

    public Date getDatetimeValue() {
        return datetimeValue;
    }

    public void setDatetimeValue(Date datetimeValue) {
        this.datetimeValue = datetimeValue;
        setValue(datetimeValue);
    }

    public Date getOtherDatetimeValue() {
        return otherDatetimeValue;
    }

    public void setOtherDatetimeValue(Date otherDatetimeValue) {
        this.otherDatetimeValue = otherDatetimeValue;
        setValue(otherDatetimeValue);
    }

    public Date getDateValue() {
        return dateValue;
    }

    public void setDateValue(Date dateValue) {
        this.dateValue = dateValue;
        setValue(dateValue);
    }

    public Date getOtherDateValue() {
        return otherDateValue;
    }

    public void setOtherDateValue(Date otherDateValue) {
        this.otherDateValue = otherDateValue;
        setValue(otherDateValue);
    }

    public Long getIntegerValue() {
        return integerValue;
    }

    public void setIntegerValue(Long integerValue) {
        this.integerValue = integerValue;
        setValue(integerValue);
    }

    public Double getFloatValue() {
        return floatValue;
    }

    public void setFloatValue(Double floatValue) {
        this.floatValue = floatValue;
        setValue(floatValue);
    }

    /**
     * Clears all field values, except the existing quedry part.
     */
    protected void clear() {
        logicalOperator = null;
        addNotOperator = null;
        openParenthesis = null;
        closeParenthesis = null;
        selectedRowName = null;
        leftExpression = null;
        conditionalOperator = null;
        clearValues();
    }

    /**
     * Clears all value bindings.
     */
    protected void clearValues() {
        escapeValue = null;
        value = null;
        booleanValue = null;
        stringValue = null;
        stringListValue = null;
        stringArrayValue = null;
        datetimeValue = null;
        otherDatetimeValue = null;
        dateValue = null;
        otherDateValue = null;
        integerValue = null;
        floatValue = null;
    }

}
