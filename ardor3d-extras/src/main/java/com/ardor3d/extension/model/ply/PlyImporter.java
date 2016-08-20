/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.ply;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.extension.model.util.FileHelper;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.GeometryTool;
import com.ardor3d.util.resource.ResourceLocator;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;

/**
 * PLY importer. See <a href="http://paulbourke.net/dataformats/ply/">the format spec</a>
 */
public class PlyImporter {

    public static enum Format {
        ASCII, BINARY_LITTLE_ENDIAN, BINARY_BIG_ENDIAN;
    };

    public static class FormatWithVersionNumber {

        private final Format format;

        private final double versionNumber;

        public FormatWithVersionNumber(final Format format, final double versionNumber) {
            super();
            this.format = format;
            this.versionNumber = versionNumber;
        }

        public Format getFormat() {
            return format;
        }

        public double getVersionNumber() {
            return versionNumber;
        }
    }

    public static enum ListProperty {
        /** indices of the vertices */
        VERTEX_INDICES(Element.FACE, Element.CUSTOM),
        /** indices of materials (not sure that it's really in the specification) */
        MATERIAL_INDICES(Element.FACE, Element.CUSTOM),
        /** texture coordinates (probably only supported by MeshLab) */
        TEXCOORD(Element.FACE, Element.CUSTOM),
        /** custom, i.e user-defined, not build-in, up to the developer to support it */
        CUSTOM(Element.CUSTOM);
        private final Element[] elements;

        private ListProperty(final Element... elements) {
            this.elements = elements;
        }

        public Element[] getElements() {
            return elements;
        }

        public static ListProperty get(final String name) {
            final String uppercaseName = name.toUpperCase();
            ListProperty result = null;
            try {
                result = ListProperty.valueOf(uppercaseName);
            } catch (final IllegalArgumentException iae) {
                if ("VERTEX_INDEX".equals(uppercaseName) || "VERTEX_INDEXES".equals(uppercaseName)) {
                    result = VERTEX_INDICES;
                } else if ("MATERIAL_INDEX".equals(uppercaseName) || "MATERIAL_INDEXES".equals(uppercaseName)) {
                    result = MATERIAL_INDICES;
                } else {
                    result = CUSTOM;
                }
            }
            return result;
        }
    }

    public static enum ScalarProperty {
        /** abscissa */
        X(Element.VERTEX, Element.CUSTOM),
        /** ordinate */
        Y(Element.VERTEX, Element.CUSTOM),
        /** applicate */
        Z(Element.VERTEX, Element.CUSTOM),
        /** normal x vector coordinate */
        NX(Element.VERTEX, Element.CUSTOM),
        /** normal y vector coordinate */
        NY(Element.VERTEX, Element.CUSTOM),
        /** normal z vector coordinate */
        NZ(Element.VERTEX, Element.CUSTOM),
        /** first vertex */
        VERTEX1(Element.EDGE, Element.CUSTOM),
        /** second vertex */
        VERTEX2(Element.EDGE, Element.CUSTOM),
        /** red color component */
        RED(Element.VERTEX, Element.EDGE, Element.CUSTOM),
        /** green color component */
        GREEN(Element.VERTEX, Element.EDGE, Element.CUSTOM),
        /** blue color component */
        BLUE(Element.VERTEX, Element.EDGE, Element.CUSTOM),
        /** material (ambient light) components */
        AMBIENT_RED(Element.MATERIAL, Element.CUSTOM), AMBIENT_GREEN(Element.MATERIAL, Element.CUSTOM), AMBIENT_BLUE(Element.MATERIAL, Element.CUSTOM), AMBIENT_COEFF(Element.MATERIAL, Element.CUSTOM),
        /** material (diffuse light) components */
        DIFFUSE_RED(Element.MATERIAL, Element.CUSTOM), DIFFUSE_GREEN(Element.MATERIAL, Element.CUSTOM), DIFFUSE_BLUE(Element.MATERIAL, Element.CUSTOM), DIFFUSE_COEFF(Element.MATERIAL, Element.CUSTOM),
        /** material (emissive light) components */
        EMISSIVE_RED(Element.MATERIAL, Element.CUSTOM), EMISSIVE_GREEN(Element.MATERIAL, Element.CUSTOM), EMISSIVE_BLUE(Element.MATERIAL, Element.CUSTOM), EMISSIVE_COEFF(Element.MATERIAL, Element.CUSTOM),
        /** material (specular light) components */
        SPECULAR_RED(Element.MATERIAL, Element.CUSTOM), SPECULAR_GREEN(Element.MATERIAL, Element.CUSTOM), SPECULAR_BLUE(Element.MATERIAL, Element.CUSTOM), SPECULAR_COEFF(Element.MATERIAL, Element.CUSTOM), SPECULAR_POWER(Element.MATERIAL, Element.CUSTOM),
        /** custom, i.e user-defined, not build-in, up to the developer to support it */
        CUSTOM(Element.CUSTOM);
        private final Element[] elements;

        private ScalarProperty(final Element... elements) {
            this.elements = elements;
        }

        public Element[] getElements() {
            return elements;
        }

        public static ScalarProperty get(final String name) {
            final String uppercaseName = name.toUpperCase();
            ScalarProperty result = null;
            try {
                result = ScalarProperty.valueOf(uppercaseName);
            } catch (final IllegalArgumentException iae) {
                result = CUSTOM;
            }
            return result;
        }
    }

    protected static class EnumWithKeyword<T extends Enum<?>> {

        private final T enumKey;

        private final String keyword;

        protected EnumWithKeyword(final T enumKey, final String keyword) {
            super();
            this.enumKey = enumKey;
            this.keyword = keyword;
        }

        @Override
        public int hashCode() {
            return Objects.hash(enumKey, keyword);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final EnumWithKeyword<?> other = (EnumWithKeyword<?>) obj;
            return enumKey == other.enumKey && Objects.equals(keyword, other.keyword);
        }

        public T getEnumKey() {
            return enumKey;
        }

        public String getKeyword() {
            return keyword;
        }

        @Override
        public String toString() {
            return "'" + Objects.toString(enumKey) + "[" + Objects.toString(keyword) + "]'";
        }
    }

    public static abstract class AbstractPropertyWithKeyword<T extends Enum<?>> extends EnumWithKeyword<T> {

        private final Data data;

        protected AbstractPropertyWithKeyword(final T enumKey, final String keyword, final Data data) {
            super(enumKey, keyword);
            this.data = data;
        }

        public Data getData() {
            return data;
        }

        public abstract Element[] getElements();

        @Override
        public String toString() {
            return "property " + super.toString() + " data type " + Objects.toString(getData());
        }
    }

    public static class ScalarPropertyWithKeyword extends AbstractPropertyWithKeyword<ScalarProperty> {

        public ScalarPropertyWithKeyword(final ScalarProperty scalarProperty, final String keyword, final Data data) {
            super(scalarProperty, keyword, data);
        }

