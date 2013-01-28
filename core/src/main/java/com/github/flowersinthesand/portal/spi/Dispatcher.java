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
package com.github.flowersinthesand.portal.spi;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import com.github.flowersinthesand.portal.Fn;
import com.github.flowersinthesand.portal.Socket;

public interface Dispatcher {

	Map<String, Set<Handler>> handlers();

	void on(String event, Object bean, Method method);

	void fire(String event, Socket socket);

	void fire(String event, Socket socket, Object data);

	void fire(String event, Socket socket, Object data, Fn.Callback1<?> reply);

	interface Handler {

		void handle(Socket socket, Object data, Fn.Callback1<?> reply);

	}

	interface Evaluator {

		Object evaluate(Map<String, Object> root, String expression);

	}

}