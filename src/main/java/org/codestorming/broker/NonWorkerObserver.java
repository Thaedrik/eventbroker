/*
 * Copyright (c) 2012-2016 Codestorming.org
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
 * An {@link EventObserver} with {@link #executeInWorker()} that always returns {@code false}.
 *
 * @author Thaedrik <thaedrik@codestorming.org>
 * @since 2.0
 */
public abstract class NonWorkerObserver implements EventObserver {

	/**
	 * {@inheritDoc}
	 * <p/>
	 * This implementation always returns {@code false}.
	 *
	 * @return {@code false}
	 */
	@Override
	public final boolean executeInWorker() {
		return false;
	}
}
