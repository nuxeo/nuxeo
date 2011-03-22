/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
