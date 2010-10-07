/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.smart.query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Smart query providing all the needed methods for UI interaction that will
 * make it possible to build a query adding clauses step by step.
 *
 * @author Anahide Tchertchian
 */
public abstract class IncrementalSmartQuery implements SmartQuery {

    protected String existingQueryPart;

    protected String logicalOperator;

    protected Boolean addNotOperator;

    protected Boolean openParenthesis;

    protected Boolean closeParenthesis;

    protected String selectedRowName;

    protected String leftExpression;

    protected String conditionalOperator;

    protected Object value;

    // provide a separate field for accepted expression values for a good JFS
    // resolution

    protected Boolean booleanValue;

    protected String stringValue;

    protected List<String> stringListValue;

    protected Date datetimeValue;

    protected Date otherDatetimeValue;

    protected Date dateValue;

    protected Date otherDateValue;

    protected Long integerValue;

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

    public boolean getShowOpenParenthesis() {
        return true;
    }

    public Boolean getOpenParenthesis() {
        return openParenthesis;
    }

    public void setOpenParenthesis(Boolean openParenthesis) {
        this.openParenthesis = openParenthesis;
    }

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

    public void setSelectedRowName(String selectedRowName) {
        this.selectedRowName = selectedRowName;
        leftExpression = null;
        conditionalOperator = null;
        clearValues();
    }

    public List<String> getSelectedRowNames() {
        List<String> res = new ArrayList<String>();
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

    public void setConditionalOperator(String conditionalOperator) {
        this.conditionalOperator = conditionalOperator;
        clearValues();
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

    protected void clearValues() {
        value = null;
        booleanValue = null;
        stringValue = null;
        stringListValue = null;
        datetimeValue = null;
        dateValue = null;
        integerValue = null;
        floatValue = null;
    }


}
