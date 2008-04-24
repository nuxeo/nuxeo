<@extends src="b.ftl">

<@block name="a2">
 a2: Derived Block in c. Document: ${this.title}
 <@transform name="wiki">${this.dublincore.content.data}</@transform>
</@block>

<@block name="b.nested">
 b.nested: Derived Block in c
</@block>

</@extends>
