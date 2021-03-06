/**
 * ﻿Copyright (C) 2008 - 2014 52°North Initiative for Geospatial Open Source
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
package org.n52.ses.eml.v001.filter.temporal;

import javax.xml.namespace.QName;

import org.n52.ses.eml.v001.filter.IFilterElement;

import net.opengis.fes.x20.TemporalOpsType;

/**
 * Factory that builds the temporal filter objects.
 *
 */
public class TemporalFilterFactory {
	
	private static final String FES_NAMESPACE = "http://www.opengis.net/fes/2.0";
	
	private static final QName BEFORE_QNAME = new QName(FES_NAMESPACE, "Before");
	private static final QName AFTER_QNAME = new QName(FES_NAMESPACE, "After");
	private static final QName MEETS_QNAME = new QName(FES_NAMESPACE, "Meets");
	private static final QName MET_BY_QNAME = new QName(FES_NAMESPACE, "MetBy");
	private static final QName ANY_INTERACTS_QNAME = new QName(FES_NAMESPACE, "AnyInteracts");

	
	/**
	 * Builds the temporal filter objects
	 * 
	 * @param temporalOps FES temporal operator
	 * 
	 * @return object representing the temporal operator
	 */
	//TODO: property names not necessary (compare to ALogicalFilter.FACTORY)?
	public IFilterElement buildTemporalFilter(TemporalOpsType temporalOps) {
		QName tOpName = temporalOps.newCursor().getName();
		
		if (tOpName.equals(BEFORE_QNAME)) {
			return new BeforeFilter(temporalOps);
		}
		
		if (tOpName.equals(AFTER_QNAME)) {
			return new AfterFilter(temporalOps);
		}
		
		if (tOpName.equals(MEETS_QNAME)) {
			return new MeetsFilter(temporalOps);
		}
		
		if (tOpName.equals(MET_BY_QNAME)) {
			return new MetByFilter(temporalOps);
		}
		if (tOpName.equals(ANY_INTERACTS_QNAME)) {
			return new AnyInteractsFilter(temporalOps);
		}
		
		return null;
	}

}
