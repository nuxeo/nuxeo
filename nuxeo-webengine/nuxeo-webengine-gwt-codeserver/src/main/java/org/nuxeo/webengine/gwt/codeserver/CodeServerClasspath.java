/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.webengine.gwt.codeserver;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;

@XObject("classpath")
public class CodeServerClasspath {

	protected URL[] entries = new URL[0];

	@XNode
	public void setLibdir(File dir) {
		List<URL> entries = new ArrayList<URL>();
		for (File entry : dir.listFiles()) {
			try {
				entries.add(entry.toURI().toURL());
			} catch (MalformedURLException e) {
				throw new NuxeoException("Cannot find location of " + entry);
			}
		}
		this.entries = entries.toArray(new URL[entries.size()]);
	}
}
