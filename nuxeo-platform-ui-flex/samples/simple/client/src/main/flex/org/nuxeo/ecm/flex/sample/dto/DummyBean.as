
package org.nuxeo.ecm.flex.sample.dto
{
	import mx.collections.ArrayCollection;

	[RemoteClass(alias="org.nuxeo.ecm.platform.ui.flex.samples.DummyBean")]
	public class DummyBean extends BaseDummyBean
	{
		
		public function DummyBean()
		{
			_myField = "Value from AS";
		}
		
		public function get myField():String
		{
			return _myField;
		}
		
		public function set myField(value:String):void
		{
			_myField = value;
		}

		public function doSomething2():void
		{
			_myField = "value changed by local method of DummyBean class";
		}
		
	}
}
