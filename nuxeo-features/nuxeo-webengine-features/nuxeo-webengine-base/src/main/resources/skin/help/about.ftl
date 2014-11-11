<@extends src="base.ftl">
<@block name="title">Nuxeo WebEngine - About</@block>
<@block name="header"><h1><a href="${Context.modulePath}">Nuxeo WebEngine - About</a></@block>
<@block name="content">

<h1>${env.engine} version ${env.version}</h1>
<h2>License:</h2>
<pre>
 (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.

 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Lesser General Public License
 (LGPL) version 2.1 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/lgpl.html

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.
</pre>

<h2>Team:</h2>
<ul>
<li> Main Developer: <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
<li> Contributors: <a href="mailto:eb@nuxeo.com">Eric Barroca</a>,
    <a href="mailto:troger@nuxeo.com">Thomas Roger</a>,
    <a href="mailto:td@nuxeo.com">Thierry Delprat</a>,
    <a href="mailto:tsoulcie@nuxeo.com">Thibaut Soulcie</a>,
    <a href="mailto:sf@nuxeo.com">Stefane Fermigier</a>
</ul>

<h2>Modules:</h2>
<ul>
<#list API.getBundles() as bundle>
  <li> ${bundle.symbolicName}</li>
</#list>
</ul>

<h2>Components:</h2>
<ul>
<#list API.getComponents() as component>
  <li> ${component.name}</li>
</#list>
</ul>

<h2>Pending Components:</h2>
<ul>
<#list API.getPendingComponents() as component>
  <li> ${component.name} </li>
</#list>
</ul>
</@block>
</@extends>
