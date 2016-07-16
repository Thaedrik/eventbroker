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

import java.util.Set;

/**
 * Mono threaded implementation of the {@link EventBroker} interface.
 * <p/>
 * All event notifications are signaled via a <strong>unique</strong> thread owned by the {@code
 * MonoThreadedEventBroker}.
 * <p/>
 * A call to the {@link #tearDown(boolean)} method is necessary to stop the thread for listening, otherwise it may
 * prevent the JVM to shutdown.
 *
 * @author Thaedrik <thaedrik@codestorming.org>
 */
public class MonoThreadedEventBroker extends AbstractEventBroker implements EventBroker {

	/**
	 * {@link Event} instance used to stop the event handling thread.
	 */
	protected static final Event STOP_EVENT = new BasicEvent(null, null);

	protected final Thread eventHandler = new Thread("EventBroker-EventHandler") {
		@Override
		public void run() {
			for (; ; ) {
				try {
					final Event event = eventQueue.take();
					if (event == STOP_EVENT) {
						stopped = true;
						break;
					}
					// Notifying the observers
					if (event.getType() != null) {
						observersLock.readLock().lock();
						Set<EventObserver> observers = typedObservers.get(event.getType());
						if (observers != null && observers.size() > 0) {
							for (EventObserver observer : observers) {
								try {
									observer.notify(event);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
						observersLock.readLock().unlock();
					}
				} catch (InterruptedException e) {
					System.err.println("MonoThreadedEventBroker was interrupted while waiting for events");
				}
			}
			// Removing all registered observers
			typedObservers.clear();
			eventQueue.clear();
		}
	};

	public MonoThreadedEventBroker() {
		eventHandler.start();
	}

	@Override
	public void tearDown(boolean clearQueue) {
		if (!stopped) {
			super.tearDown(clearQueue);
			eventQueue.add(STOP_EVENT);
		}
	}
}
