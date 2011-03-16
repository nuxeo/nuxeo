/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.service.proxy;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Test<T> {

    T obj;

    public Test(T obj) {this.obj = obj;}

    public static void main(String[] args) {
        Test<String> t = new Test<String>("abc");
        System.out.println(t.obj);
        System.out.println(t.getClass().getTypeParameters()[0].getName());
        System.out.println(t.getClass().getTypeParameters()[0].getBounds().length);
        System.out.println(t.getClass().getTypeParameters()[0].getBounds()[0]);
        System.out.println(t.getClass().getTypeParameters()[0].getGenericDeclaration());
    }


}
