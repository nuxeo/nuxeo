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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CodeServerLoader extends URLClassLoader {

	CodeServerLoader(URL[] jars) {
		super(jars, CodeServerLoader.class.getClassLoader());
	}

	@Override
	protected java.lang.Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class<?> clazz = findLoadedClass(name);
		if (clazz != null) {
			return clazz;
		}

		if (name.equals(CodeServerLauncher.class.getName())) {
			return getParent().loadClass(name);
		}

		if (name.equals(CodeServerWrapper.class.getName())) {
			try {
				return reloadClass(CodeServerWrapper.class);
			} catch (URISyntaxException | IOException cause) {
				throw new ClassNotFoundException("Cannot reload wrapper in gwt dev class loader", cause);
			}
		}
		try {
			return ClassLoader.getSystemClassLoader().loadClass(name);
		} catch (ClassNotFoundException cause) {
			;
		}
		synchronized (getClassLoadingLock(name)) {
			clazz = findClass(name);
			if (clazz != null) {
				return clazz;
			}
		}
		throw new ClassNotFoundException("Cannot find " + name + " in gwt class loader");
	};

	Class<?> reloadClass(Class<?> clazz) throws URISyntaxException, IOException {
		URI location = clazz.getResource(clazz.getSimpleName().concat(".class")).toURI();
		byte[] content = Files.readAllBytes(getPath(location));
		return defineClass(clazz.getName(), content, 0, content.length);
	}

	private Path getPath(URI location) throws IOException {
		try {
			return Paths.get(location);
		} catch (FileSystemNotFoundException cause) {
			Map<String, Object> env = Collections.emptyMap();
			FileSystems.newFileSystem(location, env);
			return Paths.get(location);
		}
	}

	CodeServerLauncher load() throws ReflectiveOperationException {
		return (CodeServerLauncher) loadClass(CodeServerWrapper.class.getName()).newInstance();
	}

}
