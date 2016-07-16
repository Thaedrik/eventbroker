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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Default implementation of an {@code EventBroker}.
 * <p/>
 * This implementation uses a blocking queue for the events and a read/write lock for accessing the observers
 * collection.
 *
 * @author Thaedrik <thaedrik@codestorming.org>
 * @since 2.0
 */
public abstract class AbstractEventBroker implements EventBroker {

	protected final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();

	protected final Map<String, Set<EventObserver>> typedObservers = new HashMap<>();

	/**
	 * Read-Write lock for synchronizing observers collections access.
	 */
	protected final ReadWriteLock observersLock = new ReentrantReadWriteLock();

	protected volatile boolean stopped;

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
		if (!stopped && clearQueue) {
			eventQueue.clear();
		}
	}

	protected void checkState() {
		if (stopped) {
			throw new IllegalStateException("Trying to call a shutdown EventBroker.");
		}
	}
}
