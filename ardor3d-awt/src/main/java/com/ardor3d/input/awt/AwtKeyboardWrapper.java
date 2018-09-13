/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.awt;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.util.EnumSet;
import java.util.Objects;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.input.Key;
import com.ardor3d.input.KeyEvent;
import com.ardor3d.input.KeyState;
import com.ardor3d.input.KeyboardWrapper;
import com.ardor3d.util.PeekingIterator;

/**
 * Keyboard wrapper class for use with AWT.
 */
public class AwtKeyboardWrapper implements KeyboardWrapper, KeyListener {

    @GuardedBy("this")
    protected KeyboardIterator _currentIterator = null;

    protected final Component _component;

    protected boolean _consumeEvents = false;

    protected final EnumSet<Key> _pressedList = EnumSet.noneOf(Key.class);

    public AwtKeyboardWrapper(final Component component) {
        _component = Objects.requireNonNull(component, "component");
    }

    @Override
    public void init() {
        _component.addKeyListener(this);
        _component.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(final FocusEvent e) {}

            @Override
            public void focusGained(final FocusEvent e) {
                _pressedList.clear();
            }
        });
    }

    @Override
    public synchronized PeekingIterator<KeyEvent> getEvents() {
        if (_currentIterator == null || !_currentIterator.hasNext()) {
            _currentIterator = new KeyboardIterator(this);
        }

        return _currentIterator;
    }

    @Override
    public synchronized void keyTyped(final java.awt.event.KeyEvent e) {
        if (_consumeEvents) {
            e.consume();
            // ignore this event
        }
    }

    @Override
    public synchronized void keyPressed(final java.awt.event.KeyEvent e) {
        final Key pressed = fromKeyEventToKey(e);
        if (!_pressedList.contains(pressed)) {
            _upcomingEvents.add(new KeyEvent(pressed, KeyState.DOWN, e.getKeyChar()));
            _pressedList.add(pressed);
        }
        if (_consumeEvents) {
            e.consume();
        }
    }

    @Override
    public synchronized void keyReleased(final java.awt.event.KeyEvent e) {
        final Key released = fromKeyEventToKey(e);
        _upcomingEvents.add(new KeyEvent(released, KeyState.UP, e.getKeyChar()));
        _pressedList.remove(released);
        if (_consumeEvents) {
            e.consume();
        }
    }

    /**
     * Convert from AWT key event to Ardor3D Key. Override to provide additional or custom behavior.
     *
     * @param e
     *            the AWT KeyEvent received by the input system.
     * @return an Ardor3D Key, to be forwarded to the Predicate/Trigger system.
     */
    public synchronized Key fromKeyEventToKey(final java.awt.event.KeyEvent e) {
        return AwtKey.findByCode(e.getKeyCode());
    }

    public boolean isConsumeEvents() {
        return _consumeEvents;
    }

    public void setConsumeEvents(final boolean consumeEvents) {
        _consumeEvents = consumeEvents;
    }
}
