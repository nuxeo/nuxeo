package org.nuxeo.ecm.platform.query.api;

import org.nuxeo.common.xmap.annotation.XNode;

public interface PredicateDefinition {

    public static final String ATOMIC_PREDICATE = "atomic";

    public static final String SUB_CLAUSE_PREDICATE = "subClause";

    @XNode("@operator")
    public void setOperator(String operator);

    public String getOperator();

    public String getParameter();

    public void setParameter(String parameter);

    public PredicateFieldDefinition[] getValues();

    public void setValues(PredicateFieldDefinition[] values);

    public String getType();

    public String getOperatorField();

    public String getOperatorSchema();

}