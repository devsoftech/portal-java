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
package com.github.flowersinthesand.portal.spi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.github.flowersinthesand.portal.Fn;
import com.github.flowersinthesand.portal.Socket;
import com.github.flowersinthesand.portal.handler.DataBean;
import com.github.flowersinthesand.portal.handler.EventsHandler;
import com.github.flowersinthesand.portal.spi.DefaultDispatcher.EventHandler;

public class DispatcherTest {

	@Test
	public void binding() throws SecurityException, NoSuchMethodException,
			InstantiationException, IllegalAccessException {
		EventsHandler h = new EventsHandler();

		DefaultDispatcher dispatcher = new DefaultDispatcher();
		dispatcher.on("load", h, h.getClass().getMethod("onLoad"));

		Map<String, Set<EventHandler>> eventHandlers = dispatcher.eventHandlers();
		Assert.assertNotNull(eventHandlers.get("load"));
	}

	@Test
	public void firing() throws SecurityException, NoSuchMethodException {
		EventsHandler h = new EventsHandler();
		Class<?> clazz = h.getClass(); 
		Socket socket = Mockito.mock(Socket.class);
		
		Map<String, Object> before = new LinkedHashMap<String, Object>();
		before.put("number", 100);
		before.put("string", "String");
		final DataBean after = new DataBean();
		after.setNumber(100);
		after.setString("String");

		Dispatcher dispatcher = new DefaultDispatcher();
		dispatcher.on("socket", h, clazz.getMethod("onSocket", Socket.class));
		dispatcher.fire("socket", socket);
		Assert.assertArrayEquals(new Object[] { socket }, h.args);

		dispatcher.on("data", h, clazz.getMethod("onData", DataBean.class));
		dispatcher.fire("data", socket, before);
		Assert.assertArrayEquals(new Object[] { after }, h.args);

		dispatcher.on("repli", h, clazz.getMethod("onRepli", Fn.Callback.class));
		dispatcher.fire("repli", socket, before, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				Assert.assertNull(arg1);
			}
		});
		Assert.assertTrue(h.args[0] instanceof Fn.Callback);

		dispatcher.on("repli-data", h, clazz.getMethod("onRepliData", Fn.Callback1.class, DataBean.class));
		dispatcher.fire("repli-data", socket, before, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				Assert.assertEquals(after, arg1);
			}
		});
		Assert.assertTrue(h.args[0] instanceof Fn.Callback1);

		dispatcher.on("socket-data-repli", h, clazz.getMethod("onSocketDataRepli", Socket.class, DataBean.class, Fn.Callback1.class));
		dispatcher.fire("socket-data-repli", socket, before, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				Assert.assertEquals(after, arg1);
			}
		});
		Assert.assertSame(socket, h.args[0]);
		Assert.assertEquals(after, h.args[1]);
		Assert.assertTrue(h.args[2] instanceof Fn.Callback1);
	}
	
}
