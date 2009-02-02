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
 *
 * $Id$
 */

package org.nuxeo.runtime.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used to collect method annotations from inheritance tree.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
class MethodAnnotations {

    protected final List<Entry> entries = new ArrayList<Entry>();

    public void addMethods(Class<?> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            addMethod(m);
        }
    }

    public void addMethod(Method m) {
        int mod = m.getModifiers();
        if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) {
            return; // ignore non public or protected methods
        }
        Annotation[] annos = m.getDeclaredAnnotations();
        if (annos.length == 0) {
            return;
        }
        for (int i = 0, len = entries.size(); i < len; i++) {
            Entry entry = entries.get(i);
            if (entry.isSameAs(m)) {
                for (Annotation anno : annos) {
                    Class<?> annoType = anno.annotationType();
                    Annotation a = entry.annos.get(annoType);
                    if (a == null) {
                        //TODO support merging annotations?
                        entry.annos.put(anno.annotationType(), anno);
                        entry.method = m;
                    } else {
                        entry.annos.put(anno.annotationType(), anno);
                        entry.method = m;
                    }
                }
                return;
            }
        }
        entries.add(new Entry(m, annos));
    }

    public void addSuperMethod(AnnotatedMethod am) {
        for (int i = 0, len = entries.size(); i < len; i++) {
            Entry entry = entries.get(i);
            if (entry.isSameAs(am.method)) {
                Annotation[] annos = am.getAnnotations();
                for (Annotation anno : annos) {
                    Class<?> annoType = anno.annotationType();
                    Annotation a = entry.annos.get(annoType);
                    if (a == null) {
                        //TODO support merging annotations?
                        entry.annos.put(anno.annotationType(), anno);
                    } else {
                        entry.annos.put(anno.annotationType(), anno);
                    }
                }
                return;
            }
        }
        entries.add(new Entry(am.method, am.getAnnotations()));
    }

    static class Entry {
        final Class<?>[] parameterTypes; // store as member to avoid cloning when calling getParameterTypes()
        Method method;
        final Map<Class<? extends Annotation>, Annotation> annos;

        Entry(Method method, Annotation[] annos) {
            this.method = method;
            parameterTypes = method.getParameterTypes();
            this.annos = new HashMap<Class<? extends Annotation>, Annotation>();
            for (Annotation anno : annos) {
                this.annos.put(anno.annotationType(), anno);
            }
        }

        public boolean isSameAs(Method m) {
            if (method.getName().equals(m.getName())) {
                if (method.getReturnType() == m.getReturnType()) {
                    Class<?>[] pt = m.getParameterTypes();
                    if (parameterTypes.length == pt.length) {
                        for (int i = 0; i < parameterTypes.length; i++) {
                            if (parameterTypes[i] != pt[i]) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
            }
            return false;
        }
    }

}
