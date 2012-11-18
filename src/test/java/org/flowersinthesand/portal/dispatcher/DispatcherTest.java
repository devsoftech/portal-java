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
package org.flowersinthesand.portal.dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowersinthesand.portal.Fn;
import org.flowersinthesand.portal.Fn.Callback;
import org.flowersinthesand.portal.Fn.Callback1;
import org.flowersinthesand.portal.Socket;
import org.flowersinthesand.portal.dispatcher.Dispatcher.Invoker;
import org.flowersinthesand.portal.handler.DispatchHandler;
import org.junit.Assert;
import org.mockito.Mockito;
import org.testng.annotations.Test;

public class DispatcherTest {

	String url = "/dispatch";

	@Test
	public void staticBinding() throws SecurityException, NoSuchMethodException,
			InstantiationException, IllegalAccessException {
		DispatchHandler h = new DispatchHandler();

		Dispatcher dispatcher = new Dispatcher();
		dispatcher.on(url, "load", h, h.getClass().getMethod("onLoad"));

		Map<String, Map<String, Set<Invoker>>> events = dispatcher.events();
		Assert.assertNotNull(events.get(url));

		Map<String, Set<Invoker>> event = events.get(url);
		Assert.assertNotNull(event.get("load"));
	}

	@Test
	public void dynamicBinding() {
		Dispatcher dispatcher = new Dispatcher();
		dispatcher.on(url, "e1", null, new Fn.Callback() {
			@Override
			public void call() {}
		});
		dispatcher.on(url, "e2", null, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {}
		});
		dispatcher.on(url, "e3", null, new Fn.Callback2<Object, Fn.Callback>() {
			@Override
			public void call(Object arg1, Callback reply) {}
		});
		dispatcher.on(url, "e4", null, new Fn.Callback2<Object, Fn.Callback1<Object>>() {
			@Override
			public void call(Object arg1, Callback1<Object> reply) {}
		});

		Map<String, Map<String, Set<Invoker>>> events = dispatcher.events();
		Assert.assertNotNull(events.get(url));

		Map<String, Set<Invoker>> event = events.get(url);
		Assert.assertNotNull(event.get("e1"));
		Assert.assertNotNull(event.get("e2"));
		Assert.assertNotNull(event.get("e3"));
		Assert.assertNotNull(event.get("e4"));
	}

	@Test
	public void staticFiring() throws SecurityException, NoSuchMethodException {
		DispatchHandler h = new DispatchHandler();
		Class<?> clazz = h.getClass(); 
		Socket socket = Mockito.mock(Socket.class);
		final Object data = Mockito.mock(Object.class);

		Dispatcher dispatcher = new Dispatcher();
		dispatcher.on(url, "socket", h, clazz.getMethod("onSocket", Socket.class));
		dispatcher.fire(url, "socket", socket);
		Assert.assertArrayEquals(new Object[] { socket }, h.args);

		dispatcher.on(url, "data", h, clazz.getMethod("onData", Object.class));
		dispatcher.fire(url, "data", socket, data);
		Assert.assertArrayEquals(new Object[] { data }, h.args);

		dispatcher.on(url, "repli", h, clazz.getMethod("onRepli", Fn.Callback.class));
		dispatcher.fire(url, "repli", socket, data, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				Assert.assertNull(arg1);
			}
		});
		Assert.assertTrue(h.args[0] instanceof Fn.Callback);

		dispatcher.on(url, "repli-data", h, clazz.getMethod("onRepliData", Fn.Callback1.class, Object.class));
		dispatcher.fire(url, "repli-data", socket, data, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				Assert.assertSame(data, arg1);
			}
		});
		Assert.assertTrue(h.args[0] instanceof Fn.Callback1);

		dispatcher.on(url, "socket-data-repli", h, clazz.getMethod("onSocketDataRepli", Socket.class, Object.class, Fn.Callback1.class));
		dispatcher.fire(url, "socket-data-repli", socket, data, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				Assert.assertSame(data, arg1);
			}
		});
		Assert.assertSame(socket, h.args[0]);
		Assert.assertSame(data, h.args[1]);
		Assert.assertTrue(h.args[2] instanceof Fn.Callback1);
	}

	@Test
	public void dynamicFiring() {
		final List<Object> theNumberOfAssertions = new ArrayList<Object>();
		final Socket socket = Mockito.mock(Socket.class);
		final Socket intruder = Mockito.mock(Socket.class);
		final Object data = Mockito.mock(Object.class);

		Dispatcher dispatcher = new Dispatcher();
		dispatcher.on(url, "signal", socket, new Fn.Callback() {
			@Override
			public void call() {
				theNumberOfAssertions.add(null);
				Assert.assertTrue(true);
			}
		});
		dispatcher.on(url, "signal", intruder, new Fn.Callback() {
			@Override
			public void call() {
				theNumberOfAssertions.add(null);
				Assert.assertTrue(false);
			}
		});
		dispatcher.fire(url, "signal", socket);

		dispatcher.on(url, "data", socket, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				theNumberOfAssertions.add(null);
				Assert.assertSame(arg1, data);
			}
		});
		dispatcher.fire(url, "data", socket, data);
		
		dispatcher.on(url, "repli1", socket, new Fn.Callback2<Object, Fn.Callback>() {
			@Override
			public void call(Object arg1, Fn.Callback reply) {
				theNumberOfAssertions.add(null);
				Assert.assertSame(arg1, data);
				reply.call();
			}
		});
		dispatcher.fire(url, "repli1", socket, data, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				Assert.assertNull(arg1);
			}
		});

		dispatcher.on(url, "repli2", socket, new Fn.Callback2<Object, Fn.Callback1<Object>>() {
			@Override
			public void call(Object arg1, Fn.Callback1<Object> reply) {
				theNumberOfAssertions.add(null);
				Assert.assertSame(arg1, data);
				reply.call(arg1);
			}
		});
		dispatcher.fire(url, "repli2", socket, data, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				Assert.assertSame(arg1, data);
			}
		});

		Assert.assertEquals(theNumberOfAssertions.size(), 4);
	}
	
}
