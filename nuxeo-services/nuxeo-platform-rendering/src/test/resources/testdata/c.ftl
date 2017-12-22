<@extends src="/testdata/b.ftl">

<@block name="a1">
 The default content goes here [ <@superBlock/> ]
</@block>

<@block name="a2">
 The default content goes here [ <@superBlock/> ]
 a2: Derived Block in c. Document: ${doc.title}
 <@wiki>${doc.dublincore.content.data}</@wiki>
</@block>

<@block name="b.nested">
 b.nested: Derived Block in c
</@block>

</@extends>
