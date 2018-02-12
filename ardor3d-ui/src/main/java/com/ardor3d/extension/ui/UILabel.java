/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ardor3d.extension.ui.util.SubTex;

/**
 * Basic implementation of {@link AbstractLabelUIComponent}.
 */
public class UILabel extends AbstractLabelUIComponent {

    private final LabelState _disabledState = new LabelState();
    private final LabelState _defaultState = new LabelState();

    /**
     * @param text
     *            the text value of this label.
     */
    public UILabel(final String text) {
        this(text, null);
    }

    /**
     *
     * @param text
     *            the text value of this label.
     * @param icon
     *            the icon value of this label.
     */
    public UILabel(final String text, final SubTex icon) {
        setText(text);
        setIcon(icon);

        applySkin();
        switchState(getDefaultState());
    }

    @Override
    public LabelState getDefaultState() {
        return _defaultState;
    }

    @Override
    public LabelState getDisabledState() {
        return _disabledState;
    }

    @Override
    public Set<UIState> getStates() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList((UIState) _defaultState, _disabledState)));
    }
}
