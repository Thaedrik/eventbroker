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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
public class MultiThreadedEventBroker implements EventBroker {

	/**
	 * {@link Event} instance used to stop the event handling thread.
	 */
	private static final Event STOP_EVENT = new BasicEvent(null, null);

	private static final String ADD_OBSERVER_EVENT = "add-observer-event";

	private static final String REMOVE_OBSERVER_EVENT = "remove-observer-event";

	private final Map<String, Set<EventObserver>> typedObservers = new HashMap<>();

	protected final BlockingQueue<Event> eventQueue;

	protected final ExecutorService monoExecutor;

	protected final ExecutorService executor;

	private volatile boolean stopped;

	protected final Thread eventHandler = new Thread("MultiThreadedEventBroker-EventHandler") {
		@Override
		public void run() {
			for (; ; ) {
				try {
					final Event event = eventQueue.take();
					if (event == STOP_EVENT) {
						stopped = true;
						break;
					} // else

					final String eventType = event.getType();
					if (eventType == ADD_OBSERVER_EVENT) {
						_register(event.<ObserverData>getData());
					} else if (eventType == REMOVE_OBSERVER_EVENT) {
						_unregister(event.<ObserverData>getData());
					} else if (eventType != null) {
						Set<EventObserver> observers = typedObservers.get(event.getType());
						if (observers != null && observers.size() > 0) {
							for (final EventObserver observer : observers) {
								final ExecutorService e = observer.executeInWorker() ? executor : monoExecutor;
								e.submit(new Runnable() {
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
		eventQueue = new ArrayBlockingQueue<>(100);
		monoExecutor = Executors.newSingleThreadExecutor();
		executor = Executors.newFixedThreadPool(workerNumber);
		eventHandler.start();
	}

	private void _register(ObserverData observerData) {
		Set<EventObserver> observers = typedObservers.get(observerData.eventType);
		if (observers == null) {
			observers = createObserverSet(observerData.eventType);
		}
		observers.add(observerData.observer);
	}

	private void _unregister(ObserverData observerData) {
		Set<EventObserver> observers = typedObservers.get(observerData.eventType);
		if (observers != null) {
			observers.remove(observerData.observer);
		}
	}

	@Override
	public void fire(Event event) {
		checkState();
		if (event == null) {
			throw new NullPointerException("Event cannot be null");
		}// else
		try {
			eventQueue.put(event);
		} catch (InterruptedException e) {
			System.err.println("Fire was interrupted for the event :\n\t" + event);
		}
	}

	@Override
	public void register(EventObserver eventObserver, String eventType) {
		if (eventType == null || eventObserver == null) {
			throw new NullPointerException("eventType and eventObserver cannot be null");
		}// else
		fire(new BasicEvent(ADD_OBSERVER_EVENT, new ObserverData(eventObserver, eventType)));
	}

	private Set<EventObserver> createObserverSet(String eventType) {
		Set<EventObserver> observers = typedObservers.get(eventType);
		if (observers == null) {
			observers = new HashSet<>();
			typedObservers.put(eventType, observers);
		}
		return observers;
	}

	@Override
	public void unregister(EventObserver eventObserver, String eventType) {
		if (eventType == null || eventObserver == null) {
			throw new NullPointerException("eventType and eventObserver cannot be null");
		}// else
		fire(new BasicEvent(REMOVE_OBSERVER_EVENT, new ObserverData(eventObserver, eventType)));
	}

	@Override
	public void tearDown(boolean clearQueue) {
		if (!stopped) {
			if (clearQueue) {
				eventQueue.clear();
			}
			eventQueue.add(STOP_EVENT);
		}
	}

	protected void checkState() {
		if (stopped) {
			throw new IllegalStateException("Trying to call a shutdown EventBroker.");
		}
	}

	private static class ObserverData {

		final EventObserver observer;

		final String eventType;

		private ObserverData(EventObserver observer, String eventType) {
			this.observer = observer;
			this.eventType = eventType;
		}
	}
}
