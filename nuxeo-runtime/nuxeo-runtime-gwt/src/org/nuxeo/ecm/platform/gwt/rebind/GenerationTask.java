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

package org.nuxeo.ecm.platform.gwt.rebind;

import java.io.PrintWriter;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class GenerationTask {

    protected TreeLogger logger;
    protected GeneratorContext context;
    protected String typeName;
    protected TypeOracle oracle;
    protected TypeInfo typeInfo;
    protected ClassSourceFileComposerFactory composer;
    protected SourceWriter writer;
    
    public GenerationTask() {
    }
    
    public String generate(TreeLogger logger, GeneratorContext context, String typeName) throws UnableToCompleteException {
        this.logger = logger;
        this.context = context;
        this.typeName = typeName;        
        oracle = context.getTypeOracle();
        typeInfo = new TypeInfo(getType(typeName));
        
        PrintWriter printWriter = context.tryCreate(logger, typeInfo.getProxyPackage(),
                typeInfo.getProxyName());
        if (printWriter !=null) {
            composer = new ClassSourceFileComposerFactory(
                    typeInfo.getProxyPackage(), typeInfo.getProxyName());
            //composer.addImplementedInterface(typeInfo.getClassType().getSimpleSourceName());         
            writer = new JSourceWriter(composer, context, printWriter);
            run();
        }
        return typeInfo.getProxyQName();
    }

    protected JClassType getTypeForClass(Class<?> clazz) throws UnableToCompleteException {
        JClassType classType = oracle.findType(clazz.getName());
        if (classType == null) {
            logger.log(TreeLogger.ERROR, "Unable to find metadata for type '"
                    + typeName + "'", null);
            throw new UnableToCompleteException();
        }
        return classType;
    }

    protected JClassType getType(String typeName) throws UnableToCompleteException {
        JClassType classType = oracle.findType(typeName);
        if (classType == null) {
            logger.log(TreeLogger.ERROR, "Unable to find metadata for type '"
                    + typeName + "'", null);
            throw new UnableToCompleteException();
        }
        return classType;
    }

    public abstract void run() throws UnableToCompleteException;
    
}
