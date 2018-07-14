/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.swt;

import java.util.Objects;

import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;

import com.ardor3d.input.FocusWrapper;

/**
 * Focus Listener wrapper class for use with SWT.
 */
public class SwtFocusWrapper implements FocusWrapper, FocusListener {
    private volatile boolean _focusLost = false;

    private final Control _control;

    public SwtFocusWrapper(final Control control) {
        _control = Objects.requireNonNull(control, "control");
    }

    @Override
    public void focusGained(final FocusEvent focusEvent) {
        // nothing to do
    }

    @Override
    public void focusLost(final FocusEvent focusEvent) {
        _focusLost = true;
    }

    @Override
    public boolean getAndClearFocusLost() {
        final boolean result = _focusLost;

        _focusLost = false;

        return result;
    }

    @Override
    public void init() {
        _control.addFocusListener(this);
    }
}
