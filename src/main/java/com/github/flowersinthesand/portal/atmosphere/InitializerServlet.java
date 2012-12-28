/*
 * Copyright 2012 Donghwan Kim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.flowersinthesand.portal.atmosphere;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.App;
import com.github.flowersinthesand.portal.Initializer;

public class InitializerServlet extends AtmosphereServlet {

	private static final long serialVersionUID = -708330422123334651L;
	private final Logger logger = LoggerFactory.getLogger(InitializerServlet.class);

	@Override
	public void init(ServletConfig sc) throws ServletException {
		super.init(sc);
		
		List<File> files = new ArrayList<File>();
		files.add(new File(getServletContext().getRealPath("/WEB-INF/classes/")));
		files.addAll(Arrays.asList(new File(getServletContext().getRealPath("/WEB-INF/lib/")).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".jar");
			}
		})));

		try {
			Initializer i = new Initializer().init(files);
			for (App app : i.apps().values()) {
				getServletContext().setAttribute("com.github.flowersinthesand.portal.App#" + app.name(), app);
				framework.addAtmosphereHandler(app.name(), (AtmosphereHandler) app.socketManager());
			}
		} catch (IOException e) {
			logger.error("Failed to scan the class path", e);
		}
	}

}
