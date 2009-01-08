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
import java.io.StringWriter;
import java.util.ArrayList;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;


/**
 * A source writer that is writing in memory.
 * Used to be able to add imports after begining to write
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JSourceWriter extends StringWriter implements SourceWriter {

    protected String crlf = System.getProperty("line.separator"); 
    protected ClassSourceFileComposerFactory composer;
    protected int indent = 0;
    protected boolean inComment = false;
    protected ArrayList<String> interfaces = new ArrayList<String>();
    protected ArrayList<String> imports = new ArrayList<String>();
    protected GeneratorContext context;
    protected PrintWriter printWriter;
    
    public JSourceWriter(ClassSourceFileComposerFactory composer, GeneratorContext context, PrintWriter printWriter) {
        this.composer = composer;
        this.context = context;
        this.printWriter = printWriter;
    }
    
    /**
     * @return the composer.
     */
    public ClassSourceFileComposerFactory getComposer() {
        return composer;
    }
    
    
    public void println() {
        write(crlf);
    }
    
    public void println(String str) {
        print(str);
        write(crlf);
    }
    
    public void print(String s) {
        tabs();
        if (inComment) write("* ");
        write(s);
    }
    
    public void indent() {
        indent++;
    }
    
    public void outdent() { 
        indent--;
    }
    
    public void indentln(String s) {
        indent();
        println(s);
        outdent();  
    }
    
    public void tabs() {
        for (int j = 0; j < indent; ++j) {
            write("  ");
        }
    }

    public void beginJavaDocComment() {
        println("/**");
        inComment = true;
    }

    public void endJavaDocComment() {
        println("*/");
        inComment = false;
    }

    public void commit(TreeLogger logger) {
        for (String imp : imports) {
            composer.addImport(imp);
        }
        for (String intfName : interfaces) {
            composer.addImplementedInterface(intfName);
        }        
        SourceWriter sw = composer.createSourceWriter(context, printWriter);
        // only to debug
//        System.out.println("----------------------------------------------");
//        System.out.println(toString());
//        System.out.println("----------------------------------------------");
        sw.print(toString());
        sw.commit(logger);
    }

    public void addImport(String imp) {
        imports.add(imp);
    }
    
    public void addImplementedInterface(String intfName) {
        interfaces.add(intfName);
    }
    
    
}
