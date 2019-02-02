/*
 * Copyright Refinitiv 2018
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
 */
package com.refinitiv.beamio.trepwebsockets;

import java.io.Serializable;
import org.joda.time.Instant;

import com.google.auto.value.AutoValue;
import com.refinitiv.beamio.trepwebsockets.AutoValue_TrepCheckpointMark_Message;
import com.refinitiv.beamio.trepwebsockets.json.MarketPrice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.apache.beam.sdk.io.UnboundedSource;
import org.apache.beam.sdk.transforms.windowing.BoundedWindow;

/**
 * <p>Checkpoint for an unbounded TrepWsIOIO.Read. 
 * <p>Consists of the TREP ADS Sequence number and Timestamp.
 */
// default coder
public class TrepCheckpointMark implements UnboundedSource.CheckpointMark, Serializable {

	private static final long serialVersionUID = 1831054650379622269L;
	
	private final State state = new State();

	public TrepCheckpointMark() {
	}
	
	/**
	 * <p>A condensed MarketPrice message containing only sequence number and timestamp
	 * <p>Primarily used to save memory
	 *
	 */
	@AutoValue
	public abstract static class Message implements Serializable {
        private static final long serialVersionUID = 6977867817839628065L;
        public static Message of(Long seqNumber, Instant timestamp) {
            return new AutoValue_TrepCheckpointMark_Message(
                    seqNumber == null ? -1 : seqNumber,
                    timestamp == null ? Instant.now() : timestamp);
	    }
	    public abstract Long getSeqNumber();
	    public abstract Instant getTimestamp(); 
	}

	protected List<Message> getMessages() {
		return state.getMessages();
	}

	protected void addMessage(MarketPrice marketPrice) {
		state.atomicWrite(() -> {
		    Message message = Message.of(marketPrice.getSeqNumber(), marketPrice.getTimestamp());
			state.addMessage(message);
			state.updateOldestPendingTimestampIf(message.getTimestamp(), Instant::isBefore);
		});
	}
	
	protected Instant getOldestPendingTimestamp() {
	    return state.getOldestPendingTimestamp();
	}

	@Override
	public void finalizeCheckpoint() {
		State snapshot = state.snapshot();
		for (Message message : snapshot.messages) {
		    Instant currentMessageTimestamp = new Instant(message.getTimestamp());
	        snapshot.updateOldestPendingTimestampIf(currentMessageTimestamp, Instant::isAfter);
		}

		state.atomicWrite(() -> {
			state.removeMessages(snapshot.messages);
			state.updateOldestPendingTimestampIf(snapshot.oldestPendingTimestamp, Instant::isAfter);
		});
	}

	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TrepCheckpointMark other = (TrepCheckpointMark) obj;
        if (state == null) {
            if (other.state != null)
                return false;
        } else if (!state.equals(other.state))
            return false;
        return true;
    }

    /**
	 * Encapsulates the state of a checkpoint mark
	 */
	private class State implements Serializable {

		private static final long serialVersionUID = 3661245446722087170L;

		private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

		private final List<Message> messages;
		private Instant oldestPendingTimestamp;
		
		public State() {
			this(new ArrayList<>(), BoundedWindow.TIMESTAMP_MIN_VALUE);
		}

        State(List<Message> messages, Instant oldestPendingTimestamp) {
			this.messages = messages;
			this.oldestPendingTimestamp = oldestPendingTimestamp;
		}

	    /**
	     * Conditionally sets {@code oldestPendingTimestamp} to the value of the supplied {@code
	     * candidate}, if the provided {@code check} yields true for the {@code candidate} when called
	     * with the existing {@code oldestPendingTimestamp} value.
	     *
	     * @param candidate The potential new value.
	     * @param check The comparison method to call on {@code candidate} passing the existing {@code
	     *     oldestPendingTimestamp} value as a parameter.
	     */
        private void updateOldestPendingTimestampIf(
            Instant candidate, BiFunction<Instant, Instant, Boolean> check) {
            atomicWrite(
                () -> {
                    if (check.apply(candidate, this.oldestPendingTimestamp)) {
                        this.oldestPendingTimestamp = candidate;
                    }
                });
        }
	    
		/**
		 * Create and return a copy of the current state.
		 *
		 * @return A new {@code State} instance which is a deep copy of the target
		 *         instance at the time of execution.
		 */
		public State snapshot() {
			return atomicRead(() -> new State(new ArrayList<>(this.messages), this.oldestPendingTimestamp));
		}

	    public Instant getOldestPendingTimestamp() {
	       return atomicRead(() ->this.oldestPendingTimestamp);
	     }
		
		public List<Message> getMessages() {
			return atomicRead(() -> this.messages);
		}

		public void addMessage(Message trepMessage) {
			atomicWrite(() -> this.messages.add(trepMessage));
		}

		public void removeMessages(List<Message> messages) {
			atomicWrite(() -> this.messages.removeAll(messages));
		}

		/**
		 * Call the provided {@link Supplier} under this State's read lock and return
		 * its result.
		 *
		 * @param operation
		 *            The code to execute in the context of this State's read lock.
		 * @param <T>
		 *            The return type of the provided {@link Supplier}.
		 * @return The value produced by the provided {@link Supplier}.
		 */
		public <T> T atomicRead(Supplier<T> operation) {
			lock.readLock().lock();
			try {
				return operation.get();
			} finally {
				lock.readLock().unlock();
			}
		}

		/**
		 * Call the provided {@link Runnable} under this State's write lock.
		 *
		 * @param operation
		 *            The code to execute in the context of this State's write lock.
		 */
		public void atomicWrite(Runnable operation) {
			lock.writeLock().lock();
			try {
				operation.run();
			} finally {
				lock.writeLock().unlock();
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((messages == null) ? 0 : messages.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			State other = (State) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (messages == null) {
				if (other.messages != null) {
					return false;
				}
			} else if (!messages.equals(other.messages)) {
				return false;
			}
			return true;
		}

		private TrepCheckpointMark getOuterType() {
			return TrepCheckpointMark.this;
		}

	}

}