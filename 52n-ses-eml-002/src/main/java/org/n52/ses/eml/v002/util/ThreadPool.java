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

package org.n52.ses.eml.v002.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.n52.ses.util.concurrent.NamedThreadFactory;

/**
 * ThreadPool for the execution of {@link Runnable}s.
 * Implemented as Singleton. 
 * 
 * @author Thomas Everding
 *
 */
public class ThreadPool {
	
	private static ThreadPool instance = null;
	
	private ExecutorService executor;
	
	/**
	 * 
	 * Private Constructor
	 *
	 */
	private ThreadPool() {
		this.executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("EML002-UpdateHanderPool"));
	}
	
	
	/**
	 * 
	 * @return the only instance of this class
	 */
	public static synchronized ThreadPool getInstance() {
		if (instance == null) {
			instance = new ThreadPool();
		}
		
		return instance;
	}
	
	
	/**
	 * Executes a class implementing {@link Runnable}.
	 * Does not block.
	 * 
	 * @param runnable the runnable
	 */
	public synchronized void execute(Runnable runnable) {
		this.executor.submit(runnable);
	}
}
