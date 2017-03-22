/*
 * Copyright (c) 2012-2017 Codestorming.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Codestorming - initial API and implementation
 */
package org.codestorming.broker;

/**
 * Basic implementation of the {@link PriorityEvent}.
 *
 * @author Thaedrik <thaedrik@codestorming.org>
 * @since 2.1
 */
public class BasicPriorityEvent extends BasicEvent implements PriorityEvent {

	protected final Priority priority;

	/**
	 * Creates a new {@code BasicEvent}.
	 *
	 * @param type The event type.
	 * @param data The event data.
	 */
	public BasicPriorityEvent(String type, Priority priority, Object data) {
		super(type, data);
		this.priority = priority;
	}

	@Override
	public Priority getPriority() {
		return priority;
	}
}
