/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.blazebit.query.connector.base.ConventionContext;

import java.lang.reflect.Member;

/**
 * Convention context for Scaleway model records.
 * Allows full recursive traversal of all component accessor methods.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayConventionContext implements ConventionContext {

	public static final ConventionContext INSTANCE = new ScalewayConventionContext();

	private ScalewayConventionContext() {
	}

	@Override
	public ConventionContext getSubFilter(Class<?> concreteClass, Member member) {
		return this;
	}
}
