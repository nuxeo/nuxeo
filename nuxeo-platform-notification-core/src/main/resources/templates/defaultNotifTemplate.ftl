<HTML>
	<BODY>
		<P>${htmlEscape(sender.firstName)} ${htmlEscape(sender.lastName)} wants you to see the folowing document:
		<a href="${docUrl}">${doc.dublincore.title}</a></P> <BR>
		<P>${comment}</P>
	</BODY>
</HTML>