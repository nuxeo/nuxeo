#!/bin/bash
##
## (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
##
## All rights reserved. This program and the accompanying materials
## are made available under the terms of the GNU Lesser General Public License
## (LGPL) version 2.1 which accompanies this distribution, and is available at
## http://www.gnu.org/licenses/lgpl.html
##
## This library is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
## Lesser General Public License for more details.
##
## Contributors:
##     Julien Carsique
##
## Script for Launcher debugging purpose: calls nuxeoctl with LAUNCHER_DEBUG option
## For debugging Nuxeo (not the Nuxeo Launcher), use nuxeoctl's "-d" option
##

: ${PWD:=$(cd "$(dirname "$0")"/..; pwd -P)}
export LAUNCHER_DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,address=8788,server=y,suspend=y"
./test.sh $@
