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

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.codestorming.broker.MonoThreadedEventBroker.STOP_EVENT;

/**
 * Multi-threaded implementation of the {@link EventBroker} interface.
 * <p/>
 * By default, all {@link EventObserver}s are called inside the same thread. When the observers are registered, a call
 * to the {@link EventObserver#executeInWorker()} method defines how they will be notified.
 * <p/>
 * A call to the {@link #tearDown(boolean)} method is necessary to stop the thread for listening, otherwise it may
 * prevent the JVM to shutdown.
 *
 * @author Thaedrik <thaedrik@codestorming.org>
 * @since 2.0
 */
public class MultiThreadedEventBroker extends AbstractEventBroker implements EventBroker {

	protected final MonoThreadedEventBroker defaultBroker;

	protected final ExecutorService executor;

	protected final Thread eventHandler = new Thread("MultiThreadedEventBroker-EventHandler") {
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
							for (final EventObserver observer : observers) {
								executor.submit(new Runnable() {
									@Override
									public void run() {
										try {
											observer.notify(event);
										} catch (Throwable t) {
											System.err.println(t.getMessage());
										}
									}
								});
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

	/**
	 * Creates a new {@code MultiThreadedEventBroker} instance with the specified maximum number of workers.
	 *
	 * @param workerNumber Maximum number of worker threads.
	 * @throws IllegalArgumentException if {@code workerNumber < 1}
	 */
	public MultiThreadedEventBroker(int workerNumber) {
		defaultBroker = new MonoThreadedEventBroker();
		executor = Executors.newFixedThreadPool(workerNumber);
		eventHandler.start();
	}

	@Override
	public void fire(Event event) {
		super.fire(event);
		defaultBroker.fire(event);
	}

	@Override
	public void register(EventObserver eventObserver, String eventType) {
		if (eventObserver.executeInWorker()) {
			super.register(eventObserver, eventType);
		} else {
			defaultBroker.register(eventObserver, eventType);
		}
	}

	@Override
	public void unregister(EventObserver eventObserver, String eventType) {
		if (eventObserver.executeInWorker()) {
			super.unregister(eventObserver, eventType);
		} else {
			defaultBroker.unregister(eventObserver, eventType);
		}
	}

	@Override
	public void tearDown(boolean clearQueue) {
		if (!stopped) {
			super.tearDown(clearQueue);
			defaultBroker.tearDown(clearQueue);
			eventQueue.add(STOP_EVENT);
		}
	}
}
