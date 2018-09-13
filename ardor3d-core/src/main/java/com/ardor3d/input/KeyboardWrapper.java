/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input;

import java.util.LinkedList;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.util.AbstractIterator;
import com.ardor3d.util.PeekingIterator;

/**
 * Defines the API for keyboard wrappers.
 */
public interface KeyboardWrapper {
    /**
     * Allows the keyboard wrapper implementation to initialise itself.
     */
    public void init();

    /**
     * Returns a peeking iterator that allows the client to loop through all keyboard events that have not yet been
     * handled.
     *
     * @return an iterator that allows the client to check which events have still not been handled
     */
    public PeekingIterator<KeyEvent> getEvents();

    @GuardedBy("this")
    final LinkedList<KeyEvent> _upcomingEvents = new LinkedList<>();

    public static final class KeyboardIterator extends AbstractIterator<KeyEvent> implements PeekingIterator<KeyEvent> {

        private final KeyboardWrapper keyboardWrapper;

        public KeyboardIterator(final KeyboardWrapper keyboardWrapper) {
            super();
            this.keyboardWrapper = keyboardWrapper;
        }

        @Override
        protected KeyEvent computeNext() {
            synchronized (keyboardWrapper) {
                if (_upcomingEvents.isEmpty()) {
                    return endOfData();
                }

                return _upcomingEvents.poll();
            }

        }
    }
}
