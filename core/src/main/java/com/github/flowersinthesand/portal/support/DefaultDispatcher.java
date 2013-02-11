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
package com.github.flowersinthesand.portal.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.Data;
import com.github.flowersinthesand.portal.Fn;
import com.github.flowersinthesand.portal.Fn.Callback1;
import com.github.flowersinthesand.portal.Order;
import com.github.flowersinthesand.portal.Reply;
import com.github.flowersinthesand.portal.Socket;
import com.github.flowersinthesand.portal.Throw;
import com.github.flowersinthesand.portal.Wire;
import com.github.flowersinthesand.portal.spi.Dispatcher;

@Bean("dispatcher")
public class DefaultDispatcher implements Dispatcher {

	private final Logger logger = LoggerFactory.getLogger(DefaultDispatcher.class);
	private Map<String, SortedSet<Dispatcher.Handler>> handlers = new LinkedHashMap<String, SortedSet<Dispatcher.Handler>>();
	@Wire
	private Evaluator evaluator;

	@Override
	public Set<Dispatcher.Handler> handlers(String type) {
		return handlers.containsKey(type) ? Collections.unmodifiableSet(handlers.get(type)) : null;
	}

	@Override
	public void on(String type, Object bean, Method method) {
		logger.debug("Attaching the '{}' event from '{}'", type, method);
		
		Dispatcher.Handler handler;
		try {
			handler = new DefaultHandler(bean, method);
		} catch (IllegalArgumentException e) {
			throw e;
		}
		
		if (!handlers.containsKey(type)) {
			handlers.put(type, new TreeSet<Dispatcher.Handler>(new Comparator<Dispatcher.Handler>() {
				@Override
				public int compare(Handler o1, Handler o2) {
					return o1.order() > o2.order() ? 1 : -1;
				}
			}));
		}

		handlers.get(type).add(handler);
	}

	@Override
	public void fire(String type, Socket socket) {
		fire(type, socket, null);
	}

	@Override
	public void fire(String type, Socket socket, Object data) {
		fire(type, socket, data, 0);
	}