        @Override
        public Element[] getElements() {
            return getEnumKey() == null ? null : getEnumKey().getElements();
        }

        @Override
        public String toString() {
            return "scalar " + super.toString();
        }
    }

    public static class ListPropertyWithKeyword extends AbstractPropertyWithKeyword<ListProperty> {

        private final Data countData;

        public ListPropertyWithKeyword(final ListProperty listProperty, final String keyword, final Data countData,
                final Data data) {
            super(listProperty, keyword, data);
            this.countData = countData;
        }

        @Override
        public Element[] getElements() {
            return getEnumKey() == null ? null : getEnumKey().getElements();
        }

        public Data getCountData() {
            return countData;
        }

        @Override
        public String toString() {
            return "list " + super.toString() + " index data type " + Objects.toString(countData);
        }
    }

    public static enum Element {
        VERTEX, FACE, EDGE, MATERIAL,
        /** custom, i.e user-defined, not build-in, up to the developer to support it */
        CUSTOM;

        public static Element get(final String name) {
            final String uppercaseName = name.toUpperCase();
            Element result = null;
            try {
                result = Element.valueOf(uppercaseName);
            } catch (final IllegalArgumentException iae) {
                result = CUSTOM;
            }
            return result;
        }
    };

    public static class ElementWithKeyword extends EnumWithKeyword<Element> {

        public ElementWithKeyword(final Element element, final String keyword) {
            super(element, keyword);
        }

        @Override
        public String toString() {
            return "element " + super.toString();
        }
    }

    public static enum Data {

        /** one byte signed integer */
        CHAR,
        /** one byte unsigned integer */
        UCHAR,
        /** two byte signed integer */
        SHORT,
        /** two byte unsigned integer */
        USHORT,
        /** four byte signed integer */
        INT,
        /** four byte unsigned integer */
        UINT,
        /** four byte floating point number */
        FLOAT,
        /** eight byte floating point number */
        DOUBLE;

        public static Data get(final String name) throws IllegalArgumentException {
            final String uppercaseName = name.toUpperCase();
            Data result = null;
            try {
                result = Data.valueOf(uppercaseName);
            } catch (final IllegalArgumentException iae) {
                switch (uppercaseName) {
                    case "INT8": {
                        result = CHAR;
                        break;
                    }
                    case "UINT8": {
                        result = UCHAR;
                        break;
                    }
                    case "INT16": {
                        result = SHORT;
                        break;
                    }
                    case "UINT16": {
                        result = USHORT;
                        break;
                    }
                    case "INT32": {
                        result = INT;
                        break;
                    }
                    case "UINT32": {
                        result = UINT;
                        break;
                    }
                    case "FLOAT32": {
                        result = FLOAT;
                        break;
                    }
                    case "FLOAT64": {
                        result = DOUBLE;
                        break;
                    }
                    default: {
                        throw new IllegalArgumentException(
                                "'" + name + "' does not match with any data type supported by the PLY format");
                    }
                }
            }
            return result;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(PlyImporter.class.getName());

    public static class PlyFileParser extends StreamTokenizer {

        /**
         * Constructor.
         *
         * @param reader
         *            The Reader.
         */
        public PlyFileParser(final Reader reader) {
            super(reader);
            resetSyntax();
            eolIsSignificant(true);
            lowerCaseMode(true);

            // all printable ascii characters
            wordChars('!', '~');

            whitespaceChars(' ', ' ');
            whitespaceChars('\n', '\n');
            whitespaceChars('\r', '\r');
            whitespaceChars('\t', '\t');
        }

        /**
         * Gets a number from the stream. Need to extract numbers since they may be in scientific notation. The number
         * is returned in nval.
         *
         * @return Logical-true if successful, else logical-false.
         */
        protected boolean getNumber() {
            try {
                nextToken();
                if (ttype != StreamTokenizer.TT_WORD) {
                    return false;
                }
                nval = Double.valueOf(sval).doubleValue();
            } catch (final IOException e) {
                System.err.println(e.getMessage());
                return false;
            } catch (final NumberFormatException e) {
                System.err.println(e.getMessage());
                return false;
            }
            return true;
        }

    }

    private ResourceLocator _modelLocator;

    private ResourceLocator _textureLocator;

    private boolean _flipTextureVertically;

    private boolean _useCompression;

    private MinificationFilter _minificationFilter;

    /**
     * Constructor.
     */
    public PlyImporter() {
        super();
        _flipTextureVertically = true;
        _useCompression = true;
        _minificationFilter = MinificationFilter.Trilinear;
    }

    /**
     * Reads a PLY file from the given resource
     *
     * @param resource
     *            the name of the resource to find.
     * @return a PlyGeometryStore data object containing the scene and other useful elements.
     */
    public PlyGeometryStore load(final String resource) {
        return load(resource, new FileHelper(), new GeometryTool());
    }

    /**
     * Reads a PLY file from the given resource
     *
     * @param resource
     *            the name of the resource to find.
     * @param fileHelper
     *            the file helper used to determine whether the resource is an Ascii file or a binary file
     *
     * @return a PlyGeometryStore data object containing the scene and other useful elements.
     */
    public PlyGeometryStore load(final String resource, final FileHelper fileHelper) {
        return load(resource, fileHelper, new GeometryTool());
    }

    /**
     * Reads a PLY file from the given resource
     *
     * @param resource
     *            the name of the resource to find.
     * @param geometryTool
     *            the geometry tool to optimize the meshes
     *
     * @return a PlyGeometryStore data object containing the scene and other useful elements.
     */
    public PlyGeometryStore load(final String resource, final GeometryTool geometryTool) {
        return load(resource, new FileHelper(), geometryTool);
    }

    /**
     * Reads a PLY file from the given resource
     *
     * @param resource
     *            the name of the resource to find.
     * @param fileHelper
     *            the file helper used to determine whether the resource is an Ascii file or a binary file
     * @param geometryTool
     *            the geometry tool to optimize the meshes
     *
     * @return a PlyGeometryStore data object containing the scene and other useful elements.
     */
    public PlyGeometryStore load(final String resource, final FileHelper fileHelper, final GeometryTool geometryTool) {
        final ResourceSource source;
        if (_modelLocator == null) {
            source = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL, resource);
        } else {
            source = _modelLocator.locateResource(resource);
        }

        if (source == null) {
            throw new Error("Unable to locate '" + resource + "'");
        }

        return load(source, fileHelper, geometryTool);
    }

    /**
     * Reads a PLY file from the given resource
     *
     * @param resource
     *            the resource to find.
     * @return a PlyGeometryStore data object containing the scene and other useful elements.
     */
    public PlyGeometryStore load(final ResourceSource resource) {
        return load(resource, new FileHelper(), new GeometryTool());
    }

    /**
     * Reads a PLY file from the given resource
     *
     * @param resource
     *            the resource to find.
     * @param fileHelper
     *            the file helper used to determine whether the resource is an Ascii file or a binary file
     *
     * @return a PlyGeometryStore data object containing the scene and other useful elements.
     */
    public PlyGeometryStore load(final ResourceSource resource, final FileHelper fileHelper) {
        return load(resource, fileHelper, new GeometryTool());
    }

