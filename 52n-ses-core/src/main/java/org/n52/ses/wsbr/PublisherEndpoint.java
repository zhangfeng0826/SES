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
// 
// MyCapability.java
// Wed May 21 14:00:09 CEST 2008
// Generated by the Apache Muse Code Generation Tool
// 
package org.n52.ses.wsbr;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import net.opengis.sensorML.x101.IoComponentPropertyType;
import net.opengis.sensorML.x101.SensorMLDocument;
import net.opengis.sensorML.x101.SystemType;
import net.opengis.sensorML.x101.OutputsDocument.Outputs.OutputList;
import net.opengis.sensorML.x101.SensorMLDocument.SensorML.Member;

import org.apache.muse.core.Resource;
import org.apache.muse.util.xml.XmlUtils;
import org.apache.muse.ws.addressing.EndpointReference;
import org.apache.muse.ws.addressing.soap.SoapFault;
import org.apache.muse.ws.notification.NotificationProducer;
import org.apache.muse.ws.resource.impl.AbstractWsResourceCapability;
import org.apache.muse.ws.resource.lifetime.ScheduledTermination;
import org.apache.muse.ws.resource.lifetime.faults.ResourceNotDestroyedFault;
import org.apache.xmlbeans.XmlObject;
import org.n52.oxf.xmlbeans.parser.XMLBeansParser;
import org.n52.oxf.xmlbeans.parser.XMLHandlingException;
import org.n52.ses.api.common.SensorMLConstants;
import org.n52.ses.api.common.SesConstants;
import org.n52.ses.api.common.WsbrConstants;
import org.n52.ses.api.event.DataTypesMap;
import org.n52.ses.api.ws.IDestroyRegistration;
import org.n52.ses.api.ws.IPublisherEndpoint;
import org.n52.ses.persistency.SESFilePersistence;
import org.n52.ses.util.common.ConfigurationRegistry;
import org.n52.ses.wsn.SESSubscriptionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * @author Jan Torben Heuer <jan.heuer@uni-muenster.de>
 * 
 * The PRM capability
 */
public class PublisherEndpoint extends AbstractWsResourceCapability implements IPublisherEndpoint, IDestroyRegistration {
	private static final Logger logger = LoggerFactory.getLogger(PublisherEndpoint.class);

	private Element sensorML;
	private QName[] topics;
	private static String demand = "foo";

	QName[] PROPERTIES = new QName[] { WsbrConstants.CREATION_QNAME, WsbrConstants.PUBLISHER_REFERENCE_QNAME, WsbrConstants.DEMAND, WsbrConstants.TOPIC };

