/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.jinput;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.input.ControllerEvent;
import com.ardor3d.input.ControllerInfo;
import com.ardor3d.input.ControllerState;
import com.ardor3d.input.ControllerWrapper;
import com.ardor3d.util.PeekingIterator;

import net.java.games.input.Component;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Controller;
import net.java.games.input.Controller.Type;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;

public class JInputControllerWrapper implements ControllerWrapper {

    protected final Event _event = new Event();
    protected ControllerIterator _eventsIt = new ControllerIterator(this);
    protected final List<ControllerInfo> _controllers = new ArrayList<>();
    protected static boolean _inited = false;

    @Override
    public PeekingIterator<ControllerEvent> getEvents() {
        init();
        if (!_eventsIt.hasNext()) {
            _eventsIt = new ControllerIterator(this);
        }
        for (final Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
            controller.poll();
            while (controller.getEventQueue().getNextEvent(_event)) {
                if (controller.getType() != Type.KEYBOARD && controller.getType() != Type.MOUSE) {
                    synchronized (JInputControllerWrapper.this) {
                        _upcomingEvents.add(createControllerEvent(controller, _event));
                    }
                }
            }
        }

        return _eventsIt;
    }

    @Override
    public int getControllerCount() {
        init();
        return _controllers.size();
    }

    @Override
    public ControllerInfo getControllerInfo(final int controllerIndex) {
        init();
        return _controllers.get(controllerIndex);
    }

    @Override
    public synchronized void init() {
        if (_inited) {
            return;
        }

        try {
            ControllerEnvironment.getDefaultEnvironment();

            for (final Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
                if (controller.getType() != Type.KEYBOARD && controller.getType() != Type.MOUSE) {
                    _controllers.add(getControllerInfo(controller));
                    for (final Component component : controller.getComponents()) {
                        ControllerState.NOTHING.set(controller.getName(), component.getIdentifier().getName(), 0);
                    }
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            _inited = true;
        }
    }

    protected ControllerInfo getControllerInfo(final Controller controller) {
        final List<String> axisNames = new ArrayList<>();
        final List<String> buttonNames = new ArrayList<>();

        for (final Component comp : controller.getComponents()) {
            if (comp.getIdentifier() instanceof Identifier.Axis) {
                axisNames.add(comp.getName());
            } else if (comp.getIdentifier() instanceof Identifier.Button) {
                buttonNames.add(comp.getName());
            }
        }

        return new ControllerInfo(controller.getName(), axisNames, buttonNames);
    }

    protected ControllerEvent createControllerEvent(final Controller controller, final Event event) {
        return new ControllerEvent(event.getNanos(), controller.getName(),
                event.getComponent().getIdentifier().getName(), event.getValue());
    }
}