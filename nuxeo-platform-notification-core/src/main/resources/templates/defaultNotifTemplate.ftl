<HTML>
	<BODY>
		<P>${htmlEscape(sender.firstName)} ${htmlEscape(sender.lastName)} wants you to see the following document:
		<a href="${docUrl}">${htlmEscape(doc.dublincore.title)}</a></P> <BR>
		<P>${htlmEscape(comment)}</P>
	</BODY>
</HTML>