package com.eingsoft.emop.tc.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * scope description annotation, describe the class or method return object scope
 * 
 * @author beam
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ScopeDesc {

	/**
	 * the scope of the target
	 *
	 * @return the scope of the target class
	 */
	Scope[] value();

	public static enum Scope {
		/**
		 * Only one instance globally
		 */
		Singleton,
		/**
		 * Each request or thread will have different instance
		 */
		Request,
		/**
		 * Different instance for every retrieval
		 */
		Prototype,
		/**
		 * Same tcSessionId will have same instance
		 */
		TcSessionId,
		/**
		 * Same tc login credential will have same instance
		 */
		TcCredential,
		/**
		 * Same tcContextHolder will have the same instance
		 */
		TcContextHolder
	}
}
