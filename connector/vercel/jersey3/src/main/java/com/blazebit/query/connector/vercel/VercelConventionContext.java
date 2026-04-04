/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.blazebit.query.connector.base.ConventionContext;

import java.lang.reflect.Member;

/**
 * A method filter for the Vercel connector model classes.
 *
 * @author Blazebit
 * @since 1.0.0
 */
public class VercelConventionContext implements ConventionContext {

	public static final ConventionContext INSTANCE = new VercelConventionContext();

	private VercelConventionContext() {
	}

	@Override
	public ConventionContext getSubFilter(Class<?> concreteClass, Member member) {
		return this;
	}
}
