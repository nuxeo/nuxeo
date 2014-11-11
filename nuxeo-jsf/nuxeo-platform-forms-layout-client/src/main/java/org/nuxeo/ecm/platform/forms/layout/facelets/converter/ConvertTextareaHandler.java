/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Thierry Martins
 */

/**
 * Textarea converter handler.
 *
 * @author <a href="mailto:tm@nuxeo.com">Thierry Martins</a>
 */
package org.nuxeo.ecm.platform.forms.layout.facelets.converter;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.convert.Converter;

import org.nuxeo.ecm.platform.ui.web.converter.TextareaConverter;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.tag.MetaRuleset;
import com.sun.facelets.tag.jsf.ConvertHandler;
import com.sun.facelets.tag.jsf.ConverterConfig;

public class ConvertTextareaHandler extends ConvertHandler {

    public ConvertTextareaHandler(ConverterConfig config) {
        super(config);
    }

    /**
     * Returns a new TextareaConverter
     * 
     * @see TextareaConverter
     * @see com.sun.faces.facelets.tag.jsf.ConverterHandler#createConverter(com.sun.faces.facelets.FaceletContext)
     */
    protected Converter createConverter(FaceletContext ctx)
            throws FacesException, ELException, FaceletException {
        return ctx.getFacesContext().getApplication().createConverter(
                TextareaConverter.CONVERTER_ID);
    }
    
    @Override
    public void setAttributes(FaceletContext ctx, Object obj) {
        super.setAttributes(ctx, obj);
    }

    @Override
    public MetaRuleset createMetaRuleset(Class type) {
        return super.createMetaRuleset(type).ignore("locale");
    }
    
}
