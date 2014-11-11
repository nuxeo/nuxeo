package org.nuxeo.ecm.platform.actions.elcache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.runtime.expression.JexlExpression;

public class CachedJEXLManager {

    private static Map<String, JexlExpression> expCache = new ConcurrentHashMap<String, JexlExpression>();

    public static boolean enforceThreadSafe=false;

    public static boolean useCache=true;

    public static  JexlExpression getExpression(String elString) throws Exception
    {
        if (!useCache){
            return new JexlExpression(elString);
        }

        JexlExpression exp = expCache.get(elString);

        if (exp==null)
        {
            if (enforceThreadSafe) {
                exp =  new ThreadSafeJexlExpression(elString);
            }
            else {
                exp =  new JexlExpression(elString);
            }
            expCache.put(elString, exp);
        }

        return exp;
    }

}
