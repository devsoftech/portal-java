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
package com.github.flowersinthesand.portal.samples.chat;

import java.util.Map;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.Data;
import com.github.flowersinthesand.portal.On;
import com.github.flowersinthesand.portal.Room;
import com.github.flowersinthesand.portal.Socket;
import com.github.flowersinthesand.portal.Wire;

@Bean
public class ChatHandler {

	@Wire
	private Room chat;

	@On.open
	public void open(Socket socket) {
		chat.add(socket);
	}

	@On.message
	public void message(@Data Map<String, Object> message) {
		chat.send("message", message);
	}

}
