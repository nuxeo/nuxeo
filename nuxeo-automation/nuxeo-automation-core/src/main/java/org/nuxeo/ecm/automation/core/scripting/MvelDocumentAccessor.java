/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.scripting;

import org.mvel2.MVEL;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.PropertyHandlerFactory;
import org.mvel2.integration.VariableResolverFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MvelDocumentAccessor implements PropertyHandler {

    static {
        PropertyHandlerFactory.registerPropertyHandler(MyObj.class, new MvelDocumentAccessor());
    }
    
    public Object getProperty(String arg0, Object arg1,
            VariableResolverFactory arg2) {
        if ("value".equals(arg0)) {
            return ((MyObjImpl)arg1).value; 
        }
        return null;
    }

    public Object setProperty(String arg0, Object arg1,
            VariableResolverFactory arg2, Object arg3) {
        return null;
    }

    
    public static void main(String[] args) {
        MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;
        System.out.println(MVEL.getProperty("id", new MyObjImpl("testid", "testv")));
        System.out.println(MVEL.getProperty("value", new MyObjImpl("testid", "testv")));
    }
    
    
    public static interface MyObj {
        public String getId();
    }
    
    public static class MyObjImpl implements MyObj {
        String id;String value;
        MyObjImpl(String id, String value) {
            this.id = id; this.value = value;
        }
        public String getId() {
            return id;
        }
    }
    
}
