/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.shell.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Parameter;
import org.nuxeo.ecm.shell.ShellException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class DefaultCommandType extends AbstractCommandType {

    protected List<Setter> injectable;

    protected Map<String, Token> params;

    protected List<Token> args;

    @SuppressWarnings("unchecked")
    public static DefaultCommandType fromAnnotatedClass(String className)
            throws ShellException {
        Class<Runnable> cls;
        try {
            cls = (Class<Runnable>) Class.forName(className);
        } catch (Exception e) {
            throw new ShellException(e);
        }
        return fromAnnotatedClass(cls);
    }

    public static DefaultCommandType fromAnnotatedClass(
            Class<? extends Runnable> cls) throws ShellException {
        HashMap<String, Token> params = new HashMap<String, Token>();
        ArrayList<Token> args = new ArrayList<Token>();
        ArrayList<Setter> injectable = new ArrayList<Setter>();
        Command cmd = cls.getAnnotation(Command.class);
        if (cmd == null) {
            throw new ShellException("Class " + cls
                    + " is not a command. You must annotated it with @Command");
        }
        for (Field field : cls.getDeclaredFields()) {
            Parameter param = field.getAnnotation(Parameter.class);
            if (param != null) {
                Token a = new Token();
                a.name = param.name();
                a.help = param.help();
                a.isRequired = param.hasValue();
                a.setter = new FieldSetter(field);
                a.completor = param.completor();
                params.put(a.name, a);
                continue;
            }
            Argument arg = field.getAnnotation(Argument.class);
            if (arg != null) {
                Token a = new Token();
                a.name = arg.name();
                a.index = arg.index();
                a.help = arg.help();
                a.completor = arg.completor();
                a.isRequired = arg.required();
                a.setter = new FieldSetter(field);
                args.add(a);
                continue;
            }
            Context ctx = field.getAnnotation(Context.class);
            if (ctx != null) {
                injectable.add(new FieldSetter(field));
            }
        }
        for (Method method : cls.getDeclaredMethods()) {
            Parameter param = method.getAnnotation(Parameter.class);
            if (param != null) {
                Token a = new Token();
                a.name = param.name();
                a.help = param.help();
                a.isRequired = param.hasValue();
                a.setter = new MethodSetter(method);
                a.completor = param.completor();
                params.put(a.name, a);
                continue;
            }
            Argument arg = method.getAnnotation(Argument.class);
            if (arg != null) {
                Token a = new Token();
                a.name = arg.name();
                a.index = arg.index();
                a.help = arg.help();
                a.isRequired = arg.required();
                a.setter = new MethodSetter(method);
                a.completor = arg.completor();
                args.add(a);
            }
        }
        Collections.sort(args);
        return new DefaultCommandType(cls, injectable, params, args);
    }

    public DefaultCommandType(Class<? extends Runnable> cmdClass,
            List<Setter> injectable, Map<String, Token> params, List<Token> args) {
        super(cmdClass, injectable, params, args);
    }

    public String getHelp() {
        return cmdClass.getAnnotation(Command.class).help();
    }

    public String getName() {
        return cmdClass.getAnnotation(Command.class).name();
    }

    public String[] getAliases() {
        return cmdClass.getAnnotation(Command.class).aliases();
    }

    public static class MethodSetter implements Setter {
        protected Method method;

        protected Class<?> type;

        public MethodSetter(Method method) {
            this.method = method;
            method.setAccessible(true);
            Class<?>[] types = method.getParameterTypes();
            if (types.length != 1) {
                throw new IllegalArgumentException(
                        "Invalid method setter should take one argument. Method: "
                                + method);
            }
            type = types[0];
        }

        public void set(Object obj, Object value) throws ShellException {
            try {
                method.invoke(obj, value);
            } catch (Exception e) {
                throw new ShellException(e);
            }
        }

        public Class<?> getType() {
            return type;
        }
    }

    public static class FieldSetter implements Setter {
        protected Field field;

        protected Class<?> type;

        public FieldSetter(Field field) {
            this.field = field;
            field.setAccessible(true);
            this.type = field.getType();
        }

        public void set(Object obj, Object value) throws ShellException {
            try {
                field.set(obj, value);
            } catch (Exception e) {
                throw new ShellException(e);
            }
        }

        public Class<?> getType() {
            return type;
        }
    }

}
