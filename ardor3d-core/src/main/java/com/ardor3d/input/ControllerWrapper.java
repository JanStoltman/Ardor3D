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
import com.ardor3d.util.PeekingIterator;
import com.google.common.collect.AbstractIterator;

public interface ControllerWrapper {
    /**
     * Allows the controller wrapper implementation to initialize itself.
     */
    public void init();

    /**
     * Returns a peeking iterator that allows the client to loop through all controller events that have not yet been
     * handled.
     *
     * @return an iterator that allows the client to check which events have still not been handled
     */
    public PeekingIterator<ControllerEvent> getEvents();

    public int getControllerCount();

    public ControllerInfo getControllerInfo(int controllerIndex);

    @GuardedBy("this")
    final LinkedList<ControllerEvent> _upcomingEvents = new LinkedList<>();

    public static final class ControllerIterator extends AbstractIterator<ControllerEvent>
            implements PeekingIterator<ControllerEvent> {

        private final ControllerWrapper controllerWrapper;

        public ControllerIterator(final ControllerWrapper controllerWrapper) {
            super();
            this.controllerWrapper = controllerWrapper;
        }

        @Override
        protected ControllerEvent computeNext() {
            synchronized (controllerWrapper) {
                if (_upcomingEvents.isEmpty()) {
                    return endOfData();
                }

                return _upcomingEvents.poll();
            }

        }
    }
}