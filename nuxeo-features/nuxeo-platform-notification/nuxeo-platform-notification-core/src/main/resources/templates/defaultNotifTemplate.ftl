<HTML>
        <BODY>
                <P>${htmlEscape(sender.firstName)} ${htmlEscape(sender.lastName)} wants you to see the following document:
                <a href="${docUrl}">${htmlEscape(doc.dublincore.title)}</a></P> <BR>
                <P>${htmlEscape(comment)}</P>
        </BODY>
</HTML>
