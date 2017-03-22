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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The {@code PriorityEventBroker} handles events by their priority in a defined number of threads.
 * <p/>
 * This broker can be used if the order of incoming events is not important but rather their priority between them. That
 * is, higher priority events will have a tendancy to be handled sooner than the lower priority ones. Nevertheless, when
 * events are delayed their priority is incremented to prevent lowest priority events to never be handled. For example,
 * an event with priority `VERY_LOW` delayed four times will have its priority set to `VERY_HIGH`. This allows events
 * that happened first to have a slight advantage over other events with the same priority.
 * <p/>
 * If this broker is defined with more than one thread, the events will be handled concurrently, observers
 * implementations should take caution about this fact.
 * <p/>
 * <em>NOTE:</em> As this broker may accept instances of {@link Event}, their priority will be {@link Priority#NORMAL}
 * by default.
 *
 * @author Thaedrik <thaedrik@codestorming.org>
 * @since 2.1
 */
public final class PriorityEventBroker extends AbstractEventBroker implements EventBroker {

	private static final int MAX_SIZE = Runtime.getRuntime().availableProcessors() + 1;

	private static final int PRIORITY_THRESHOLD = Priority.values().length;

	private static final Event STOP_EVENT = new DelegatePriorityEvent(
			new BasicPriorityEvent("__STOP_EVENT__", Priority.VERY_HIGH, null));

	// Comparator to sort events by DESCENDING priority (Highest priority will be first)
	private static final Comparator<Event> EVENT_COMPARATOR = new Comparator<Event>() {
		@Override
		public int compare(Event o1, Event o2) {
			return ((DelegatePriorityEvent) o2).priority - ((DelegatePriorityEvent) o1).priority;
		}
	};

	private volatile int size;

	private ExecutorService executors = Executors.newCachedThreadPool();

	private final BlockingQueue<Event> eventQ = new ArrayBlockingQueue<>(500);

	/**
	 * Creates a new {@code PriorityEventBroker} of size one.
	 */
	public PriorityEventBroker() {
		this(1);
	}

	/**
	 * Creates a new {@code PriorityEventBroker} with the specified size.
	 *
	 * @param size Number of threads to handle the events.
	 * @see #changeSize(int)
	 */
	public PriorityEventBroker(int size) {
		this.size = size;
		startNewThreads(size);
	}

	@Override
	public void fire(Event event) {
		checkState();
		if (event == null) {
			throw new NullPointerException("Event is null");
		} // else

		if (event != STOP_EVENT) {
			event = new DelegatePriorityEvent(event);
		}
		try {
			eventQ.put(event);
		} catch (InterruptedException e) {
			System.err.println("Interrupted exception while adding event: " + e.getMessage());
		}
	}

	/**
	 * Change the number of threads this broker is using to handle the incoming events.
	 * <p/>
	 * The size is limited between zero and the number of available processors of the JVM plus one. If set to zero, the
	 * broker will continue to accept events but they won't be handled until at least one thread is added to the broker.
	 * <em>It can be used to pause the broker.</em>
	 * <p/>
	 * Removing threads to the broker is a command passed as an event, that means the already fired events will have to
	 * be handled first.
	 *
	 * @param newSize New number of threads the broker must use.
	 */
	public synchronized void changeSize(int newSize) {
		if (newSize > size && newSize <= MAX_SIZE) {
			startNewThreads(newSize - size);
			size = newSize;
		} else if (newSize < size && newSize >= 0) {
			stopThreads(size - newSize);
			size = newSize;
		}
	}

	/**
	 * Returns the current number of threads used by this broker.
	 *
	 * @return the current number of threads used by this broker.
	 */
	public int getSize() {
		return size;
	}

	private void startNewThreads(int number) {
		for (int i = 0; i < number; i++) {
			executors.submit(new Runnable() {

				boolean stopping = false;

				@Override
				public void run() {
					ArrayList<Event> events = new ArrayList<>(5);
					while (!stopping) {
						// Indicates if an event is added to the list
						boolean changed = false;
						try {
							Event event;
							// If events is empty, using blocking take() method
							// to obtain the first event.
							if (events.isEmpty()) {
								event = eventQ.take();
								events.add(event);
								changed = true;
								if (event == STOP_EVENT) {
									stopping = true;
								}
							}

							// Trying to obtain next events if possible, using poll() method
							// to avoid blocking if no more events are available.
							for (int j = 0, n = PRIORITY_THRESHOLD - events.size(); j < n && !stopping; j++) {
								event = eventQ.poll();
								if (event != null) {
									events.add(event);
									changed = true;
									if (event == STOP_EVENT) {
										stopping = true;
									}
								}
							}

							// Sorting events
							if (changed) {
								Collections.sort(events, EVENT_COMPARATOR);
							}

							// There is always at least ONE event in the list
							event = events.remove(0);
							if (event != STOP_EVENT) {
								handleEvent(event);
							} else {
								stopping = true;
							}

							// Incrementing events' priorities
							if (changed && events.size() > 1) {
								for (Event e : events) {
									((DelegatePriorityEvent) e).increment();
								}
							}
						} catch (Throwable t) {
							t.printStackTrace();
						}
					}
					// Finishing remaining events
					for (Event event : events) {
						if (event != STOP_EVENT) {
							handleEvent(event);
						}
					}
				}
			});
		}
	}

	private void stopThreads(int number) {
		for (int i = 0; i < number; i++) {
			fire(STOP_EVENT);
		}
	}

	private void handleEvent(Event event) {
		for (EventObserver observer : currentObservers(event.getType())) {
			try {
				observer.notify(((DelegatePriorityEvent) event).event);
			} catch (Exception e) {
				System.err.println(e.getLocalizedMessage());
			}
		}
	}

	@Override
	public void tearDown(boolean clearQueue) {
		if (!stopped) {
			if (clearQueue) {
				eventQ.clear();
			}
			changeSize(0);
			stopped = true;
			executors.shutdown();
		}
	}

	static class DelegatePriorityEvent implements Event {

		final Event event;

		int priority;

		public DelegatePriorityEvent(Event event) {
			this.event = event;
			if (event instanceof PriorityEvent) {
				priority = ((PriorityEvent) event).getPriority().code();
			} else {
				priority = Priority.NORMAL.code();
			}
		}

		void increment() {
			priority++;
		}

		@Override
		public String getType() {
			return event.getType();
		}

		@Override
		public <T> T getData() {
			return event.getData();
		}
	}
}
