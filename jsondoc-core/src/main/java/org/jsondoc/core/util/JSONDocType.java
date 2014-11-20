package org.jsondoc.core.util;

import java.util.LinkedList;
import java.util.List;

public class JSONDocType {

	private List<String> type = new LinkedList<String>();

	private JSONDocType mapKey;

	private JSONDocType mapValue;
	
	public JSONDocType() {
		
	}
	
	public JSONDocType(String item) {
		this.type.add(item);
	}
	
	public void addItemToType(String item) {
		this.type.add(item);
	}

	public String getType() {
		StringBuffer stringType = new StringBuffer();
		for (int i = 0; i < type.size(); i++) {
			stringType.append(type.get(i));
			if(i < type.size() - 1) {
				stringType.append(" of ");
			}
		}
		return stringType.toString();
	}

	public void setType(List<String> type) {
		this.type = type;
	}

	public JSONDocType getMapKey() {
		return mapKey;
	}

	public void setMapKey(JSONDocType mapKey) {
		this.mapKey = mapKey;
	}

	public JSONDocType getMapValue() {
		return mapValue;
	}

	public void setMapValue(JSONDocType mapValue) {
		this.mapValue = mapValue;
	}

}
