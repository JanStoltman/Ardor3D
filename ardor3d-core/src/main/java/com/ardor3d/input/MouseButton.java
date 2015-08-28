/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input;

import java.util.EnumMap;

public enum MouseButton {
    LEFT, RIGHT, MIDDLE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE;

    public static EnumMap<MouseButton, ButtonState> makeMap(final ButtonState left, final ButtonState right,
            final ButtonState middle) {
        return makeMap(left, right, middle, ButtonState.UNDEFINED, ButtonState.UNDEFINED, ButtonState.UNDEFINED,
                ButtonState.UNDEFINED, ButtonState.UNDEFINED, ButtonState.UNDEFINED);
    }

    public static EnumMap<MouseButton, ButtonState> makeMap(final ButtonState left, final ButtonState right,
            final ButtonState middle, final ButtonState four, final ButtonState five, final ButtonState six,
            final ButtonState seven, final ButtonState eight, final ButtonState nine) {
        if (left == null) {
            throw new NullPointerException("left");
        }
        if (right == null) {
            throw new NullPointerException("right");
        }
        if (middle == null) {
            throw new NullPointerException("middle");
        }
        if (four == null) {
            throw new NullPointerException("four");
        }
        if (five == null) {
            throw new NullPointerException("five");
        }
        if (six == null) {
            throw new NullPointerException("six");
        }
        if (seven == null) {
            throw new NullPointerException("seven");
        }
        if (eight == null) {
            throw new NullPointerException("eight");
        }
        if (nine == null) {
            throw new NullPointerException("nine");
        }
        final EnumMap<MouseButton, ButtonState> map = new EnumMap<MouseButton, ButtonState>(MouseButton.class);
        map.put(LEFT, left);
        map.put(RIGHT, right);
        map.put(MIDDLE, middle);
        map.put(FOUR, four);
        map.put(FIVE, five);
        map.put(SIX, six);
        map.put(SEVEN, seven);
        map.put(EIGHT, eight);
        map.put(NINE, nine);
        return map;
    }
}
