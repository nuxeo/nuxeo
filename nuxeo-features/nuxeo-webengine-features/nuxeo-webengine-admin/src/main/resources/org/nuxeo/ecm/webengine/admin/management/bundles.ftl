<?xml version="1.0"?>
<feed xmlns="http://www.w3.org/2005/Atom" xmlns:nx="http://www.nuxeo.org/server_management">
	<id>urn:nuxeo:bundles</id>
	<title>Bundles</title>
<#list bundles as bundle>
	<entry>
		<id>urn:nuxeo:bundles:${bundle.symbolicName}</id>
		<title>${bundle.symbolicName}</title>
		<updated>${bundle.lastModified}</updated>		
		<link rel="edit" href="${Context.URL}/${bundle.symbolicName}" />
		<link rel="self" href="${Context.URL}/${bundle.symbolicName}" />
		<link rel="alternate" href="${Context.URL}/${bundle.symbolicName}" />
		<link rel="file" href="${Context.URL}/${bundle.symbolicName}/file" />
		<link rel="manifest" href="${Context.URL}/${bundle.symbolicName}/manifest" />
		<nx:state>${bundle.state}</nx:state>
		<summary type="html">
		<#assign text>
		Name: ${This.getBundleHeader(bundle, "Bundle-Name")}
		<br>
		Version: ${This.getBundleHeader(bundle, "Bundle-Version")}
		<br>
		Provider: ${This.getBundleHeader(bundle, "Bundle-Vendor")}
		<br>
		File: <a href="${Context.URL}/${bundle.symbolicName}/file">${This.getBundleFileName(bundle)}</a>
		<br>
		State: ${bundle.state}
		<br>
		Id: ${bundle.bundleId}
		</#assign>
		${text?html}
		</summary>
	</entry>
</#list>
</feed>
