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
package org.n52.ses.common.environment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.muse.core.platform.mini.MiniIsolationLayer;
import org.apache.muse.core.platform.mini.MiniServlet;
import org.apache.muse.util.xml.XmlUtils;
import org.apache.muse.ws.addressing.soap.SoapConstants;
import org.apache.muse.ws.addressing.soap.SoapFault;
import org.apache.muse.ws.addressing.soap.SoapUtils;
import org.n52.ses.api.ISESFilePersistence;
import org.n52.ses.common.environment.handler.GetRequestHandler;
import org.n52.ses.common.environment.handler.GetCapabilitiesHandler;
import org.n52.ses.common.environment.handler.WSDLProvisionHandler;
import org.n52.ses.common.environment.handler.XSDProvisionHandler;
import org.n52.ses.requestlogger.RequestLoggerWrapper;
import org.n52.ses.startupinit.StartupInitServlet;
import org.n52.ses.util.common.ConfigurationRegistry;
import org.n52.ses.wsbr.RegisterPublisher;
import org.n52.ses.wsn.SESNotificationProducer;
import org.n52.ses.wsn.SESSubscriptionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Extended Servlet with support for shutdown and wsdl provision.
 * 
 * @author Matthes Rieke
 *
 */
public class SESMiniServlet extends MiniServlet {


	private static final Logger logger = LoggerFactory.getLogger(SESMiniServlet.class);
	private static final long serialVersionUID = 1L;
	private String landingPage;
	private MiniIsolationLayer sesIsolationLayer;
	private List<GetRequestHandler> getRequestHandlers = new ArrayList<GetRequestHandler>();
	private static int minimumContentLengthForGzip = 500000;
	private static RequestLoggerWrapper loggerInst;
	private static final AtomicBoolean firstResponsePrint = new AtomicBoolean(true);

	public SESMiniServlet() {
		logger.info(readFileContents("/ses_version_info.txt"));
		this.getRequestHandlers.add(new GetCapabilitiesHandler());
		this.getRequestHandlers.add(new WSDLProvisionHandler());
		this.getRequestHandlers.add(new XSDProvisionHandler());
	}

	private String readFileContents(String string) {
		InputStream is = getClass().getResourceAsStream(string);
		
		if (is != null) {
			Scanner sc = new Scanner(is);
			StringBuilder sb = new StringBuilder();
			while (sc.hasNext()) {
				sb.append(sc.nextLine());
				sb.append(System.getProperty("line.separator"));
			}
			sb.deleteCharAt(sb.length()-1);
			sc.close();
			return sb.toString();
		}
		return null;
	}

	@Override
	public void destroy() {
		if (ConfigurationRegistry.getInstance() != null) {
			ConfigurationRegistry.getInstance().shutdown();
		}
		super.destroy();
	}



	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		synchronized (SESMiniServlet.class) {
			if (this.sesIsolationLayer == null) {
				this.sesIsolationLayer = createIsolationLayer(request, getServletContext());
			}	
		}

		InputStream input = request.getInputStream();
		Document soapRequest = null;

		long time = System.currentTimeMillis();

		try
		{
			soapRequest = XmlUtils.createDocument(input);
		}

		catch (Exception error)
		{
			logger.warn(error.getMessage(), error);
			SoapFault fault = SoapUtils.convertToFault(error);
			response.setContentType("application/soap+xml; charset=utf-8");
			printResponse(request, response, createSoapFaultEnvelope(fault));
			return;
		}

		handleSoapRequest(request, response, soapRequest);

