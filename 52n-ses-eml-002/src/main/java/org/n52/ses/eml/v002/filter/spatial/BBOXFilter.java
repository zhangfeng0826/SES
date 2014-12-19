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
package org.n52.ses.eml.v002.filter.spatial;



import org.apache.xmlbeans.XmlException;
import org.n52.ses.io.parser.GML32Parser;
import org.n52.ses.api.event.MapEvent;
import org.n52.ses.api.exception.GMLParseException;
import org.n52.ses.eml.v002.filterlogic.esper.customFunctions.MethodNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.opengis.fes.x20.BBOXType;
import net.opengis.gml.x32.EnvelopeType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;


/**
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class BBOXFilter extends ASpatialFilter {

	private BBOXType bboxOperator;
	
	private static final String GML_NAMESPACE = "http://www.opengis.net/gml/3.2";

	private static final String	ENVELOPE_NAME = "Envelope";
	
	private static final Logger logger = LoggerFactory
			.getLogger(BBOXFilter.class);

	/**
	 * 
	 * Constructor
	 *
	 * @param bboxOp FES bounding box
	 */
	public BBOXFilter(BBOXType bboxOp) {
		this.bboxOperator = bboxOp;
	}

	@Override
	public String createExpressionString(boolean complexPatternGuard) {
		Geometry geom = null;
		try {
			//get Envelope
			Node bbNode = this.bboxOperator.getDomNode();
			NodeList nodes = bbNode.getChildNodes();
			Node envNode = null;
			
			for (int i = 0; i < nodes.getLength(); i++) {
				if (!nodes.item(i).getNamespaceURI().equals(GML_NAMESPACE)) {
					continue;
				}
				if (!nodes.item(i).getLocalName().equals(ENVELOPE_NAME)) {
					continue;
				}
				//envelope found
				envNode = nodes.item(i);
			}
			
			//parse Envelope
			EnvelopeType envelope = EnvelopeType.Factory.parse(envNode);
			geom = GML32Parser.parseGeometry(envelope);
		} catch (ParseException e) {
			logger.warn(e.getMessage(), e);
		} catch (GMLParseException e) {
			throw new UnsupportedOperationException(e);
		}
		catch (XmlException e) {
			logger.warn("could not parse the envelope: " + e.getMessage());
		}
		
		if (geom == null) {
			//error while creating geometry
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(MethodNames.SPATIAL_METHODS_PREFIX);
		sb.append("bbox(");
		sb.append(MethodNames.SPATIAL_METHODS_PREFIX);
		//create WKT from corners
		sb.append("fromWKT(\""+ geom.toText() +"\")");
		sb.append(", ");
		sb.append(MapEvent.GEOMETRY_KEY +")");
		
		return sb.toString();
	}

	@Override
	public void setUsedProperty(String nodeValue) {
		/*empty*/
	}

}
