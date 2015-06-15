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
 * An {@code EventBroker} is a service used for firing and observing to {@link Event}s.
 *
 * @author Thaedrik &lt;thaedrik@codestorming.org&gt;
 */
public interface EventBroker {

	/**
	 * Fires the given {@link Event} by notifying the {@link EventObserver}s registered for the event's type.
	 *
	 * @param event The {@link Event} to fire.
	 * @throws IllegalStateException if {@link #tearDown} method has been called prior to this method.
	 */
	void fire(Event event);

	/**
	 * Registers the given {@link EventObserver} to be notified when an {@link Event} of {@code eventType} is fired.
	 * <p/>
	 * The same observer can be registered for several different event type, but they will have to be unregistered one
	 * by one for each event type.
	 * <p/>
	 * Registering an observer twice for the same eventType does nothing.
	 *
	 * @param eventObserver The {@link EventObserver} to register.
	 * @param eventType The type of {@link Event} the observer will be notified for.
	 * @throws IllegalStateException if {@link #tearDown} method has been called prior to this method.
	 */
	void register(EventObserver eventObserver, String eventType);

	/**
	 * Unregisters the given {@link EventObserver} from the specified event type observers list.
	 *
	 * @param eventObserver The {@link EventObserver} to unregister.
	 * @param eventType The event type the observer will be removed from.
	 * @throws IllegalStateException if {@link #tearDown} method has been called prior to this method.
	 */
	void unregister(EventObserver eventObserver, String eventType);

	/**
	 * Implementors may use this method to clear the {@code EventBroker} internal state and/or free allocated
	 * resources.
	 * <p/>
	 * Implementors should invalidate the broker state by throwing {@link IllegalStateException} when calling any
	 * methods of a shutdown broker.
	 * <p/>
	 * Users should call this method when the {@code EventBroker} is not needed anymore.
	 */
	void tearDown();
}
