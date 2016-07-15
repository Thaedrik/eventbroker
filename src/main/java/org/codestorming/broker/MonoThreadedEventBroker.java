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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
public class MonoThreadedEventBroker implements EventBroker {

	/**
	 * {@link Event} instance used to stop the event handling thread.
	 */
	protected static final Event STOP_EVENT = new BasicEvent(null, null);

	protected final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();

	protected final Map<String, Set<EventObserver>> typedObservers = new HashMap<>();

	/**
	 * Read-Write lock for synchronizing observers collections access.
	 */
	protected final ReadWriteLock observersLock = new ReentrantReadWriteLock();

	protected volatile boolean stopped;

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
		checkState();
		if (eventType == null || eventObserver == null) {
			throw new NullPointerException("eventType and eventObserver cannot be null");
		}// else
		observersLock.readLock().lock();
		Set<EventObserver> observers = typedObservers.get(eventType);
		observersLock.readLock().unlock();
		if (observers == null) {
			observers = createObserverSet(eventType);
		}
		observersLock.writeLock().lock();
		observers.add(eventObserver);
		observersLock.writeLock().unlock();
	}

	private Set<EventObserver> createObserverSet(String eventType) {
		observersLock.writeLock().lock();
		try {
			Set<EventObserver> observers = typedObservers.get(eventType);
			if (observers == null) {
				observers = new HashSet<>();
				typedObservers.put(eventType, observers);
			}
			return observers;
		} finally {
			observersLock.writeLock().unlock();
		}
	}

	@Override
	public void unregister(EventObserver eventObserver, String eventType) {
		checkState();
		if (eventType == null || eventObserver == null) {
			throw new NullPointerException("eventType and eventObserver cannot be null");
		}// else
		observersLock.readLock().lock();
		Set<EventObserver> observers = typedObservers.get(eventType);
		observersLock.readLock().unlock();
		if (observers != null) {
			observersLock.writeLock().lock();
			observers.remove(eventObserver);
			observersLock.writeLock().unlock();
		}
	}

	@Override
	public void tearDown(boolean clearQueue) {
		if (!stopped) {
			eventQueue.clear();
			// Firing the STOP_EVENT
			eventQueue.add(STOP_EVENT);
		}
	}

	private void checkState() {
		if (stopped) {
			throw new IllegalStateException("Trying to call a shutdown EventBroker.");
		}
	}
}
