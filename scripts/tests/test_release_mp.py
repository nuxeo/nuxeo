# coding: utf-8
"""
(C) Copyright 2019 Nuxeo (http://nuxeo.com/) and contributors.

All rights reserved. This program and the accompanying materials
are made available under the terms of the GNU Lesser General Public License
(LGPL) version 2.1 which accompanies this distribution, and is available at
http://www.gnu.org/licenses/lgpl-2.1.html

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

Contributors:
    Mickaël Schoentgen <mschoentgen@nuxeo.com>
"""
from mock.mock import patch

from release_mp import ReleaseMP


@patch("glob.glob")
def test_multiple_packages(mocked_glob):
    """See NXP-26137."""

    # Fake release
    release = ReleaseMP("test", None, marketplace_conf="")

    def get_packages_list(self):
        """
        Return the list of packages to work on.
        For this test, we only need one marketplace, let's pick AI.
        """
        return ["nuxeo-ai"]

    with patch.object(ReleaseMP, "get_packages_list", get_packages_list):
        # Ensure we will only work with the AI marketplace
        assert release.get_packages_list() == ["nuxeo-ai"]

        # Use fake values to prevent bad substitution and ease testing
        release.mp_config.set("DEFAULT", "branch", "master")
        release.mp_config.set("DEFAULT", "is_final", "True")
        release.mp_config.set("DEFAULT", "other_versions", "")

        # Set several packages to upload
        packages = ["*/target/nuxeo-ai*.zip", "*/*/target/nuxeo-*.zip"]
        release.mp_config.set("nuxeo-ai", "mp_to_upload", ", ".join(packages))

        # Test the release, it will enter into .upload() and test multi packages
        release.test()

        # And now we check glog.glob() has been called 2 times
        for package in packages:
            mocked_glob.assert_any_call(package)