	// WsbrConstants.CREATION_QNAME, , ,
	// ,
	@Override
	public Element renewRegistration(Element terminationTime) throws Exception {
		Date date = XmlUtils.getDate(terminationTime);

		ScheduledTermination scheduledTermination = (ScheduledTermination) getResource().getCapability(
				"http://docs.oasis-open.org/wsrf/rlw-2/ScheduledResourceTermination");
		scheduledTermination.setTerminationTime(date);

		Element root = XmlUtils.createElement(SesConstants.RENEW_REGISTRATION_RESPONSE);
		// TODO: use joda time (?)
		XmlUtils.setElementText(root, new SimpleDateFormat().format(date));
		return root;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.n52.ses.wsrp.IDestroyRegistration#destroyRegistration(org.w3c.dom.Element)
	 * 
	 */
	@Override
	public Element destroyRegistration(Element something) throws ResourceNotDestroyedFault {
		// see: SimpleImmediateTermination
		try {
			//
			// all we're doing is making the shutdown task public...
			//
			getResource().shutdown();
		}

		catch (SoapFault fault) {
			throw new ResourceNotDestroyedFault(fault);
		}

		Element root = XmlUtils.createElement(SesConstants.DESTROY_REGISTRATION_RESPONSE);
		return root;
	}

	@Override
	public QName[] getPropertyNames() {
		return this.PROPERTIES;
	}

	@Override
	public void initialize() throws SoapFault {
		super.initialize();
		
		/* 
		 * resubscribe
		 */
		if (ConfigurationRegistry.getInstance().persistencyEnabled()) {

			Element persReg = getResource().getEndpointReference().getParameter(
					SESFilePersistence.SES_REGPUB_PERS_NAME);

			/* if no PersistentSubscribe than its an actual new subscribe */
			if (persReg != null) {
				this.sensorML = (Element) persReg.getElementsByTagNameNS(
						SensorMLConstants.NAMESPACE,
						SensorMLConstants.SENSORML.getLocalPart()).item(0);
				
				try {
					registerSensorML(this.sensorML);
				} catch (XMLHandlingException e) {
					logger.warn(e.getMessage(), e);
				}
				
				//TODO: topic, demand, termination time
				ConfigurationRegistry.getInstance().getGlobalRegisterPublisher().reRegisterPublisher(
						getResource().getEndpointReference(), null, false, null, this.sensorML, this);
				
				/*
				 * call the registry. this informs the waiting Subscribe-Reload-Thread
				 * of a new Publishers instance. If all instances have been reloaded
				 * the Subscribe-Reload-Thread continues its work.
				 */
				ConfigurationRegistry.getInstance().addReregisteredPublisher(this);
			}
		}

	}

	/**
	 * looks up the <em>first</em> {@link NotificationProducer} instance
	 * 
	 * @return
	 * @throws SoapFault
	 */
	@SuppressWarnings("unchecked")
	private Resource getNotificationProducer() throws SoapFault {
		Resource wsn = null;
		try {
			Iterator<EndpointReference> it = getResource().getResourceManager().getResourceEPRs();
			EndpointReference ref;
			while(it.hasNext()) {
				ref = it.next();
				if(ref.getAddress().toString().endsWith(SESSubscriptionManager.CONTEXT_PATH)) {
					wsn = getResource().getResourceManager().getResource(ref);
					break;
				}
			}
			
			if (wsn == null) {
				throw new SoapFault("Cannot find the NotificationProducer '" + SESSubscriptionManager.CONTEXT_PATH + "'!");
			}
		} catch (RuntimeException e) {
			throw new SoapFault("Invalid NotificationProducer returned: " + (wsn != null ? wsn.getClass().getSimpleName() : ""), e);
		}
		return wsn;

	}

	@Override
	public void initializeCompleted() throws SoapFault {
		super.initializeCompleted();
	}

	/**
	 * 
	 * 
	 * @return Capability creation time
	 */
	public Date getCreationTime() {
		return new Date();
	}

	/**
	 * @return the notification producer then Publisher must use to send events
	 */
	public EndpointReference getPublisherReference() {
		return getWsResource().getEndpointReference();
	}

	/**
	 * @return the notification producer then Publisher must use to send events
	 */
	public EndpointReference getNotificationProducerReference() {

		EndpointReference publisherEpr = null;
		try {
			publisherEpr = getNotificationProducer().getEndpointReference();
		} catch (Exception e) {
			logger.warn("Cannot find the NotificationProducer", e);
		}

		return publisherEpr;

	}

	/**
	 * Capability: demand
	 * 
	 * @return if the publisher is in on demand mode
	 */
	public Object getDemand() {
		// TODO: implement
		return demand;
	}

	/**
	 * Capability: SensorML
	 * 
	 * @return the SensorML description of the publisher
	 */
	public Element getSensorML() {
		return this.sensorML;
	}

	/**
	 * Sets the sensorML document. Warning, there is no validation!
	 * 
	 * @param sensorML the SensorML document
	 */
	public void setSensorML(Element sensorML) {
		this.sensorML = sensorML;
	}
	
	/**
	 * Registers SenorML Data. including registering phenomenons at
	 * the DataTypesMap (needed for subscriptions).
	 * 
	 * @param sml the SensorML document
	 * @throws InvalidXMLContentException if an error occurred on registering
	 */
	public void registerSensorML(Element sml) throws XMLHandlingException {
		this.sensorML = sml;
		
		XmlObject smlDoc = XMLBeansParser.parse(sml, true);
		
		if (smlDoc != null) {
			logger.info("Registering SensorML for new Publisher...");
			
			Member[] members = ((SensorMLDocument) smlDoc).getSensorML().getMemberArray();
			List<XmlObject> systems = new ArrayList<XmlObject>();
			
			/*
			 * select all System elements.
			 */
			for (Member member : members) {
				systems.addAll(Arrays.asList(member.selectChildren(new
						QName("http://www.opengis.net/sensorML/1.0.1", "System"))));
			}

			for (XmlObject sys : systems) {
				SystemType sysDoc = (SystemType) sys;
				
				//TODO DataRecord -> auslagern
				
				OutputList outs = sysDoc.getOutputs().getOutputList();
			
				/*
				 * check outputs for possible data types
				 */
				for (IoComponentPropertyType out : outs.getOutputArray()) {
					//TODO: out.isSetAbstractDataRecord()
					if (out.isSetQuantity()) {
						DataTypesMap.getInstance().registerNewDataType(out.getName(), Double.class);
					}
				}
				
			}
		}
	}

	/**
	 * The unique sensor-id from the sensorML
	 * 
	 * @return the id as string
	 * @throws SoapFault if an error occurred on retrieving the ID
	 */
	public String getSensorId() throws SoapFault {
		/* traverse xml tree */
		try {
			if (this.sensorML == null) {
				logger.info("SensorML is null");
				throw new SoapFault("SensorML document hasn't been supplied for this sensor!");
			}
			// Element system = XmlUtils.findFirstInSubTree(sensorML,
			// SensorMLConstants.SYSTEM);
			Element identification = XmlUtils.findFirstInSubTree(this.sensorML, SensorMLConstants.IDENTIFICATION);

			Element term = XmlUtils.findFirstInSubTree(identification, SensorMLConstants.TERM);
			if (SensorMLConstants.SENSOR_ID_UNIQUEIDS.contains(term.getAttribute("definition"))) {
				Element value = XmlUtils.getElement(term, SensorMLConstants.VALUE);
				Node text = value.getFirstChild();
				if(text instanceof Text) {
					return ((Text) text).getData();
				}
			}
		} catch (DOMException e) {
			logger.warn(e.getMessage());
		} catch (NullPointerException e) {
			logger.warn(e.getMessage());
		} catch (Exception e) {
			logger.warn("Unrecoverable error: ", e);
		}

		return "invalid-id";
	}

	/**
	 * Capability: Topic
	 * 
	 * @return the topics of this publisher
	 */
	public QName[] getTopic() {
		return this.topics;
	}

	/**
	 * Sets this resource's topic. Used by the {@link RegisterPublisher} class.
	 * 
	 * @param topic the topic
	 */
	public void setTopic(QName[] topic) {
		this.topics = topic;
	}

	@Override
	public void reRegister() {
		
	}
}