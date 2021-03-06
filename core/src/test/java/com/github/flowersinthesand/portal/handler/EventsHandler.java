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
package com.github.flowersinthesand.portal.handler;

import java.util.Map;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.Data;
import com.github.flowersinthesand.portal.On;
import com.github.flowersinthesand.portal.Reply;
import com.github.flowersinthesand.portal.Socket;

@Bean
public class EventsHandler {

	public Object[] args;

	@On
	public void load() {}
	
	@On("socket")
	public void socket(Socket socket) {
		args = new Object[] { socket };
	}
	
	@On("data")
	public void data(@Data DataBean data) {
		args = new Object[] { data };
	}
	
	@On("nestedData")
	public void nestedData(@Data Map<String, Object> data, @Data("data1") DataBean data1, @Data("data2") DataBean data2) {
		args = new Object[] { data, data1, data2 };
	}

	@On("repli")
	public void repli(@Reply Reply.Fn reply) {
		args = new Object[] { reply };
		reply.done();
	}

	@On("repli2")
	@Reply
	public void repli2() {}

	@On("repli-fail")
	public void repliFail(@Reply Reply.Fn reply) {
		args = new Object[] { reply };
		reply.fail(new RuntimeException("X"));
	}
	
	@On("repli-data")
	public void repliData(@Reply Reply.Fn reply, @Data DataBean data) {
		args = new Object[] { reply, data };
		reply.done(data);
	}

	@On("repli-data2")
	@Reply
	public Object repliData2(@Data DataBean data) {
		args = new Object[] { data };
		return data;
	}

	@On("socket-data-repli")
	public void socketDataRepli(Socket socket, @Data DataBean data, @Reply Reply.Fn reply) {
		args = new Object[] { socket, data, reply };
		reply.done(data);
	}
	
}