	@Override
	public void fire(String type, final Socket socket, Object data, final int eventIdForReply) {
		logger.debug("Firing {} event to Socket#{}", type, socket.id());
		Fn.Callback1<?> reply = eventIdForReply > 0 ? new Fn.Callback1<Map<String, Object>>() {
			@Override
			public void call(Map<String, Object> data) {
				data.put("id", eventIdForReply);
				logger.debug("Sending the reply event with the data {}", data);
				socket.send("reply", data);
			}
		} : null;
				
		if (handlers.containsKey(type)) {
			for (Dispatcher.Handler handler : handlers.get(type)) {
				logger.trace("Invoking handler {}", handler);
				handler.handle(socket, data, reply);
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	class DefaultHandler implements Dispatcher.Handler {

		int order = 0;
		ObjectMapper mapper = new ObjectMapper();
		Object bean;
		Method method;
		Param[] params;
		Class<?>[] throwables;
		
		DefaultHandler(Object bean, Method method) {
			this.bean = bean;
			this.method = method;
			
			if (method.isAnnotationPresent(Order.class)) {
				this.order = method.getAnnotation(Order.class).value();
			}
			
			if (method.isAnnotationPresent(Throw.class)) {
				throwables = method.getAnnotation(Throw.class).value();
				if (throwables.length == 0) {
					throwables = method.getExceptionTypes();
					if (throwables.length == 0) {
						throw new IllegalArgumentException("Both @Throw.value and exception types of method '" + method + "' are empty");
					}
				}
			}

			Class<?>[] paramTypes = method.getParameterTypes();
			Annotation[][] paramAnnotations = method.getParameterAnnotations();
			params = new Param[paramTypes.length];
			
			for (int i = 0; i < paramTypes.length; i++) {
				Class<?> paramType = paramTypes[i];
				if (paramType.isAssignableFrom(Socket.class)) {
					params[i] = new SocketParam();					
				}
				for (Annotation annotation : paramAnnotations[i]) {
					if (Data.class.equals(annotation.annotationType())) {
						params[i] = new DataParam(paramType, (Data) annotation);
					}
					if (Reply.class.equals(annotation.annotationType())) {
						if (!Fn.Callback.class.equals(paramType) && !Fn.Callback1.class.equals(paramType)) {
							throw new IllegalArgumentException(
								"@Reply must be present either on Fn.Callback or Fn.Callback1 not '" + paramType + "' in '" + method + "'");
						}
						params[i] = new ReplyParam(paramType);
					}
				}
			}
			
			int socketIndex = -1;
			int replyIndex = -1;
			List<String> expressions = new ArrayList<String>();
			for (int i = 0; i < params.length; i++) {
				Param arg = params[i];
				if (arg == null) {
					throw new IllegalArgumentException(
						"Paramters[" + i + "] '" + method.getParameterTypes()[i] + "' of method '" + method + "' cannot be resolved");
				} else if (arg instanceof SocketParam) {
					if (socketIndex != -1) {
						throw new IllegalArgumentException(
							"Socket is duplicated at '" + socketIndex + "' and '" + i + "' index in parameters of '" + method + "'");
					}
					socketIndex = i;
				} else if (arg instanceof DataParam) {
					String value = ((DataParam) arg).ann.value();
					if (expressions.contains(value)) {
						throw new IllegalArgumentException("@Data(\"" + value + "\") is duplicated in paramters of '" + method + "'");
					}
					expressions.add(value);
				} else if (arg instanceof ReplyParam) {
					if (replyIndex != -1) {
						throw new IllegalArgumentException(
							"@Reply is duplicated at '" + replyIndex + "' and '" + i + "' index in parameters of '" + method + "'");
					}
					if (method.isAnnotationPresent(Reply.class)) {
						throw new IllegalArgumentException("@Reply is alread annotated to the method '" + method + "'");
					}
				}
			}
		}

		@Override
		public int order() {
			return order;
		}

		@Override
		public void handle(Socket socket, Object data, Callback1 reply) {
			Object[] args = new Object[params.length];
			for (int i = 0; i < params.length; i++) {
				args[i] = params[i].resolve(socket, data, reply);
			}


			Map<String, Object> result = new LinkedHashMap<String, Object>();
			result.put("exception", false);
			
			try {
				result.put("data", method.invoke(bean, args));
			} catch (InvocationTargetException e) {
				Throwable ex = e.getCause();
				if (throwables != null) {
					for (Class<?> throwable : throwables) {
						if (ex.getClass().isAssignableFrom(throwable)) {
							Map<String, Object> map = new LinkedHashMap<String, Object>();
							map.put("type", ex.getClass().getName());
							map.put("message", ex.getMessage());
							result.put("data", map);
							result.put("exception", true);
							break;
						}
					}
				}
				if (!result.containsKey("data")) {
					throw new RuntimeException(e);
				}
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}

			if (method.isAnnotationPresent(Reply.class)) {
				reply.call(result);
			}
		}
		
		abstract class Param {
			abstract Object resolve(Socket socket, Object data, Callback1<?> reply);
		}
		
		class SocketParam extends Param {
			@Override
			Object resolve(Socket socket, Object data, Callback1<?> reply) {
				return socket;
			}
		}

		class DataParam extends Param {
			Class<?> type;
			Data ann;

			public DataParam(Class<?> type, Data ann) {
				this.type = type;
				this.ann = ann;
			}

			@Override
			Object resolve(Socket socket, Object data, Callback1<?> reply) {
				if (!ann.value().equals("")) {
					if (!(data instanceof Map)) {
						throw new IllegalArgumentException("@Data(\"" + ann.value() + "\") must work with Map not '" + data + "'");
					}
					data = evaluator.evaluate((Map<String, Object>) data, ann.value());
				}
				
				return mapper.convertValue(data, type);
			}
		}

		class ReplyParam extends Param {
			Class<?> type;

			public ReplyParam(Class<?> type) {
				this.type = type;
			}

			@Override
			Object resolve(Socket socket, Object data, final Callback1 reply) {
				return Fn.Callback.class.equals(type) ? new Fn.Callback() {
					@Override
					public void call() {
						Map<String, Object> result = new LinkedHashMap<String, Object>();
						result.put("data", null);
						result.put("exception", false);
						reply.call(result);
					}
				} : new Fn.Callback1<Object>() {
					@Override
					public void call(Object arg1) {
						Map<String, Object> result = new LinkedHashMap<String, Object>();
						result.put("data", arg1);
						result.put("exception", false);
						reply.call(result);
					}
				};
			}
		}

	}

	@Bean("dispatcher.Evaluator")
	public static class DefaultEvaluator implements Dispatcher.Evaluator {

		@Override
		public Object evaluate(Map<String, Object> root, String expression) {
			return root.get(expression);
		}

	}

}
