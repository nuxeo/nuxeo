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
package org.nuxeo.ecm.automation.client;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class AdapterManager {

	protected final Set<AdapterFactory<?>> factories = new HashSet<AdapterFactory<?>>();

	// put(BusinessObjectService.class,
	public <T> T getAdapter(Session session, Class<T> adapterType) {
		for (AdapterFactory<?> f : factories) {
			if (!factoryAccept(f, adapterType)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			AdapterFactory<T> tFactory = (AdapterFactory<T>) f;
			return adapterType.cast(tFactory.getAdapter(session, adapterType));
		}
		return null;
	}

	protected boolean factoryAccept(AdapterFactory<?> factory,
			Class<?> adapterType) {
		ParameterizedType itf = (ParameterizedType) factory.getClass()
				.getGenericInterfaces()[0];
		Type type = itf.getActualTypeArguments()[0];
		Class<?> clazz;
		if (type instanceof Class) {
			clazz = (Class<?>) type;
		} else if (type instanceof ParameterizedType) {
			clazz = (Class<?>)((ParameterizedType) type).getRawType();
		} else {			
			throw new UnsupportedOperationException("Don't know how to handle "
					+ type.getClass());
		}
		return clazz.isAssignableFrom(adapterType);
	}

	public void registerAdapter(AdapterFactory<?> factory) {
		factories.add(factory);
	}

	public void clear() {
		factories.clear();
	}

}
