@echo off
REM  Convenient functions for use on Nuxeo projects version controlled under Mercurial 
REM 
REM  (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
REM 
REM  All rights reserved. This program and the accompanying materials
REM  are made available under the terms of the GNU Lesser General Public License
REM  (LGPL) version 2.1 which accompanies this distribution, and is available at
REM  http://www.gnu.org/licenses/lgpl.html
REM 
REM  This library is distributed in the hope that it will be useful,
REM  but WITHOUT ANY WARRANTY; without even the implied warranty of
REM  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
REM  Lesser General Public License for more details.
REM 
REM  Contributors:
REM    Julien Carsique
REM 
REM  $Id$

set PWD=%CD%
set NXP=%1
set NXC=%2

echo [.]
hg %3 %NXP%

for /d %%D in (nuxeo-platform nuxeo-distribution nuxeo-theme nuxeo-shell nuxeo-webengine nuxeo-gwt nuxeo-services nuxeo-jsf nuxeo-features nuxeo-dm) do (
echo [%%D]
cd %PWD%\%%D
hg %3 %NXP%
)

for /d %%D in (nuxeo-core nuxeo-common nuxeo-runtime) do (
echo [%%D]
cd %PWD%\%%D
hg %3 %NXC%
)

cd %PWD%
