package org.nuxeo.ecm.core.storage.sql;

import java.lang.reflect.Method;

import org.nuxeo.runtime.test.ConditionalIgnoreRule;
import org.nuxeo.ecm.core.storage.sql.DatabaseH2;
import org.nuxeo.ecm.core.storage.sql.DatabasePostgreSQL;

public class IgnoreNonPooledCondition implements ConditionalIgnoreRule.Condition {

    @Override
    public boolean shouldIgnore(Method method, Object target) {
        return !(DatabaseHelper.DATABASE instanceof DatabaseH2
                || DatabaseHelper.DATABASE instanceof DatabasePostgreSQL);
    }

}
