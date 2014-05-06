package com.steeleforge.aem.aemin;

import com.google.javascript.jscomp.CompilationLevel;

public enum ClosureOptimizationLevel {
	NONE, WHITESPACE, SIMPLE, ADVANCED;
	public static ClosureOptimizationLevel fromString(String name) {
		if (null != name) {
			for(ClosureOptimizationLevel level : ClosureOptimizationLevel.values()) {
				if (name.equals(level.name().toString())) {
					return level;
				}
			}
		}
		return ClosureOptimizationLevel.NONE;
	}
	public CompilationLevel toCompilationLevel() {
		switch(this) {
			case WHITESPACE:
				return CompilationLevel.WHITESPACE_ONLY;
			case SIMPLE:
				return CompilationLevel.SIMPLE_OPTIMIZATIONS;
			case ADVANCED:
				return CompilationLevel.ADVANCED_OPTIMIZATIONS;
			default:
				return null;
		}
	}
}
