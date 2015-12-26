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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("codeserver")
public class CodeServerConfig {

	@XNode("@module")
	String module;

	@XNode("classpath")
	CodeServerClasspath classpath;

	@XNodeList(value = "option", type = ArrayList.class, componentType = CodeServerOption.class)
	List<CodeServerOption> options = new ArrayList<>();

	CodeServerLauncher launcher;

	private CodeServerLoader loader;

	String[] toArgs() {
		List<String> args = new ArrayList<>();
		for (CodeServerOption each : options) {
			each.toArgs(args);
		}
		args.add(module);
		return args.toArray(new String[args.size()]);
	}

	void startup() throws Exception {
		loader = new CodeServerLoader(classpath.entries);
		launcher = loader.load();
		launcher.startup(toArgs());
	}

	void shutdown() throws Exception {
		try {
			if (launcher != null) {
				launcher.shutdown();
			}
		} finally {
			launcher = null;
			try {
				if (loader != null) {
					loader.close();
				}
			} finally {
				loader = null;
			}
		}
	}
}
