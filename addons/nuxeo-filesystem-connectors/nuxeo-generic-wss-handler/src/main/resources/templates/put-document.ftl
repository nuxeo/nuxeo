<html><head><title>vermeer RPC packet</title></head>
<body>
<p>method=put document:${request.version}
<p>message=successfully put document '${doc.getRelativeFilePath(siteRoot)}' as '${doc.getRelativeFilePath(siteRoot)}'
<p>document=
<ul>
<li>document_name=${doc.getRelativeFilePath(siteRoot)}
<li>meta_info=
<ul>
<li>vti_sourcecontrollockexpires
<li>TR|04 Sep 2009 14:01:50 -0000
<li>Subject
<li>SW|
<li>vti_rtag
<li>SW|rt:${doc.etag}@00000000004
<li>vti_etag
<li>SW|&#34;&#123;${doc.etag}&#125;,4&#34;
<li>vti_parserversion
<li>SR|${config.WSSServerVersion}
<li>vti_timecreated
<li>TR|04 Sep 2009 09:34:49 -0000
<li>vti_canmaybeedit
<li>BX|true
<li>_Category
<li>SW|
<li>vti_author
<li>SR|${request.UserName}
<li>vti_sourcecontrolcookie
<li>SR|fp_internal
<li>vti_sourcecontrolcheckedoutby
<li>SR|${doc.checkoutUser}
<li>vti_sourcecontroltimecheckedout
<li>TR|04 Sep 2009 13:51:50 -0000
<li>vti_sourcecontrolversion
<li>SR|V1.0
<li>_Comments
<li>SW|
<li>vti_linkinfo
<li>VX|UHHS|http://www.w3.org/2001/03/thread UHHS|http://www.w3.org/2001/12/replyType UHHS|http://www.w3.org/2001/Annotea/User/Protocol.html
<li>vti_level
<li>IR|1
<li>vti_approvallevel
<li>SR|
<li>vti_categories
<li>VW|
<li>vti_filesize
<li>IR|492544
<li>vti_assignedto
<li>SR|
<li>Keywords
<li>SW|
<li>vti_modifiedby
<li>SR|${request.UserName}
<li>ContentTypeId
<li>SW|0x0101003C97D20C47E1C3498214C5297D0BA161
<li>vti_nexttolasttimemodified
<li>TR|04 Sep 2009 10:18:34 -0000
<li>vti_timelastmodified
<li>TR|04 Sep 2009 13:52:52 -0000
<li>vti_candeleteversion
<li>BR|true
<li>vti_sourcecontrolmultiuserchkoutby
<li>VR|${doc.checkoutUser}
<li>vti_title
<li>SR|
<li>_Author
<li>SW|${doc.author}
</ul>
</ul>
</body>
</html>