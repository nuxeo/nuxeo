<html>
<body>
<p>
    Document ${htmlEscape(docTitle)} is now available, created on ${dateTime?datetime?string("dd/MM/yyyy - HH:mm")}
    by ${author}.<br>Located at ${docLocation}, version is ${docVersion}, state is ${docState}.
</p>
<p>
    You received this notification because you subscribed to dummy notification on this
    document or on one of its parents.
</p>
</body>
</html>
