package com.mindolph.mindmap.theme;

import com.mindolph.base.constant.FontConstants;
import com.mindolph.base.util.ColorUtils;
import javafx.scene.paint.Color;

/**
 * @author mindolph
 */
public class LightTheme extends MindMapTheme {

    public LightTheme() {
        textMargins = 10;
        otherLevelVerticalInset = 16;
        otherLevelHorizontalInset = 32;
        firstLevelVerticalInset = 32;
        firstLevelHorizontalInset = 48;
        paperMargins = 20;
        selectLineGap = 5;
        horizontalBlockGap = 5;
        drawBackground = true;
        paperColor = Color.web("0xEEEEEE88");
        gridColor = paperColor.darker();
        showGrid = false;
        gridStep = 0;
        rootBackgroundColor = Color.web("0x031A31");
        firstLevelBackgroundColor = Color.web("0xB1BFCC00");
        otherLevelBackgroundColor = Color.web("0xFFFFFF00");
        rootTextColor = Color.WHITE;
        firstLevelTextColor = Color.BLACK;
        otherLevelTextColor = Color.BLACK;

        connectorStyle = ConnectorStyle.BEZIER;
        connectorWidth = 2.0f;
        connectorColor = Color.GREY;

        collapsatorBorderColor = Color.DARKGRAY;
        collapsatorBackgroundColor = Color.WHITE;
        selectLineColor = Color.ORANGE;
        jumpLinkColor = Color.CYAN;

        showCollapsatorOnMouseHover = true;
        collapsatorBorderWidth = 1.0f;
        collapsatorSize = 16;

        selectLineWidth = 2.5f;
        jumpLinkWidth = 1.5f;
        roundRadius = 8f;
        topicFont = FontConstants.DEFAULT_FONTS.get(FontConstants.KEY_MMD_TOPIC_FONT);
        noteFont = FontConstants.DEFAULT_FONTS.get(FontConstants.KEY_MMD_NOTE_FONT);

        dropShadow = false;
        shadowColor = ColorUtils.makeTransparentColor(Color.BLACK, 0.188);
        shadowOffset = 5.0f;

        borderType = BorderType.LINE;
        elementBorderWidth = 0f;
        elementBorderColor = paperColor;

    }

}
