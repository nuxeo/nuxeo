/*
 * (C) Copyright 2015-2019 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Delbosc Benoit
 */
import java.nio.file.Path
import io.gatling.commons.util.PathHelper._

object IDEPathHelper {

	val gatlingConfUrl: Path = getClass.getClassLoader.getResource("gatling.conf")
	val projectRootDir = gatlingConfUrl.ancestor(3)

	val mavenSourcesDirectory = projectRootDir / "src" / "test" / "scala"
	val mavenResourcesDirectory = projectRootDir / "src" / "test" / "resources"
	val mavenTargetDirectory = projectRootDir / "target"
	val mavenBinariesDirectory = mavenTargetDirectory / "test-classes"

	val recorderOutputDirectory = mavenSourcesDirectory
	val resultsDirectory = mavenTargetDirectory / "gatling"

	val recorderConfigFile = mavenResourcesDirectory / "recorder.conf"
}
