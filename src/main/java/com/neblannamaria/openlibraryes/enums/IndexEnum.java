package com.neblannamaria.openlibraryes.enums;

public enum IndexEnum {
	AUTHOR("authors"),
	WORK("works"),
	EDITION("editions");

	public final String label;

	IndexEnum(String label) {
		this.label = label;
	}

}
