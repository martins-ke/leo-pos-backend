package com.leo.pos.util;

public class IdGenarator {

	public static String generateId(String name, long count) { 
		String prefix = "REC-";
		if (name != null && !name.trim().isEmpty()) {
			String clean = name.trim();
			if (clean.length() >= 2) {
				prefix = clean.substring(0, 2).toUpperCase() + "-";
			} else {
				prefix = (clean.toUpperCase() + "X-");
			}
		}
		return prefix + String.format("%04d", count + 1); 
	}
}