		if (RequestLoggerWrapper.isActive()) {
			if (loggerInst == null)	{
				loggerInst = RequestLoggerWrapper.getInstance();
			}
			if (loggerInst != null)
				loggerInst.logRequest(time, soapRequest);
		}
	}

	private String createSoapFaultEnvelope(SoapFault fault) {
		Document response = XmlUtils.createDocument();

		Element soap = XmlUtils.createElement(response, SoapConstants.ENVELOPE_QNAME);
		response.appendChild(soap);


		Element body = XmlUtils.createElement(response, SoapConstants.BODY_QNAME);
		soap.appendChild(body);

		Element result = (Element) response.importNode(fault.toXML(), true);
		body.appendChild(result);


		return XmlUtils.toString(response);
	}

	private void handleSoapRequest(HttpServletRequest request, HttpServletResponse response,
			Document soapRequest) throws IOException {
		Document soapResponse;
		try {
			soapResponse = this.sesIsolationLayer.handleRequest(soapRequest);
		} catch (RuntimeException e) {
			throw new IOException(e);
		}

		/*
		 * is null? return a http response code.
		 * change made by Matthes Rieke <m.rieke@uni-muenster.de>
		 */
		if (soapResponse == null) {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
		} else {
			//TODO check support of ' ; charset=utf-8'
			response.setContentType("application/soap+xml; charset=utf-8");
			printResponse(request, response, XmlUtils.toString(soapResponse));
		}
	}


	private boolean clientSupportsGzip(HttpServletRequest request) {
		String header = request.getHeader("Accept-Encoding");
		if (header != null && !header.isEmpty()) {
			String[] split = header.split(",");
			for (String string : split) {
				if (string.equalsIgnoreCase("gzip")) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		ConfigurationRegistry conf = ConfigurationRegistry.getInstance();

		for (GetRequestHandler handler : this.getRequestHandlers) {
			if (handler.canHandle(req)) {
				try {
					printResponse(req, resp, handler.handleRequest(req, resp, conf, this.sesIsolationLayer));
				} catch (Exception e) {
					throw new ServletException(e);
				}
				return;
			}
		}

		provideLandingPage(req, resp, conf);
	}


	private void provideLandingPage(HttpServletRequest req,
			HttpServletResponse resp, ConfigurationRegistry conf)
					throws IOException, UnsupportedEncodingException {
		/*
		 * return landing page
		 */
		if (this.landingPage != null) {
			resp.setContentType("text/html");
			synchronized (this) {
				printResponse(req, resp, this.landingPage);
			}

			return;
		}

		InputStreamReader isr = new InputStreamReader(getClass().getResourceAsStream("/landing_page.html"));
		BufferedReader br = new BufferedReader(isr);
		StringBuilder sb = new StringBuilder();

		while (br.ready()) {
			sb.append(br.readLine());
		}
		String html = sb.toString();

		String reqUrl = URLDecoder.decode(req.getRequestURL().toString(),
				(req.getCharacterEncoding() == null ? Charset.defaultCharset().name() : req.getCharacterEncoding()));
		html = html.replace("[SES_URL]", reqUrl.substring(0,
				reqUrl.indexOf(req.getContextPath())) + req.getContextPath());

		/*
		 * check if we are init yet
		 */
		String sesPortTypeUrl, subMgrUrl, prmUrl = "";
		if (conf != null) {
			String defaulturi = conf.getEnvironment().getDefaultURI().substring(0,
					conf.getEnvironment().getDefaultURI().lastIndexOf("/services"));
			sesPortTypeUrl = defaulturi + "/services/" + SESNotificationProducer.CONTEXT_PATH;
			subMgrUrl = defaulturi + "/services/" + SESSubscriptionManager.CONTEXT_PATH;
			prmUrl = defaulturi + "/services/" + RegisterPublisher.RESOURCE_TYPE;

			conf.setSubscriptionManagerWsdl(subMgrUrl + "?wsdl");

			html = html.replace("<p id=\"ses-status\"><p>",
					"<p style=\"color:#0f0\">The service is active and available.</p>");

			html = html.replace("[GET_CAPS]", StringEscapeUtils.escapeHtml4(StartupInitServlet.getGetCapabilitiesRequest(sesPortTypeUrl)));
			/*
			 * replace the url
			 */
			synchronized (this) {
				if (this.landingPage == null) {
					this.landingPage = html.replace("[SES_PORT_TYPE_URL]", sesPortTypeUrl);
					this.landingPage = this.landingPage.replace("[SUB_MGR_URL]", subMgrUrl);
					this.landingPage = this.landingPage.replace("[PRM_URL]", prmUrl);	

					resp.setContentType("text/html");
					printResponse(req, resp, this.landingPage);
				}


			}

		}
		else {
			/*
			 * we do not have the config, warn the user
			 */
			html = html.replace("<p id=\"ses-status\"><p>",
					"<p style=\"color:#f00\">The service is currently not available due to unfinished or failed initialization.</p>");

			resp.setContentType("text/html");
			printResponse(req, resp, html);
		}
	}


	private void printResponse(HttpServletRequest request, HttpServletResponse response,
			String string) throws IOException {
		int contentLength = string.getBytes("UTF-8").length;

		if (firstResponsePrint.getAndSet(false)) {
			ConfigurationRegistry conf = ConfigurationRegistry.getInstance();
			if (conf == null) {
				firstResponsePrint.getAndSet(true);
			}
			else {
				minimumContentLengthForGzip = Integer.parseInt(conf.getPropertyForKey(
						ConfigurationRegistry.MINIMUM_GZIP_SIZE));	
			}
		}

		// compressed response
		if (contentLength > minimumContentLengthForGzip && clientSupportsGzip(request)) {
			response.addHeader("Content-Encoding", "gzip");
			GZIPOutputStream gzip = new GZIPOutputStream(response.getOutputStream(), contentLength);
			String type = response.getContentType();
			if (!type.contains("charset")) {
				response.setContentType(type + "; charset=utf-8");
			}
			gzip.write(string.getBytes(Charset.forName("UTF-8")));
			gzip.flush();
			gzip.finish();
		} 
		// uncompressed response
		else {
			response.setContentLength(contentLength);
			response.setCharacterEncoding("UTF-8");
			PrintWriter writer = response.getWriter();
			writer.write(string);
			writer.flush();
		}


	}


	@Override
	protected MiniIsolationLayer createIsolationLayer(
			HttpServletRequest request, ServletContext context) {
		MiniIsolationLayer isolationLayer = new SESMiniIsolationLayer(request, context);
		isolationLayer.initialize();
		
		ConfigurationRegistry.getInstance().setFilePersistence((ISESFilePersistence) isolationLayer.getRouter().getPersistence());
		
		return isolationLayer;
	}


}
