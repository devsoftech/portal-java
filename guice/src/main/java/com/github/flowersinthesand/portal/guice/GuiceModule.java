/*
 * Copyright 2012-2013 Donghwan Kim
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
package com.github.flowersinthesand.portal.guice;

import com.github.flowersinthesand.portal.Options;
import com.github.flowersinthesand.portal.spi.Module;
import com.github.flowersinthesand.portal.spi.ObjectFactory;
import com.google.inject.Injector;

public class GuiceModule implements Module {

	private Injector injector;

	public GuiceModule(Injector injector) {
		this.injector = injector;
	}

	@Override
	public void configure(Options options) {
		options.bean(ObjectFactory.class.getName(), new GuiceObjectFactory(injector));
	}

}
