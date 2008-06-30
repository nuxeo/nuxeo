
package org.nuxeo.ecm.flex.dto
{
	import mx.collections.ArrayCollection;
	
	[RemoteClass(alias="org.nuxeo.ecm.platform.ui.flex.samples.DummyBean")]
	public class DummyBean
	{
		private var _myField:String;
		
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
	}
}
