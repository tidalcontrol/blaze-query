/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.blazebit.query.connector.base.ConventionContext;

import java.lang.reflect.Member;

/**
 * Convention context for HubSpot record types.
 * Includes all record component accessors recursively.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class HubspotConventionContext implements ConventionContext {

	public static final ConventionContext INSTANCE = new HubspotConventionContext();

	private HubspotConventionContext() {
	}

	@Override
	public ConventionContext getSubFilter(Class<?> concreteClass, Member member) {
		return this;
	}
}
