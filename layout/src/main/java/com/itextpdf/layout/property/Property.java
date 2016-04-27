/*
    $Id$

    This file is part of the iText (R) project.
    Copyright (c) 1998-2016 iText Group NV
    Authors: Bruno Lowagie, Paulo Soares, et al.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation with the addition of the
    following permission added to Section 15 as permitted in Section 7(a):
    FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
    ITEXT GROUP. ITEXT GROUP DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
    OF THIRD PARTY RIGHTS

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
    or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License
    along with this program; if not, see http://www.gnu.org/licenses or write to
    the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
    Boston, MA, 02110-1301 USA, or download the license from the following URL:
    http://itextpdf.com/terms-of-use/

    The interactive user interfaces in modified source and object code versions
    of this program must display Appropriate Legal Notices, as required under
    Section 5 of the GNU Affero General Public License.

    In accordance with Section 7(b) of the GNU Affero General Public License,
    a covered work must retain the producer line in every PDF that is created
    or manipulated using iText.

    You can be released from the requirements of the license by purchasing
    a commercial license. Buying such a license is mandatory as soon as you
    develop commercial activities involving the iText software without
    disclosing the source code of your own applications.
    These activities include: offering paid services to customers as an ASP,
    serving PDFs on the fly in a web application, shipping iText with a closed
    source product.

    For more information, please contact iText Software Corp. at this
    address: sales@itextpdf.com
 */
package com.itextpdf.layout.property;

import com.itextpdf.layout.IPropertyContainer;

/**
 * An enum of property names that are used for graphical properties of layout
 * elements. The {@link IPropertyContainer} performs the same function as an
 * {@link java.util.Map}, with the values of {@link Property} as its potential keys.
 */
public enum Property {

    ACTION,
    AREA_BREAK_TYPE,
    AUTO_SCALE,
    AUTO_SCALE_HEIGHT,
    AUTO_SCALE_WIDTH,
    BACKGROUND,
    BASE_DIRECTION,
    BOLD_SIMULATION,
    BORDER,
    BORDER_BOTTOM,
    BORDER_LEFT,
    BORDER_RIGHT,
    BORDER_TOP,
    BOTTOM,
    CHARACTER_SPACING,
    COLSPAN,
    DESTINATION,
    FIRST_LINE_INDENT,
    FLUSH_ON_DRAW,
    FONT,
    FONT_COLOR,
    FONT_KERNING,
    FONT_SCRIPT,
    FONT_SIZE,
    FULL,
    FORCED_PLACEMENT,
    HEIGHT,
    HORIZONTAL_ALIGNMENT,
    /**
     * Value of 1 is equivalent to no scaling
     **/
    HORIZONTAL_SCALING,
    HYPHENATION,
    ITALIC_SIMULATION,
    KEEP_TOGETHER,
    LEADING,
    LEFT,
    LINE_DRAWER,
    LIST_START,
    LIST_SYMBOL,
    LIST_SYMBOL_ALIGNMENT,
    LIST_SYMBOL_INDENT,
    LIST_SYMBOL_PRE_TEXT,
    LIST_SYMBOL_POST_TEXT,
    LIST_SYMBOLS_INITIALIZED,
    MARGIN_BOTTOM,
    MARGIN_LEFT,
    MARGIN_RIGHT,
    MARGIN_TOP,
    PADDING_BOTTOM,
    PADDING_LEFT,
    PADDING_RIGHT,
    PADDING_TOP,
    PAGE_NUMBER,
    POSITION,
    REVERSED,
    RIGHT,
    ROTATION_ANGLE,
    ROTATION_INITIAL_HEIGHT,
    ROTATION_INITIAL_WIDTH,
    ROTATION_POINT_X,
    ROTATION_POINT_Y,
    ROWSPAN,
    SPACING_RATIO,
    SPLIT_CHARACTERS,
    STROKE_COLOR,
    STROKE_WIDTH,
    SKEW,
    TAB_ANCHOR,
    TAB_DEFAULT,
    TAB_LEADER,
    TAB_STOPS,
    TEXT_ALIGNMENT,
    /**
     * Use values from .
     */
    TEXT_RENDERING_MODE,
    TEXT_RISE,
    TOP,
    UNDERLINE,
    /**
     * Value of 1 is equivalent to no scaling
     **/
    VERTICAL_ALIGNMENT,
    VERTICAL_SCALING,
    WIDTH,
    WORD_SPACING,
    X,
    Y
}
