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

package org.nuxeo.ecm.webengine.model;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.nuxeo.ecm.webengine.model.impl.Utils;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.ecm.webengine.security.PermissionService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ViewDescriptor {
    protected String name;
    protected String fileName;
    protected Guard guard;
    protected Set<String> cats = Collections.emptySet();
    protected boolean auto;
    
    public ViewDescriptor(WebView anno) throws ParseException {
        this (anno.name(), anno.fileName(), anno.categories(), anno.guard(), anno.auto());
    }
    
    public ViewDescriptor(String name, String fileName, String[] cats, String guard, boolean auto) throws ParseException {
        this (name, fileName, cats, guard != null && guard.length() > 0 ? PermissionService.parse(guard) : null, auto);
    }
    
    public ViewDescriptor(String name, String fileName, String[] cats, Guard guard, boolean auto) {
        this.auto = auto;
        this.fileName = Utils.nullIfEmpty(fileName);
        this.name = name;
        if (cats != null && cats.length > 0) {
            this.cats.addAll(Arrays.asList(cats));
        }
        this.guard = guard != null ? guard : Guard.DEFAULT;        
    }

    public String getName() {
        return name;
    } 
    
    public boolean getAuto() {
        return auto;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public Set<String> getCategories() {
        return cats;
    }
    
    
    public boolean isEnabled(Resource ctx) {
        return getGuard().check(ctx);
    }
    
    public  Guard getGuard() {
        return guard;
    }
    
    public Template getTemplate(Resource resource) {
        Template tpl = new Template(resource);
        if (fileName != null) {
            tpl.fileName(fileName);
        } else {
            tpl.name(name);
        }
        return tpl;
    }
    
}
