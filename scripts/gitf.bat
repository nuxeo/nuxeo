@echo off
REM  Convenient functions for use on Nuxeo projects version controlled under Git
REM
REM  (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
REM
REM  All rights reserved. This program and the accompanying materials
REM  are made available under the terms of the GNU Lesser General Public License
REM  (LGPL) version 2.1 which accompanies this distribution, and is available at
REM  http://www.gnu.org/licenses/lgpl-2.1.html
REM
REM  This library is distributed in the hope that it will be useful,
REM  but WITHOUT ANY WARRANTY; without even the implied warranty of
REM  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
REM  Lesser General Public License for more details.
REM
REM  Contributors:
REM    Julien Carsique
REM

set PWD=%CD%
echo [.]
git %*

for /d %%D in (*) do (
echo [%%D]
cd %PWD%\%%D
git %*
echo
)

cd %PWD%
