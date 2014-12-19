/**
 * ﻿Copyright (C) 2012 - 2014 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * icense version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
/**
 * Part of the diploma thesis of Thomas Everding.
 * @author Thomas Everding
 */

package org.n52.ses.eml.v001.filter;

import java.util.List;

import org.n52.ses.api.common.CustomStatementEvent;

/**
 * Standard methods for all filter elements
 * 
 * @author Thomas Everding
 * 
 */
public interface IFilterElement {
	
	/**
	 * Creates the esper String for this expression
	 * 
	 * @param complexPatternGuard if <code>true</code> the guard is used for a complex pattern, else for a simple
	 * pattern
	 * @return the esper string for this expression
	 */
	public String createExpressionString(boolean complexPatternGuard);

	/**
	 * Sets if a property is used in a filter statement. It has to be checked if it exists.
	 * 
	 * @param nodeValue the property name
	 */
	public void setUsedProperty(String nodeValue);

	/**
	 * @return a list of custom events to be triggered when this filter element finds a match
	 */
	List<CustomStatementEvent> getCustomStatementEvents();	
}
