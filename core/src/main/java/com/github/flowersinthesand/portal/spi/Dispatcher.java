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

import com.github.flowersinthesand.portal.Reply;
import com.github.flowersinthesand.portal.Socket;

public interface Dispatcher {

	Set<Handler> handlers(String type);

	void on(String type, Object bean, Method method);

	void fire(String type, Socket socket);

	void fire(String type, Socket socket, Object data);

	void fire(String type, Socket socket, Object data, int eventIdForReply);

	interface Handler {
		
		int order();

		void handle(Socket socket, Object data, Reply.Fn reply);

	}

	interface Evaluator {

		Object evaluate(Map<String, Object> root, String expression);

	}

}