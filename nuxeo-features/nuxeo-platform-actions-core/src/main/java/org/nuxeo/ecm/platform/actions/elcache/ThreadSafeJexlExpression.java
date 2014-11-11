package org.nuxeo.ecm.platform.actions.elcache;

import org.nuxeo.runtime.expression.Context;
import org.nuxeo.runtime.expression.JexlExpression;

public class ThreadSafeJexlExpression extends JexlExpression
{
    public ThreadSafeJexlExpression(String elExpression) throws Exception
    {
        super(elExpression);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object eval(Context context) throws Exception {
        synchronized (this) {
            return super.eval(context);
        }
    }

}

