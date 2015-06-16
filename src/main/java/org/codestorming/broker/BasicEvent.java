/*
 * Copyright (c) 2012-2015 Codestorming.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors
 * 		Codestorming - initial API and implementation
 */

package org.codestorming.broker;

/**
 * A <em>non-mutable</em> {@link Event} implementation.
 *
 * @author Thaedrik &lt;thaedrik@codestorming.org&gt;
 */
public class BasicEvent implements Event {

	protected final String type;

	protected final Object data;

	/**
	 * Creates a new {@code BasicEvent}.
	 *
	 * @param type The event type.
	 * @param data The event data.
	 */
	public BasicEvent(String type, Object data) {
		this.type = type;
		this.data = data;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getData() {
		return (T) data;
	}
}
