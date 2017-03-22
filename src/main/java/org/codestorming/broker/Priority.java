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
 * Enumeration for event priority.
 *
 * @author Thaedrik <thaedrik@codestorming.org>
 * @since 2.1
 */
public enum Priority {
	VERY_LOW(1),
	LOW(2),
	NORMAL(3),
	HIGH(4),
	VERY_HIGH(5);

	private final int code;

	public static Priority get(int code) {
		switch (code) {
		case 1:
			return VERY_LOW;
		case 2:
			return LOW;
		case 3:
			return NORMAL;
		case 4:
			return HIGH;
		case 5:
			return VERY_HIGH;
		default:
			throw new IllegalArgumentException("No Priority with code " + code);
		}
	}

	Priority(int code) {
		this.code = code;
	}

	public int code() {
		return code;
	}
}