    /**
     * Reads a PLY file from the given resource
     *
     * @param resource
     *            the resource to find.
     * @param geometryTool
     *            the geometry tool to optimize the meshes
     *
     * @return a PlyGeometryStore data object containing the scene and other useful elements.
     */
    public PlyGeometryStore load(final ResourceSource resource, final GeometryTool geometryTool) {
        return load(resource, new FileHelper(), geometryTool);
    }

    /**
     * Reads a PLY file from the given resource
     *
     * @param resource
     *            the resource to find.
     * @param fileHelper
     *            the file helper used to determine whether the resource is an Ascii file or a binary file
     * @param geometryTool
     *            the geometry tool to optimize the meshes
     *
     * @return a PlyGeometryStore data object containing the scene and other useful elements.
     */
    public PlyGeometryStore load(final ResourceSource resource, final FileHelper fileHelper,
            final GeometryTool geometryTool) {
        final boolean isAscii = fileHelper.isFilePureAscii(resource);
        final PlyGeometryStore store = createGeometryStore(geometryTool);
        if (isAscii) { // Ascii file
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream()))) {
                final PlyFileParser parser = new PlyFileParser(reader);
                try {
                    // starts reading the header
                    parser.nextToken();
                    // reads "ply"
                    if ("ply".equals(parser.sval)) {
                        PlyImporter.LOGGER.log(Level.INFO, "ply keyword on line " + parser.lineno());
                    } else {
                        PlyImporter.LOGGER.log(Level.SEVERE,
                                "Ascii file but no ply keyword on line " + parser.lineno());
                    }
                    // reads the EOL for verifying that the file has a correct format
                    parser.nextToken();
                    if (parser.ttype != StreamTokenizer.TT_EOL) {
                        PlyImporter.LOGGER.log(Level.SEVERE,
                                "Format Error: expecting End Of Line on line " + parser.lineno());
                    }
                    parser.nextToken();
                    // reads the rest of the header
                    FormatWithVersionNumber formatWithVersionNumber = null;
                    final Map<ElementWithKeyword, Map.Entry<Integer, Set<AbstractPropertyWithKeyword<?>>>> elementMap = new LinkedHashMap<>();
                    while (parser.ttype != StreamTokenizer.TT_EOF && !"end_header".equals(parser.sval)) {
                        if (parser.ttype == StreamTokenizer.TT_WORD) {
                            final int currentLineNumber = parser.lineno();
                            switch (parser.sval) {
                                case "comment": {
                                    parser.nextToken();
                                    if (parser.ttype == StreamTokenizer.TT_WORD) {
                                        if ("TextureFile".equals(parser.sval)) {
                                            parser.nextToken();
                                            if (parser.ttype == StreamTokenizer.TT_WORD) {
                                                final String textureName = parser.sval;
                                                store.setTextureName(textureName);
                                                final Texture texture;
                                                if (_textureLocator == null) {
                                                    texture = TextureManager.load(textureName, getMinificationFilter(),
                                                            isUseCompression()
                                                                    ? TextureStoreFormat.GuessCompressedFormat
                                                                    : TextureStoreFormat.GuessNoCompressedFormat,
                                                            isFlipTextureVertically());
                                                } else {
                                                    final ResourceSource source = _textureLocator
                                                            .locateResource(textureName);
                                                    texture = TextureManager.load(source, getMinificationFilter(),
                                                            isUseCompression()
                                                                    ? TextureStoreFormat.GuessCompressedFormat
                                                                    : TextureStoreFormat.GuessNoCompressedFormat,
                                                            isFlipTextureVertically());
                                                }
                                                store.setTexture(texture);
                                            } else {
                                                PlyImporter.LOGGER.log(Level.SEVERE,
                                                        "'TextureFile' comment with no texture file on line "
                                                                + currentLineNumber);
                                            }
                                        }
                                    }
                                    break;
                                }
                                case "format": {
                                    parser.nextToken();
                                    if (parser.ttype == StreamTokenizer.TT_WORD) {
                                        if (formatWithVersionNumber == null) {
                                            Format format = null;
                                            try {
                                                format = Format.valueOf(parser.sval.toUpperCase());
                                            } catch (final IllegalArgumentException iae) {
                                                PlyImporter.LOGGER.log(Level.SEVERE, "Unknown format '" + parser.sval
                                                        + "' on line " + currentLineNumber + ": " + iae.getMessage());
                                            }
                                            final double versionNumber;
                                            if (parser.getNumber()) {
                                                versionNumber = parser.nval;
                                                if (Double.compare(versionNumber, 1.0d) != 0) {
                                                    PlyImporter.LOGGER.log(Level.WARNING,
                                                            "Unsupported format version number '" + parser.nval
                                                                    + "' on line " + currentLineNumber
                                                                    + ". This importer supports only PLY 1.0");
                                                }
                                                parser.nextToken();
                                                if (parser.ttype != StreamTokenizer.TT_EOL) {
                                                    PlyImporter.LOGGER.log(Level.SEVERE,
                                                            "Format Error: expecting End Of Line on line "
                                                                    + currentLineNumber);
                                                }
                                            } else {
                                                PlyImporter.LOGGER.log(Level.SEVERE,
                                                        "Format version number missing on line " + currentLineNumber
                                                                + "\n");
                                                versionNumber = Double.NaN;
                                            }
                                            formatWithVersionNumber = new FormatWithVersionNumber(format,
                                                    versionNumber);
                                            PlyImporter.LOGGER.log(Level.INFO,
                                                    "Format '" + (format == null ? "null" : format.name())
                                                            + "' version number '" + versionNumber
                                                            + "' detected on line " + currentLineNumber);
                                        } else {
                                            PlyImporter.LOGGER.log(Level.WARNING,
                                                    "Format already defined, format declaration ignored on line "
                                                            + currentLineNumber);
                                        }
                                    } else {
                                        PlyImporter.LOGGER.log(Level.SEVERE,
                                                "Format type (ascii, binary_big_endian or binary_little_endian) missing on line "
                                                        + currentLineNumber);
                                    }
                                    break;
                                }
                                case "element": {
                                    parser.nextToken();
                                    if (parser.ttype == StreamTokenizer.TT_WORD) {
                                        final String elementName = parser.sval;
                                        final Element element = Element.get(elementName);
                                        final ElementWithKeyword elementWithKeyword = new ElementWithKeyword(element,
                                                elementName);
                                        if (elementMap.containsKey(element)) {
                                            PlyImporter.LOGGER.log(Level.WARNING,
                                                    elementWithKeyword
                                                            + " already defined, element declaration ignored on line "
                                                            + currentLineNumber);
                                        } else {
                                            final int elementCount;
                                            if (parser.getNumber()) {
                                                elementCount = (int) parser.nval;
                                                if (elementCount < 0) {
                                                    PlyImporter.LOGGER.log(Level.SEVERE,
                                                            elementWithKeyword + " count = " + elementCount
                                                                    + " whereas it should be >= 0 on line "
                                                                    + currentLineNumber);
                                                }
                                                parser.nextToken();
                                                if (parser.ttype != StreamTokenizer.TT_EOL) {
                                                    PlyImporter.LOGGER.log(Level.SEVERE,
                                                            "Format Error: expecting End Of Line on line "
                                                                    + currentLineNumber);
                                                }
                                            } else {
                                                PlyImporter.LOGGER.log(Level.SEVERE, elementWithKeyword
                                                        + " count missing on line " + currentLineNumber);
                                                elementCount = 0;
                                            }
                                            elementMap.put(elementWithKeyword,
                                                    new AbstractMap.SimpleEntry<Integer, Set<AbstractPropertyWithKeyword<?>>>(
                                                            Integer.valueOf(elementCount), null));
                                            PlyImporter.LOGGER.log(Level.INFO,
                                                    elementWithKeyword + " detected on line " + currentLineNumber);
                                        }
                                    } else {
                                        PlyImporter.LOGGER.log(Level.SEVERE,
                                                "Element type (vertex, face or edge) missing on line "
                                                        + currentLineNumber);
                                    }
                                    break;
                                }
                                case "property": {
                                    ElementWithKeyword latestInsertedElementWithKeyword = null;
                                    for (final ElementWithKeyword elementWithKeyword : elementMap.keySet()) {
                                        latestInsertedElementWithKeyword = elementWithKeyword;
                                    }
                                    if (latestInsertedElementWithKeyword == null) {
                                        PlyImporter.LOGGER.log(Level.SEVERE,
                                                "Property definition not preceded by an element definition on line "
                                                        + currentLineNumber);
                                    } else {
                                        parser.nextToken();
                                        if (parser.ttype == StreamTokenizer.TT_WORD) {
                                            if ("list".equals(parser.sval)) {
                                                // list property, for face elements (vertex indices, texture
                                                // coordinates, ...)
                                                parser.nextToken();
                                                if (parser.ttype == StreamTokenizer.TT_WORD) {
                                                    Data countData = null;
                                                    try {
                                                        countData = Data.get(parser.sval);
                                                    } catch (final IllegalArgumentException iae) {
                                                        PlyImporter.LOGGER.log(Level.SEVERE,
                                                                "Count data type '" + parser.sval + "' unknown on line "
                                                                        + currentLineNumber);
                                                    }
                                                    if (countData != null) {
                                                        parser.nextToken();
                                                        if (parser.ttype == StreamTokenizer.TT_WORD) {
                                                            Data data = null;
                                                            try {
                                                                data = Data.get(parser.sval);
                                                            } catch (final IllegalArgumentException iae) {
                                                                PlyImporter.LOGGER.log(Level.SEVERE,
                                                                        "Data type '" + parser.sval
                                                                                + "' unknown on line "
                                                                                + currentLineNumber);
                                                            }
                                                            if (data != null) {
                                                                parser.nextToken();
                                                                if (parser.ttype == StreamTokenizer.TT_WORD) {
                                                                    final String listPropertyName = parser.sval;
                                                                    final ListProperty listProperty = ListProperty
                                                                            .get(listPropertyName);
                                                                    final ListPropertyWithKeyword listPropertyWithKeyword = new ListPropertyWithKeyword(
                                                                            listProperty, listPropertyName, countData,
                                                                            data);
                                                                    if (Arrays.asList(listProperty.getElements())
                                                                            .contains(latestInsertedElementWithKeyword
                                                                                    .getEnumKey())) {
                                                                        final Entry<Integer, Set<AbstractPropertyWithKeyword<?>>> elementMapEntry = elementMap
                                                                                .get(latestInsertedElementWithKeyword);
                                                                        Set<AbstractPropertyWithKeyword<?>> propertySet = elementMapEntry
                                                                                .getValue();
                                                                        if (propertySet == null) {
                                                                            propertySet = new LinkedHashSet<>();
                                                                            elementMapEntry.setValue(propertySet);
                                                                        }
                                                                        propertySet.add(listPropertyWithKeyword);
                                                                        PlyImporter.LOGGER.log(Level.INFO,
                                                                                listPropertyWithKeyword
                                                                                        + " detected on line "
                                                                                        + currentLineNumber);
                                                                    } else {
                                                                        PlyImporter.LOGGER.log(Level.SEVERE,
                                                                                "Unexpected " + listPropertyWithKeyword
                                                                                        + " on line "
                                                                                        + currentLineNumber);
                                                                    }
                                                                } else {
                                                                    PlyImporter.LOGGER.log(Level.SEVERE,
                                                                            "List property keyword (vertex_indices, texcoord, ...) missing on line "
                                                                                    + currentLineNumber);
                                                                }
                                                            }
                                                        } else {
                                                            PlyImporter.LOGGER.log(Level.SEVERE,
                                                                    "Second data type (float32, int8, ...) missing on line "
                                                                            + currentLineNumber);
                                                        }
                                                    }
                                                } else {
                                                    PlyImporter.LOGGER.log(Level.SEVERE,
                                                            "First data type (float32, int8, ...) missing on line "
                                                                    + currentLineNumber);
                                                }
                                            } else {
                                                // scalar property (vertex coordinates, normal coordinates, ...)
                                                Data data = null;
                                                try {
                                                    data = Data.get(parser.sval);
                                                } catch (final IllegalArgumentException iae) {
                                                    PlyImporter.LOGGER.log(Level.SEVERE, "Data type '" + parser.sval
                                                            + "' unknown on line " + currentLineNumber);
                                                }
                                                if (data != null) {
                                                    parser.nextToken();
                                                    if (parser.ttype == StreamTokenizer.TT_WORD) {
                                                        final String scalarPropertyName = parser.sval;
                                                        final ScalarProperty scalarProperty = ScalarProperty
                                                                .get(scalarPropertyName);
                                                        final ScalarPropertyWithKeyword scalarPropertyWithKeyword = new ScalarPropertyWithKeyword(
                                                                scalarProperty, scalarPropertyName, data);
                                                        if (Arrays.asList(scalarProperty.getElements()).contains(
                                                                latestInsertedElementWithKeyword.getEnumKey())) {
                                                            final Entry<Integer, Set<AbstractPropertyWithKeyword<?>>> elementMapValue = elementMap
                                                                    .get(latestInsertedElementWithKeyword);
                                                            Set<AbstractPropertyWithKeyword<?>> propertySet = elementMapValue
                                                                    .getValue();
                                                            if (propertySet == null) {
                                                                propertySet = new LinkedHashSet<>();
                                                                elementMapValue.setValue(propertySet);
                                                            }
                                                            propertySet.add(scalarPropertyWithKeyword);
                                                            PlyImporter.LOGGER.log(Level.INFO, scalarPropertyWithKeyword
                                                                    + " detected on line " + currentLineNumber);
                                                        } else {
                                                            PlyImporter.LOGGER.log(Level.SEVERE,
                                                                    "Unexpected " + scalarPropertyWithKeyword + " in a "
                                                                            + latestInsertedElementWithKeyword
                                                                            + " on line " + currentLineNumber);
                                                        }
                                                    } else {
                                                        PlyImporter.LOGGER.log(Level.SEVERE,
                                                                "Scalar property keyword (x, nx, vertex1, red, ...) missing on line "
                                                                        + currentLineNumber);
                                                    }
                                                }
                                            }
                                        } else {
                                            PlyImporter.LOGGER.log(Level.SEVERE,
                                                    "Property type (list) or scalar data type (float32, int8, ...) missing on line "
                                                            + currentLineNumber);
                                        }
                                    }
                                    break;
                                }
                                default: {
                                    PlyImporter.LOGGER.log(Level.SEVERE,
                                            "Unknown command '" + parser.sval + "' on line " + currentLineNumber);
                                    break;
                                }
                            }
                        } else {
                            PlyImporter.LOGGER.log(Level.SEVERE,
                                    "No word at the beginning of the line " + parser.lineno());
                        }
                        // reads the whole line, doesn't look at the content
                        while (parser.ttype != StreamTokenizer.TT_EOL) {
                            parser.nextToken();
                        }
                        // if there is still something to read, reads the next token
                        if (parser.ttype != StreamTokenizer.TT_EOF) {
                            parser.nextToken();
                        }
                    }
                    if ("end_header".equals(parser.sval)) {
                        PlyImporter.LOGGER.log(Level.INFO, "End of header on line " + parser.lineno());
                        do {
                            parser.nextToken();
                        } while (parser.ttype != StreamTokenizer.TT_EOL);
                    }
                    // reads the lines after the header
                    final Iterator<Entry<ElementWithKeyword, Entry<Integer, Set<AbstractPropertyWithKeyword<?>>>>> elementMapEntryIterator = elementMap
                            .entrySet().iterator();
                    Entry<ElementWithKeyword, Entry<Integer, Set<AbstractPropertyWithKeyword<?>>>> elementMapEntry;
                    int currentReadableElementCount;
                    ElementWithKeyword currentElementWithKeyword;
                    Set<AbstractPropertyWithKeyword<?>> currentProperties;
                    if (elementMapEntryIterator.hasNext()) {
                        elementMapEntry = elementMapEntryIterator.next();
                        currentReadableElementCount = elementMapEntry.getValue().getKey().intValue();
                        currentElementWithKeyword = elementMapEntry.getKey();
                        currentProperties = elementMapEntry.getValue().getValue();
                        PlyImporter.LOGGER.log(Level.INFO, "Reading of " + currentReadableElementCount + " elements ("
                                + currentElementWithKeyword + ") started on line " + parser.lineno());
                    } else {
                        elementMapEntry = null;
                        currentReadableElementCount = 0;
                        currentElementWithKeyword = null;
                        currentProperties = null;
                    }
                    while (parser.ttype != StreamTokenizer.TT_EOF) {
                        final int currentLineNumber = parser.lineno();
                        // reads one line
                        final List<Double> valueList = new ArrayList<>();
                        do {
                            parser.nextToken();
                            if (parser.ttype == StreamTokenizer.TT_WORD) {
                                try {
                                    final Double value = Double.valueOf(parser.sval);
                                    parser.nval = value.doubleValue();
                                    valueList.add(value);
                                } catch (final NumberFormatException nfe) {
                                    PlyImporter.LOGGER.log(Level.SEVERE, "Unparsable string '" + parser.sval
                                            + "', expected a number on line " + currentLineNumber);
                                }
                            }
                        } while (parser.ttype != StreamTokenizer.TT_EOL && parser.ttype != StreamTokenizer.TT_EOF);
                        if (!valueList.isEmpty()) {
                            // stores the values into an array
                            final double[] values = new double[valueList.size()];
                            for (int valueIndex = 0; valueIndex < values.length; valueIndex++) {
                                values[valueIndex] = valueList.get(valueIndex).doubleValue();
                            }
                            // determines which kind of element is being parsed
                            if (currentReadableElementCount == 0) {
                                if (elementMapEntryIterator.hasNext()) {
                                    elementMapEntry = elementMapEntryIterator.next();
                                    currentReadableElementCount = elementMapEntry.getValue().getKey().intValue();
                                    currentElementWithKeyword = elementMapEntry.getKey();
                                    currentProperties = elementMapEntry.getValue().getValue();
                                    if (currentElementWithKeyword != null) {
                                        PlyImporter.LOGGER.log(Level.INFO,
                                                "Reading of " + currentReadableElementCount + " elements ("
                                                        + currentElementWithKeyword + ") started on line "
                                                        + currentLineNumber);
                                    }
                                } else {
                                    elementMapEntry = null;
                                    currentElementWithKeyword = null;
                                    currentProperties = null;
                                }
                            }
                            if (currentElementWithKeyword == null) {
                                PlyImporter.LOGGER.log(Level.SEVERE,
                                        "Element data beyond element ranges defined in the header on line "
                                                + currentLineNumber);
                            } else {
                                if (currentElementWithKeyword.getEnumKey() == Element.CUSTOM) {
                                    processElementCustomData(formatWithVersionNumber, currentElementWithKeyword,
                                            currentProperties, values, currentLineNumber, store);
                                } else {
                                    boolean hasBuildInProperties = false;
                                    boolean hasCustomProperties = false;
                                    if (currentProperties == null || currentProperties.isEmpty()) {
                                        PlyImporter.LOGGER.log(Level.SEVERE, currentElementWithKeyword
                                                + " data with no property skipped on line " + currentLineNumber);
                                    } else {
                                        for (final AbstractPropertyWithKeyword<?> currentProperty : currentProperties) {
                                            final boolean isCustom = currentProperty
                                                    .getEnumKey() == ScalarProperty.CUSTOM
                                                    || currentProperty.getEnumKey() == ListProperty.CUSTOM;
                                            if (!hasCustomProperties && isCustom) {
                                                hasCustomProperties = true;
                                            }
                                            if (!hasBuildInProperties && !isCustom) {
                                                hasBuildInProperties = true;
                                            }
                                            if (hasBuildInProperties && hasCustomProperties) {
                                                break;
                                            }
                                        }
                                        if (hasBuildInProperties) {
                                            processElementBuildInData(formatWithVersionNumber,
                                                    currentElementWithKeyword, currentProperties, values,
                                                    currentLineNumber, store);
                                        }
                                        if (hasCustomProperties) {
                                            processElementCustomData(formatWithVersionNumber, currentElementWithKeyword,
                                                    currentProperties, values, currentLineNumber, store);
                                        }
                                    }
                                }
                            }
                            if (currentReadableElementCount > 0) {
                                currentReadableElementCount--;
                            }
                        }
                        // goes to the next line if the end of file isn't reached
                        while (parser.ttype != StreamTokenizer.TT_EOL && parser.ttype != StreamTokenizer.TT_EOF) {
                            parser.nextToken();
                        }
                    }
                } catch (final IOException ioe) {
                    throw new Exception("IO Error on line " + parser.lineno(), ioe);
                }
            } catch (final Throwable t) {
                throw new Error("Unable to load ply resource from URL: " + resource, t);
            }
        } else { // Binary file
            try (final InputStream data = resource.openStream()) {
                // TODO
                throw new UnsupportedOperationException("Binary PLY import not implemented");
            } catch (final Throwable t) {
                throw new Error("Unable to load ply resource from URL: " + resource, t);
            }
        }
        store.commitObjects();
        store.cleanup();
        return store;
    }

    protected PlyGeometryStore createGeometryStore(final GeometryTool geometryTool) {
        return new PlyGeometryStore(geometryTool);
    }

    /**
     * Processes the data within a build-in element, handles only the build-in properties whose behaviour is defined in
     * the specification
     *
     * @param formatWithVersionNumber
     *            format with version number
     * @param elementWithKeyword
     *            element and keyword
     * @param elementProperties
     *            properties of the element
     * @param values
     *            parsed values contained in a single line of file
     * @param lineNumber
     *            number of the line in the PLY file being parsed
     * @param store
     *            geometry store to fill during the process
     */
    protected void processElementBuildInData(final FormatWithVersionNumber formatWithVersionNumber,
            final ElementWithKeyword elementWithKeyword, final Set<AbstractPropertyWithKeyword<?>> elementProperties,
            final double[] values, final int lineNumber, final PlyGeometryStore store) {
        Vector3 vertex = null;
        Vector3 normal = null;
        ColorRGBA color = null;
        PlyMaterial material = null;
        PlyEdgeInfo edgeInfo = null;
        PlyFaceInfo faceInfo = null;
        final Iterator<AbstractPropertyWithKeyword<?>> elementPropertiesIterator = elementProperties.iterator();
        final AbstractPropertyWithKeyword<?>[] valueProperties = new AbstractPropertyWithKeyword<?>[values.length];
        int valueIndex = 0;
        // loops on the properties
        while (valueIndex < values.length && elementPropertiesIterator.hasNext()) {
            final AbstractPropertyWithKeyword<?> elementProperty = elementPropertiesIterator.next();
            if (elementProperty instanceof ScalarPropertyWithKeyword) {
                valueProperties[valueIndex] = elementProperty;
                valueIndex++;
            } else if (elementProperty instanceof ListPropertyWithKeyword) {
                // sets it to null so that it's skipped later as the value at this index is only used to know how much
                // coordinates are in the list
                valueProperties[valueIndex] = null;
                // uses the list property in all concerned indices
                final int listSize = (int) values[valueIndex];
                for (int listIndex = 1; listIndex <= listSize; listIndex++) {
                    valueProperties[valueIndex + listIndex] = elementProperty;
                }
                valueIndex += 1 + listSize;
            }
        }
        valueIndex = 0;
        // loops on the real values
        for (final double value : values) {
            final AbstractPropertyWithKeyword<?> propertyWithKeyWord = valueProperties[valueIndex];
            // it can be null when a value indicates the number of values in a list
            if (propertyWithKeyWord != null) {
                final Element[] propertyElements = propertyWithKeyWord.getElements();
                if (propertyElements != null && propertyElements.length > 0
                        && Arrays.asList(propertyElements).contains(elementWithKeyword.getEnumKey())) {
                    switch (elementWithKeyword.getEnumKey()) {
                        case CUSTOM: {
                            throw new IllegalArgumentException("Custom data of the " + elementWithKeyword
                                    + " passed to the method responsible for treating build-in data");
                        }
                        case MATERIAL: {
                            if (propertyWithKeyWord instanceof ScalarPropertyWithKeyword) {
                                final ScalarProperty scalarProperty = ((ScalarPropertyWithKeyword) propertyWithKeyWord)
                                        .getEnumKey();
                                switch (scalarProperty) {
                                    case CUSTOM: {
                                        PlyImporter.LOGGER.log(Level.FINE, "Custom data of the " + propertyWithKeyWord
                                                + " skipped in the method responsible for treating build-in data");
                                        break;
                                    }
                                    case AMBIENT_RED: {
                                        if (material == null) {
                                            material = new PlyMaterial();
                                            store.getMaterialLibrary().add(material);
                                        }
                                        material.setAmbientRed((float) (value / 255.0d));
                                        break;
                                    }
                                    case AMBIENT_GREEN: {
                                        if (material == null) {
                                            material = new PlyMaterial();
                                            store.getMaterialLibrary().add(material);
                                        }
                                        material.setAmbientGreen((float) (value / 255.0d));
                                        break;
                                    }
                                    case AMBIENT_BLUE: {
                                        if (material == null) {
                                            material = new PlyMaterial();
                                            store.getMaterialLibrary().add(material);
                                        }
                                        material.setAmbientBlue((float) (value / 255.0d));
                                        break;
                                    }
                                    case AMBIENT_COEFF: {
                                        if (material == null) {
                                            material = new PlyMaterial();
                                            store.getMaterialLibrary().add(material);
                                        }
                                        material.setAmbientAlpha((float) value);
                                        break;
                                    }
                                    case DIFFUSE_RED: {
                                        if (material == null) {
                                            material = new PlyMaterial();
                                            store.getMaterialLibrary().add(material);
                                        }
                                        material.setDiffuseRed((float) (value / 255.0d));
                                        break;
                                    }
                                    case DIFFUSE_GREEN: {
                                        if (material == null) {
                                            material = new PlyMaterial();
                                            store.getMaterialLibrary().add(material);
                                        }
                                        material.setDiffuseGreen((float) (value / 255.0d));
                                        break;
                                    }
                                    case DIFFUSE_BLUE: {
                                        if (material == null) {
                                            material = new PlyMaterial();
                                            store.getMaterialLibrary().add(material);
                                        }
                                        material.setDiffuseBlue((float) (value / 255.0d));
                                        break;
                                    }
                                    case DIFFUSE_COEFF: {
                                        if (material == null) {
                                            material = new PlyMaterial();
                                            store.getMaterialLibrary().add(material);
                                        }
                                        material.setDiffuseAlpha((float) value);
                                        break;
                                    }
                                    case SPECULAR_RED: {
                                        if (material == null) {
                                            material = new PlyMaterial();
                                            store.getMaterialLibrary().add(material);
                                        }
                                        material.setSpecularRed((float) (value / 255.0d));
                                        break;
                                    }
                                    case SPECULAR_GREEN: {
                                        if (material == null) {
                                            material = new PlyMaterial();
                                            store.getMaterialLibrary().add(material);
                                        }
                                        material.setSpecularGreen((float) (value / 255.0d));
                                        break;
                                    }
                                    case SPECULAR_BLUE: {
                                        if (material == null) {
                                            material = new PlyMaterial();
                                            store.getMaterialLibrary().add(material);
                                        }
                                        material.setSpecularBlue((float) (value / 255.0d));
                                        break;
                                    }
                                    case SPECULAR_COEFF: {
                                        if (material == null) {
                                            material = new PlyMaterial();
                                            store.getMaterialLibrary().add(material);
                                        }
                                        material.setSpecularAlpha((float) value);
                                        break;
                                    }
                                    case SPECULAR_POWER: {
                                        if (material == null) {
                                            material = new PlyMaterial();
                                            store.getMaterialLibrary().add(material);
                                        }
                                        // not sure how to treat the specular power, 200 is the maximum specular value
                                        // material.setShininess((float) (128 * MathUtils.clamp(value, 0, 200) / 200));
                                        material.setShininess((float) value);
                                        break;
                                    }
                                    default:
                                        throw new UnsupportedOperationException(
                                                "Missing implementation: the " + propertyWithKeyWord
                                                        + " is not supported by the " + elementWithKeyword + " yet");
                                }
                            } else if (propertyWithKeyWord instanceof ListPropertyWithKeyword) {
                                final ListProperty listProperty = ((ListPropertyWithKeyword) propertyWithKeyWord)
                                        .getEnumKey();
                                switch (listProperty) {
                                    case CUSTOM: {
                                        PlyImporter.LOGGER.log(Level.FINE, "Custom data of the " + propertyWithKeyWord
                                                + " skipped in the method responsible for treating build-in data");
                                        break;
                                    }
                                    default:
                                        throw new UnsupportedOperationException(
                                                "Missing implementation: the " + propertyWithKeyWord
                                                        + " is not supported by the " + elementWithKeyword + " yet");
                                }
                            }
                            break;
                        }
                        case EDGE: {
                            if (propertyWithKeyWord instanceof ScalarPropertyWithKeyword) {
                                final ScalarProperty scalarProperty = ((ScalarPropertyWithKeyword) propertyWithKeyWord)
                                        .getEnumKey();
                                switch (scalarProperty) {
                                    case CUSTOM: {
                                        PlyImporter.LOGGER.log(Level.FINE, "Custom data of the " + propertyWithKeyWord
                                                + " skipped in the method responsible for treating build-in data");
                                        break;
                                    }
                                    case VERTEX1: {
                                        if (edgeInfo == null) {
                                            edgeInfo = new PlyEdgeInfo();
                                            store.addLine(edgeInfo);
                                        }
                                        final Integer edgeIndex1 = Integer.valueOf((int) value);
                                        edgeInfo.setIndex1(edgeIndex1);
                                        break;
                                    }
                                    case VERTEX2: {
                                        if (edgeInfo == null) {
                                            edgeInfo = new PlyEdgeInfo();
                                            store.addLine(edgeInfo);
                                        }
                                        final Integer edgeIndex2 = Integer.valueOf((int) value);
                                        edgeInfo.setIndex2(edgeIndex2);
                                        break;
                                    }
                                    case RED: {
                                        final ColorRGBA edgeColor;
                                        if (edgeInfo == null) {
                                            edgeColor = new ColorRGBA();
                                            edgeInfo = new PlyEdgeInfo();
                                            edgeInfo.setColor(edgeColor);
                                            store.addLine(edgeInfo);
                                        } else {
                                            if (edgeInfo.getColor() == null) {
                                                edgeColor = new ColorRGBA();
                                                edgeInfo.setColor(edgeColor);
                                            } else {
                                                edgeColor = edgeInfo.getColor();
                                            }
                                        }
                                        edgeColor.setRed((float) (value / 255.0d));
                                        break;
                                    }
                                    case GREEN: {
                                        final ColorRGBA edgeColor;
                                        if (edgeInfo == null) {
                                            edgeColor = new ColorRGBA();
                                            edgeInfo = new PlyEdgeInfo();
                                            edgeInfo.setColor(edgeColor);
                                            store.addLine(edgeInfo);
                                        } else {
                                            if (edgeInfo.getColor() == null) {
                                                edgeColor = new ColorRGBA();
                                                edgeInfo.setColor(edgeColor);
                                            } else {
                                                edgeColor = edgeInfo.getColor();
                                            }
                                        }
                                        edgeColor.setGreen((float) (value / 255.0d));
                                        break;
                                    }
                                    case BLUE: {
                                        final ColorRGBA edgeColor;
                                        if (edgeInfo == null) {
                                            edgeColor = new ColorRGBA();
                                            edgeInfo = new PlyEdgeInfo();
                                            edgeInfo.setColor(edgeColor);
                                            store.addLine(edgeInfo);
                                        } else {
                                            if (edgeInfo.getColor() == null) {
                                                edgeColor = new ColorRGBA();
                                                edgeInfo.setColor(edgeColor);
                                            } else {
                                                edgeColor = edgeInfo.getColor();
                                            }
                                        }
                                        edgeColor.setBlue((float) (value / 255.0d));
                                        break;
                                    }
                                    default:
                                        throw new UnsupportedOperationException(
                                                "Missing implementation: the " + propertyWithKeyWord
                                                        + " is not supported by the " + elementWithKeyword + " yet");
                                }
                            } else if (propertyWithKeyWord instanceof ListPropertyWithKeyword) {
                                final ListProperty listProperty = ((ListPropertyWithKeyword) propertyWithKeyWord)
                                        .getEnumKey();
                                switch (listProperty) {
                                    case CUSTOM: {
                                        PlyImporter.LOGGER.log(Level.FINE, "Custom data of the " + propertyWithKeyWord
                                                + " skipped in the method responsible for treating build-in data");
                                        break;
                                    }
                                    default:
                                        throw new UnsupportedOperationException(
                                                "Missing implementation: the " + propertyWithKeyWord
                                                        + " is not supported by the " + elementWithKeyword + " yet");
                                }
                            }
                            break;
                        }
                        case FACE: {
                            if (propertyWithKeyWord instanceof ScalarPropertyWithKeyword) {
                                final ScalarProperty scalarProperty = ((ScalarPropertyWithKeyword) propertyWithKeyWord)
                                        .getEnumKey();
                                switch (scalarProperty) {
                                    case CUSTOM: {
                                        PlyImporter.LOGGER.log(Level.FINE, "Custom data of the " + propertyWithKeyWord
                                                + " skipped in the method responsible for treating build-in data");
                                        break;
                                    }
                                    default:
                                        throw new UnsupportedOperationException(
                                                "Missing implementation: the " + propertyWithKeyWord
                                                        + " is not supported by the " + elementWithKeyword + " yet");
                                }
                            } else if (propertyWithKeyWord instanceof ListPropertyWithKeyword) {
                                final ListProperty listProperty = ((ListPropertyWithKeyword) propertyWithKeyWord)
                                        .getEnumKey();
                                switch (listProperty) {
                                    case CUSTOM: {
                                        PlyImporter.LOGGER.log(Level.FINE, "Custom data of the " + propertyWithKeyWord
                                                + " skipped in the method responsible for treating build-in data");
                                        break;
                                    }
                                    case VERTEX_INDICES: {
                                        if (faceInfo == null) {
                                            faceInfo = new PlyFaceInfo();
                                            store.addFace(faceInfo);
                                        }
                                        faceInfo.addVertexIndex((int) value);
                                        break;
                                    }
                                    case TEXCOORD: {
                                        if (faceInfo == null) {
                                            faceInfo = new PlyFaceInfo();
                                            store.addFace(faceInfo);
                                        }
                                        faceInfo.addTextureCoordinate((float) value);
                                        break;
                                    }
                                    case MATERIAL_INDICES: {
                                        if (faceInfo == null) {
                                            faceInfo = new PlyFaceInfo();
                                            store.addFace(faceInfo);
                                        }
                                        faceInfo.addMaterialIndex((int) value);
                                        break;
                                    }
                                    default:
                                        throw new UnsupportedOperationException(
                                                "Missing implementation: the " + propertyWithKeyWord
                                                        + " is not supported by the " + elementWithKeyword + " yet");
                                }
                            }
                            break;
                        }
                        case VERTEX: {
                            if (propertyWithKeyWord instanceof ScalarPropertyWithKeyword) {
                                final ScalarProperty scalarProperty = ((ScalarPropertyWithKeyword) propertyWithKeyWord)
                                        .getEnumKey();
                                switch (scalarProperty) {
                                    case CUSTOM: {
                                        PlyImporter.LOGGER.log(Level.FINE, "Custom data of the " + propertyWithKeyWord
                                                + " skipped in the method responsible for treating build-in data");
                                        break;
                                    }
                                    case X: {
                                        if (vertex == null) {
                                            vertex = new Vector3();
                                            store.getDataStore().getVertices().add(vertex);
                                        }
                                        vertex.setX(value);
                                        break;
                                    }
                                    case Y: {
                                        if (vertex == null) {
                                            vertex = new Vector3();
                                            store.getDataStore().getVertices().add(vertex);
                                        }
                                        vertex.setY(value);
                                        break;
                                    }
                                    case Z: {
                                        if (vertex == null) {
                                            vertex = new Vector3();
                                            store.getDataStore().getVertices().add(vertex);
                                        }
                                        vertex.setZ(value);
                                        break;
                                    }
                                    case NX: {
                                        if (normal == null) {
                                            normal = new Vector3();
                                            store.getDataStore().getNormals().add(normal);
                                        }
                                        normal.setX(value);
                                        break;
                                    }
                                    case NY: {
                                        if (normal == null) {
                                            normal = new Vector3();
                                            store.getDataStore().getNormals().add(normal);
                                        }
                                        normal.setY(value);
                                        break;
                                    }
                                    case NZ: {
                                        if (normal == null) {
                                            normal = new Vector3();
                                            store.getDataStore().getNormals().add(normal);
                                        }
                                        normal.setZ(value);
                                        break;
                                    }
                                    case RED: {
                                        if (color == null) {
                                            color = new ColorRGBA();
                                            store.getDataStore().getColors().add(color);
                                        }
                                        color.setRed((float) (value / 255.0d));
                                        break;
                                    }
                                    case GREEN: {
                                        if (color == null) {
                                            color = new ColorRGBA();
                                            store.getDataStore().getColors().add(color);
                                        }
                                        color.setGreen((float) (value / 255.0d));
                                        break;
                                    }
                                    case BLUE: {
                                        if (color == null) {
                                            color = new ColorRGBA();
                                            store.getDataStore().getColors().add(color);
                                        }
                                        color.setBlue((float) (value / 255.0d));
                                        break;
                                    }
                                    default:
                                        throw new UnsupportedOperationException(
                                                "Missing implementation: the " + propertyWithKeyWord
                                                        + " is not supported by the " + elementWithKeyword + " yet");
                                }
                            } else if (propertyWithKeyWord instanceof ListPropertyWithKeyword) {
                                final ListProperty listProperty = ((ListPropertyWithKeyword) propertyWithKeyWord)
                                        .getEnumKey();
                                switch (listProperty) {
                                    case CUSTOM: {
                                        PlyImporter.LOGGER.log(Level.FINE, "Custom data of the " + propertyWithKeyWord
                                                + " skipped in the method responsible for treating build-in data");
                                        break;
                                    }
                                    default:
                                        throw new UnsupportedOperationException(
                                                "Missing implementation: the " + propertyWithKeyWord
                                                        + " is not supported by the " + elementWithKeyword + " yet");
                                }
                            }
                            break;
                        }
                        default:
                            PlyImporter.LOGGER.log(Level.SEVERE, "Element '" + elementWithKeyword.getEnumKey() + "["
                                    + elementWithKeyword.getKeyword() + "]' unsupported on line " + lineNumber);
                    }
                } else {
                    throw new IllegalArgumentException(
                            "The " + propertyWithKeyWord + " is not supported by the " + elementWithKeyword);
                }
            }
            valueIndex++;
        }
    }

    /**
     * Processes the data within a custom element or within a build-in element with custom properties. The default
     * implementation just displays a warning message as it's up to the developer to manage the user-defined custom data
     * whose behaviour isn't defined in the specification
     *
     * @param formatWithVersionNumber
     *            format with version number
     * @param elementWithKeyword
     *            element and keyword
     * @param elementProperties
     *            properties of the element
     * @param values
     *            parsed values contained in a single line of file
     * @param lineNumber
     *            number of the line in the PLY file being parsed
     * @param store
     *            geometry store to fill during the process
     */
    protected void processElementCustomData(final FormatWithVersionNumber formatWithVersionNumber,
            final ElementWithKeyword elementWithKeyword, final Set<AbstractPropertyWithKeyword<?>> elementProperties,
            final double[] values, final int lineNumber, final PlyGeometryStore store) {
        PlyImporter.LOGGER.log(Level.WARNING,
                "'" + elementWithKeyword.getEnumKey().name() + "[" + elementWithKeyword.getKeyword()
                        + "]' ignored on line " + lineNumber + ". Override " + getClass().getCanonicalName()
                        + ".processElementCustomData() to remove this warning");
    }

    public void setModelLocator(final ResourceLocator locator) {
        _modelLocator = locator;
    }

    public void setTextureLocator(final ResourceLocator locator) {
        _textureLocator = locator;
    }

    public void setFlipTextureVertically(final boolean flipTextureVertically) {
        _flipTextureVertically = flipTextureVertically;
    }

    public boolean isFlipTextureVertically() {
        return _flipTextureVertically;
    }

    public void setUseCompression(final boolean useCompression) {
        _useCompression = useCompression;
    }

    public boolean isUseCompression() {
        return _useCompression;
    }

    public void setMinificationFilter(final MinificationFilter minificationFilter) {
        _minificationFilter = minificationFilter;
    }

    public MinificationFilter getMinificationFilter() {
        return _minificationFilter;
    }
}
