package com.mindolph.base.control;

import com.mindolph.base.constant.PrefConstants;
import com.mindolph.base.plugin.Generator;
import com.mindolph.base.plugin.InputHelper;
import com.mindolph.base.plugin.Plugin;
import com.mindolph.base.plugin.PluginManager;
import com.mindolph.base.util.EventUtils;
import com.mindolph.base.util.LayoutUtils;
import com.mindolph.mfx.util.BoundsUtils;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.reactfx.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftboot.util.pref.PreferenceManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Code area with smart editing support.
 *
 * @author mindolph.com@gmail.com
 */
public class SmartCodeArea extends ExtCodeArea {
    private static final Logger log = LoggerFactory.getLogger(SmartCodeArea.class);

    private StackPane inputDialog;

    private StackPane reframeDialog;

    private final EventSource<String> inputHelpSource = new EventSource<>();

    private final InputHelperManager inputHelperManager;

    // Indicate that whether the input method is using, if true, the input helper is paused until it becomes false.
    private boolean isInputMethod = false;

    private Pane parentPane;

    public SmartCodeArea() {
        super();
        this.inputHelperManager = new InputHelperManager(this.hashCode(), getFileType());
        this.bindCaretCoordinate();
        this.bindInputHelper();
    }

    @Override
    public void addFeatures(FEATURE... features) {
        super.addFeatures(features);
        List<InputMap<KeyEvent>> inputMaps = new ArrayList<>();

        inputMaps.add(InputMap.consume(EventPattern.keyPressed(KeyCode.UP), keyEvent -> {
            if (!isInputMethod) {
                if (isInputHelperEnabled()) {
                    inputHelperManager.consume(keyEvent, null);
                    if (!keyEvent.isConsumed()) {
                        // move caret implicitly
                        moveCaret(DIRECTION_UP);
                    }
                }
                else {
                    moveCaret(DIRECTION_UP);
                }
            }
        }));
        inputMaps.add(InputMap.consume(EventPattern.keyPressed(KeyCode.DOWN), keyEvent -> {
            if (!isInputMethod) {
                if (isInputHelperEnabled()) {
                    inputHelperManager.consume(keyEvent, null);
                    if (!keyEvent.isConsumed()) {
                        // move caret implicitly
                        moveCaret(DIRECTION_DOWN);
                    }
                }
                else {
                    moveCaret(DIRECTION_DOWN);
                }
            }
        }));
        inputMaps.add(InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER), keyEvent -> {
            if (!isInputMethod) {
                if (isInputHelperEnabled()) {
                    inputHelperManager.consume(keyEvent, extractLastWordFromCaret());
                    if (!keyEvent.isConsumed()) {
                        // line break implicitly
                        this.replaceSelection("\n");
                    }
                }
                else {
                    // line break implicitly
                    this.replaceSelection("\n");
                }
            }
        }));
        inputMaps.add(InputMap.consume(EventPattern.keyReleased(), keyEvent -> {
            if (isInputHelperEnabled() && !isInputMethod && !isUpOrDown(keyEvent)) {
                if (EventUtils.isEditableInput(keyEvent)) {
                    inputHelperManager.consume(keyEvent, extractLastWordFromCaret());
                }
            }
        }));
        Nodes.addInputMap(this, InputMap.sequence(inputMaps.toArray(new InputMap[]{})));
    }


    @Override
    protected ContextMenu createContextMenu() {
        ContextMenu menu = super.createContextMenu();
        withPlugins(plugin -> {
            Optional<Generator> opt = plugin.getGenerator();
            if (opt.isPresent()) {
                Generator generator = opt.get();
                MenuItem menuItem = generator.contextMenuItem(getSelectedText());
                menu.getItems().add(menuItem);
                menuItem.setOnAction(event -> {
                    this.closeGenerateDialog();
                    this.inputDialog = generator.inputDialog(this.hashCode());
                    this.showGenerateDialog();
                });

                generator.onGenerated(generatedText -> {
                    this.closeReframeDialog();
                    int origin = this.getSelection().getStart();
                    this.replaceSelection(generatedText);
                    log.debug(" select from %d to %d".formatted(origin, this.getCaretPosition()));
                    super.selectRange(origin, this.getCaretPosition());
                    this.closeGenerateDialog();
                    this.reframeDialog = generator.reframeDialog(this.hashCode());
                    this.showReframeDialog();
                });
                generator.onCancel(isNormally -> {
                    if (isNormally) {
                        this.closeGenerateDialog();
                    }
                });
                generator.onComplete(isKeep -> {
                    if (!isKeep) {
                        super.replaceSelection(StringUtils.EMPTY);
                    }
                    else {
                        super.selectRange(this.getCaretPosition(), this.getCaretPosition());
                    }
                    this.closeReframeDialog();
                });
            }
        });
        return menu;
    }

    // @since 1.7
    private void showGenerateDialog() {
        parentPane.getChildren().add(inputDialog);
        relocatedDialogToCaret(inputDialog);
    }

    // @since 1.7
    private void closeGenerateDialog() {
        log.debug("Closing generate dialog");
        parentPane.getChildren().remove(inputDialog);
        inputDialog = null;
        super.setEditable(true);
        super.setDisabled(false);
        super.requestFocus();
    }

    // @since 1.7
    private void showReframeDialog() {
        parentPane.getChildren().add(reframeDialog);
        super.setEditable(false);
        super.setDisabled(true);
        relocatedDialogToCaret(reframeDialog);
    }

    // @since 1.7
    private void closeReframeDialog() {
        log.debug("Closing reframe dialog");
        parentPane.getChildren().remove(reframeDialog);
        reframeDialog = null;
        super.setEditable(true);
        super.setDisabled(false);
        super.requestFocus();
    }

    // @since 1.7
    private void relocatedDialogToCaret(StackPane inputDialog) {
        Platform.runLater(() -> {
            Bounds hoverBounds = BoundsUtils.fromPoint(getDialogTargetPoint(), inputDialog.getWidth(), inputDialog.getHeight());
            Point2D p2 = LayoutUtils.bestLocation(this.getBoundsInParent(), hoverBounds, new Dimension2D(5, 5));
            inputDialog.relocate(p2.getX(), p2.getY());
            inputDialog.requestFocus();
        });
    }

    // @since 1.7
    private Point2D getDialogTargetPoint() {
        // calculate target point with x of left side border and y of caret bottom.
        Optional<Bounds> optBounds = getCharacterBoundsOnScreen(0, 0);
        Bounds leftSideBoundsInScreen = optBounds.orElse(BoundsUtils.newZero());
        Bounds caretBoundsInScreen = this.getCaretBounds().orElse(BoundsUtils.newZero());
        Point2D targetPointInScreen = new Point2D(leftSideBoundsInScreen.getMinX(), caretBoundsInScreen.getMaxY());
        return this.screenToLocal(targetPointInScreen);
    }

    // @since 1.7
    private void withPlugins(Consumer<Plugin> consumer) {
        Collection<Plugin> plugins = PluginManager.getIns().findPlugin(this.getFileType());
        for (Plugin plugin : plugins) {
            consumer.accept(plugin);
        }
    }

    // @since 1.7
    private void withGenerators(Consumer<Generator> consumer) {
        Collection<Plugin> plugins = PluginManager.getIns().findPlugin(this.getFileType());
        for (Plugin plugin : plugins) {
            Optional<Generator> opt = plugin.getGenerator();
            if (opt.isPresent()) {
                Generator generator = opt.get();
                consumer.accept(generator);
            }
        }
    }

    /**
     * Parent pane is for showing input helper.
     *
     * @param parentPane The pane must only contain the code area
     */
    public void withParentPane(Pane parentPane) {
        this.parentPane = parentPane;
        inputHelperManager.setParentPane(parentPane);
    }

    // @since 1.6
    private void bindCaretCoordinate() {
        this.caretBoundsProperty().addListener((observable, oldValue, newValue) -> {
            newValue.ifPresent(bounds -> {
                inputHelperManager.updateCaret(this, bounds.getMaxX(), bounds.getMaxY());
            });
        });
    }

    // @since 1.6
    private void bindInputHelper() {
        // delay update context text to reduce redundant calculating.
        // TODO better way is to do updating when new word completed and any other actions that makes text change completed.
        inputHelpSource.reduceSuccessions((s, s2) -> s2, Duration.ofMillis(INPUT_HELP_DELAY_IN_MILLIS))
                .subscribe(s -> {
                    // non-blocking
                    new Thread(() -> {
                        Collection<Plugin> plugins = PluginManager.getIns().findPlugin(getFileType());
                        for (Plugin plugin : plugins) {
                            Optional<InputHelper> opt = plugin.getInputHelper();
                            opt.ifPresent(inputHelper -> inputHelper.updateContextText(this.hashCode(), s));
                        }
                    }
                    ).start();
                });

        // prepare the context words
        this.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!isInputHelperEnabled()) {
                return;
            }
            if (!StringUtils.equals(oldValue, newValue)) inputHelpSource.push(newValue);
        });

        // stop helping when paragraph is changed by like mouse click.
        this.currentParagraphProperty().addListener((observable, oldValue, newValue) -> {
            if (!isInputHelperEnabled()) {
                return;
            }
            inputHelperManager.consume(InputHelperManager.UNKNOWN_INPUT, null);
        });

        // Insert selected text from input helper.
        inputHelperManager.onSelected((selection) -> {
            if (StringUtils.startsWithIgnoreCase(selection.selected(), selection.input())) {
                int start = selection.input().length();
                this.deleteText(this.getCaretPosition() - start, this.getCaretPosition());
                this.insertText(this.getCaretPosition(), selection.selected());
//                this.insertText(this.getCaretPosition(), StringUtils.substring(selection.selected(), start));
            }
            this.requestFocus(); // take back focus from input helper
        });
    }

    @Override
    protected void handleInputMethodEvent(InputMethodEvent event) {
        super.handleInputMethodEvent(event);
//        if (!isInputHelperEnabled()) {
//            log.debug("'%s'%n", event.getCommitted());
//            return;
//        }
        if (StringUtils.isBlank(event.getCommitted())) {
            isInputMethod = true;
            log.debug("in input method");
            this.insertText(this.getCaretPosition(), event.getCommitted());
        }
        else {
            isInputMethod = false;
            log.debug("not in input method with: " + event.getCommitted());
            inputHelperManager.consume(InputHelperManager.UNKNOWN_INPUT, extractLastWordFromCaret());
        }
    }


    private boolean isUpOrDown(KeyEvent keyEvent) {
        return KeyCode.UP.equals(keyEvent.getCode()) || KeyCode.DOWN.equals(keyEvent.getCode());
    }

    private boolean isInputHelperEnabled() {
        return PreferenceManager.getInstance().getPreference(PrefConstants.GENERAL_EDITOR_ENABLE_INPUT_HELPER, true);
    }

}
