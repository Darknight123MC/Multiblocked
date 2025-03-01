package com.lowdragmc.multiblocked.api.gui.dialogs;

import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib.client.utils.RenderUtils;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectorWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;
import com.lowdragmc.lowdraglib.gui.widget.TabButton;
import com.lowdragmc.lowdraglib.gui.widget.TabContainer;
import com.lowdragmc.lowdraglib.gui.widget.TextBoxWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.BlockPosFace;
import com.lowdragmc.lowdraglib.utils.CycleItemStackHandler;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.block.BlockComponent;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.api.pattern.JsonBlockPattern;
import com.lowdragmc.multiblocked.api.pattern.predicates.PredicateComponent;
import com.lowdragmc.multiblocked.api.pattern.predicates.SimplePredicate;
import com.lowdragmc.multiblocked.api.pattern.util.RelativeDirection;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.registry.MbdPredicates;
import com.lowdragmc.multiblocked.api.tile.part.PartTileEntity;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import com.lowdragmc.multiblocked.client.renderer.impl.CycleBlockStateRenderer;
import com.lowdragmc.multiblocked.client.renderer.impl.MBDBlockStateRenderer;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class JsonBlockPatternWidget extends DialogWidget {
    public static BlockComponent symbolBlock;
    public JsonBlockPattern pattern;
    public TabContainer container;
    public BlockPatternSceneWidget sceneWidget;
    public SelectorWidget[] selectors;
    public TextFieldWidget[] repeats;
    public WidgetGroup bottomInfoGroup;
    public DraggableScrollableWidgetGroup symbolSelector;
    public DraggableScrollableWidgetGroup predicateGroup;
    public DraggableScrollableWidgetGroup tfGroup;
    public TextBoxWidget textBox;
    public boolean needUpdatePredicateSelector;
    public boolean isPretty;

    public JsonBlockPatternWidget(WidgetGroup parent, JsonBlockPattern pattern, Consumer<JsonBlockPattern> onClose) {
        super(parent, true);
        setParentInVisible();
        this.setOnClosed(()->onClose.accept(null));
        this.pattern = pattern;
        this.addWidget(new ImageWidget(0, 0, getSize().width, getSize().height, new ResourceTexture("multiblocked:textures/gui/json_block_pattern.png")));
        this.addWidget(bottomInfoGroup = new WidgetGroup(0, 0, getSize().width, getSize().height));
        this.addWidget(sceneWidget = new BlockPatternSceneWidget());
        this.addWidget(container = new TabContainer(0, 0, 384, 256));
        this.addWidget(new ButtonWidget(280, 29, 70, 20, cd -> {
            if (onClose != null) onClose.accept(pattern);
            if (isParentInVisible) {
                for (Widget widget : parent.widgets) {
                    widget.setVisible(true);
                    widget.setActive(true);
                }
            }
            parent.waitToRemoved(this);
        }).setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("multiblocked.gui.label.save_pattern", -1).setDropShadow(true)).setHoverBorderTexture(1, -1));

        // patternTab
        ResourceTexture tabPattern = new ResourceTexture("multiblocked:textures/gui/tab_pattern.png");
        WidgetGroup patternTab;
        container.addTab((TabButton) new TabButton(171, 29, 20, 20)
                        .setTexture(tabPattern.getSubTexture(0, 0, 1, 0.5), tabPattern.getSubTexture(0, 0.5, 1, 0.5))
                        .setHoverTooltips("multiblocked.gui.dialogs.pattern.pattern"),
                patternTab = new WidgetGroup(0, 0, getSize().width, getSize().height));

        int bgColor = 0x8f111111;

        patternTab.addWidget(new LabelWidget(174, 92, "multiblocked.gui.label.repeat"));
        repeats = new TextFieldWidget[2];
        patternTab.addWidget(new ImageWidget(266, 86, 29, 18, new ResourceTexture("multiblocked:textures/gui/repeat.png")).setHoverTooltips("multiblocked.gui.dialogs.pattern.repeat"));
        patternTab.addWidget(repeats[0] = new TextFieldWidget(215, 87, 40, 15, () -> sceneWidget.selected == null ? "" : pattern.aisleRepetitions[sceneWidget.selected.a][0] + "", s -> {
            if (sceneWidget.selected != null && sceneWidget.centerOffset[0] != sceneWidget.selected.a) {
                pattern.aisleRepetitions[sceneWidget.selected.a][0] = Integer.parseInt(s);
                if (pattern.aisleRepetitions[sceneWidget.selected.a][0] > pattern.aisleRepetitions[sceneWidget.selected.a][1]) {
                    pattern.aisleRepetitions[sceneWidget.selected.a][1] = pattern.aisleRepetitions[sceneWidget.selected.a][0];
                }
            }
        }).setNumbersOnly(1, Integer.MAX_VALUE));

        patternTab.addWidget(repeats[1] = new TextFieldWidget(305, 87, 40, 15, () -> sceneWidget.selected == null ? "" : pattern.aisleRepetitions[sceneWidget.selected.a][1] + "", s -> {
            if (sceneWidget.selected != null && sceneWidget.centerOffset[0] != sceneWidget.selected.a) {
                pattern.aisleRepetitions[sceneWidget.selected.a][1] = Integer.parseInt(s);
                if (pattern.aisleRepetitions[sceneWidget.selected.a][0] > pattern.aisleRepetitions[sceneWidget.selected.a][1]) {
                    pattern.aisleRepetitions[sceneWidget.selected.a][0] = pattern.aisleRepetitions[sceneWidget.selected.a][1];
                }
            }
        }).setNumbersOnly(0, Integer.MAX_VALUE));
        repeats[0].setActive(false); repeats[1].setActive(false); repeats[0].setHoverTooltips("multiblocked.gui.tips.min"); repeats[1].setHoverTooltips("multiblocked.gui.tips.max");

        patternTab.addWidget(symbolSelector = new DraggableScrollableWidgetGroup(215, 105, 130, 35).setBackground(new ColorRectTexture(bgColor)));
        patternTab.addWidget(new ButtonWidget(174, 105, 35, 35, (cd) -> {
            char next = (char) (pattern.symbolMap.keySet().stream().max(Comparator.comparingInt(a -> a)).get() + 1);
            pattern.symbolMap.put(next, new HashSet<>());
            updateSymbolButton();
        }).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/button_wood.png"), new TextTexture("multiblocked.gui.tips.add", -1).setDropShadow(true).setWidth(40)).setHoverBorderTexture(1, -1));
        updateSymbolButton();
        patternTab.addWidget(new LabelWidget(174, 143, "multiblocked.gui.label.tips"));

        List<String> candidates = Arrays.stream(RelativeDirection.values()).map(Enum::name).collect(Collectors.toList());
        patternTab.addWidget(new LabelWidget(174, 70, "multiblocked.gui.label.dir"));
        patternTab.addWidget(new ImageWidget(193, 60, 20, 20, new ResourceTexture("multiblocked:textures/gui/axis.png")).setHoverTooltips("multiblocked.gui.dialogs.pattern.direction"));
        patternTab.addWidget(new ImageWidget(215, 57, 40, 10, new TextTexture("multiblocked.gui.dialogs.pattern.char", -1).setDropShadow(true)));
        patternTab.addWidget(new ImageWidget(260, 57, 40, 10, new TextTexture("multiblocked.gui.dialogs.pattern.string", -1).setDropShadow(true)));
        patternTab.addWidget(new ImageWidget(305, 57, 40, 10, new TextTexture("multiblocked.gui.dialogs.pattern.aisle", -1).setDropShadow(true)));
        selectors = new SelectorWidget[3];
        patternTab.addWidget(selectors[0] = new SelectorWidget(215, 67, 40, 15, candidates, -1).setOnChanged(s->this.onDirChange(0, s)).setButtonBackground(new ColorRectTexture(bgColor)).setValue(pattern.structureDir[0].name()));
        patternTab.addWidget(selectors[1] = new SelectorWidget(260, 67, 40, 15, candidates, -1).setOnChanged(s->this.onDirChange(1, s)).setButtonBackground(new ColorRectTexture(bgColor)).setValue(pattern.structureDir[1].name()));
        patternTab.addWidget(selectors[2] = new SelectorWidget(305, 67, 40, 15, candidates, -1).setOnChanged(s->this.onDirChange(2, s)).setButtonBackground(new ColorRectTexture(bgColor)).setValue(pattern.structureDir[2].name()));

        //predicateTab
        ResourceTexture tabPredicate = new ResourceTexture("multiblocked:textures/gui/tab_predicate.png");
        WidgetGroup predicateTab;
        container.addTab((TabButton) new TabButton(171 + 25, 29, 20, 20)
                        .setTexture(tabPredicate.getSubTexture(0, 0, 1, 0.5),
                                tabPredicate.getSubTexture(0, 0.5, 1, 0.5))
                        .setHoverTooltips("multiblocked.gui.dialogs.pattern.predicate"),
                predicateTab = new WidgetGroup(0, 0, getSize().width, getSize().height));
        DraggableScrollableWidgetGroup predicatesContainer = new DraggableScrollableWidgetGroup(171, 52, 179, 136 - 52)
                .setBackground(new ColorRectTexture(bgColor))
                .setYScrollBarWidth(4)
                .setYBarStyle(null, new ColorRectTexture(-1));
        predicateTab.addWidget(predicatesContainer);
        AtomicReference<PredicateWidget> selectedPredicate = new AtomicReference<>();
        SelectorWidget sw;
        TextFieldWidget fw;
        predicateTab.addWidget(sw = new SelectorWidget(172, 138, 60, 16, new ArrayList<>(MbdPredicates.PREDICATE_REGISTRY.keySet()), -1)
                .setButtonBackground(new ColorRectTexture(bgColor)).setValue("blocks"));
        predicateTab.addWidget(fw = new TextFieldWidget(232, 138, 50, 16,  null, null));
        predicateTab.addWidget(new ButtonWidget(285, 138, 16, 16, cd -> {
            if (sw.getValue() == null) return;
            SimplePredicate predicate = MbdPredicates.createPredicate(sw.getValue());
            String predicateName = fw.getCurrentString();
            if (predicate != null && !pattern.predicates.containsKey(predicateName)) {
                pattern.predicates.put(fw.getCurrentString(), predicate);
                predicatesContainer.addWidget(new PredicateWidget(0, predicatesContainer.widgets.size() * 21, predicate, predicateName, selectedPredicate));
            }
        }).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/add.png")).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.dialogs.pattern.create_predicate"));
        predicateTab.addWidget(new ButtonWidget(350 - 36, 138, 16, 16, cd -> {
            if (selectedPredicate.get() == null) return;
            String name = selectedPredicate.get().name;
            if (sceneWidget.selected != null && !name.equals("controller")) {
                pattern.symbolMap.get(sceneWidget.selected.symbol).add(name);
                needUpdatePredicateSelector = true;
                for (SymbolTileEntity tile : sceneWidget.tiles.values()) {
                    tile.updateRenderer();
                }
                if (sceneWidget != null) {
                    sceneWidget.needCompileCache();
                }
            }
        }).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/move_down.png")).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.dialogs.pattern.add_predicate"));
        predicateTab.addWidget(new ButtonWidget(350 - 18, 138, 16, 16, cd -> {
            if (selectedPredicate.get() == null) return;
            String name = selectedPredicate.get().name;
            if (sceneWidget.selected != null) {
                pattern.symbolMap.get(sceneWidget.selected.symbol).remove(name);
            }
            pattern.symbolMap.values().forEach(set->set.remove(name));
            pattern.predicates.remove(name);
            needUpdatePredicateSelector = true;
            boolean found = false;
            for (Widget widget : predicatesContainer.widgets) {
                if (found) {
                    widget.addSelfPosition(0, -21);
                    widget.setVisible(true);
                } else if (widget == selectedPredicate.get()) {
                    predicatesContainer.waitToRemoved(widget);
                    found = true;
                }
            }
            for (SymbolTileEntity tile : sceneWidget.tiles.values()) {
                tile.updateRenderer();
            }
            if (sceneWidget != null) {
                sceneWidget.needCompileCache();
            }
        }).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/remove.png")).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.dialogs.pattern.remove_predicate"));
        pattern.predicates.forEach((predicateName, predicate) -> {
            if (predicateName.equals("controller")) return;
            predicatesContainer.addWidget(new PredicateWidget(0, predicatesContainer.widgets.size() * 21, predicate, predicateName, selectedPredicate));
        });

        //textFieldTab
        ResourceTexture tabTextField = new ResourceTexture("multiblocked:textures/gui/tab_text_field.png");
        WidgetGroup textFieldTab;
        container.addTab((TabButton) new TabButton(171 + 50, 29, 20, 20)
                        .setTexture(tabTextField.getSubTexture(0, 0, 1, 0.5),
                                tabTextField.getSubTexture(0, 0.5, 1, 0.5))
                        .setHoverTooltips("multiblocked.gui.tips.json"),
                textFieldTab = new WidgetGroup(0, 0, getSize().width, getSize().height));
        textFieldTab.addWidget(new ImageWidget(171, 52, 179, 20, ResourceBorderTexture.BAR));
        textFieldTab.addWidget(new SwitchWidget(173, 54, 16, 16, (cd, r) -> {
            isPretty = r;
            updatePatternJson();
        }).setHoverBorderTexture(1, -1).setTexture(new ResourceTexture("multiblocked:textures/gui/pretty.png"), new ResourceTexture("multiblocked:textures/gui/pretty_active.png")).setHoverTooltips("multiblocked.gui.tips.pretty"));
        textFieldTab.addWidget(new ButtonWidget(193, 54, 16, 16, cd -> Minecraft.getInstance().keyboardHandler.setClipboard(isPretty ? Multiblocked.prettyJson(getPatternJson()) : getPatternJson())).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/copy.png")).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.tips.copy"));
        textFieldTab.addWidget(tfGroup = new DraggableScrollableWidgetGroup(171, 72, 179, 136 - 52)
                .setBackground(new ColorRectTexture(bgColor))
                .setYScrollBarWidth(4)
                .setYBarStyle(null, new ColorRectTexture(-1)));
        tfGroup.addWidget(textBox = new TextBoxWidget(0, 0, 175, Collections.singletonList("")).setFontColor(-1).setShadow(true));
        container.setOnChanged((a, b)->{
            if (b == textFieldTab) {
                updatePatternJson();
            }
        });

        //information
        bottomInfoGroup.addWidget(new LabelWidget(31, 166, () -> LocalizationUtils.format("multiblocked.gui.label.symbol") + " " + (sceneWidget.selected == null ? "" : ("'" + sceneWidget.selected.symbol + "'"))).setTextColor(-1));
        bottomInfoGroup.addWidget(new LabelWidget(31, 178, () -> {
            if (sceneWidget.selected == null) return "Aisle Repetition: ";
            return String.format("Aisle Index: %d", sceneWidget.selected.a);
        }).setTextColor(-1));
        bottomInfoGroup. addWidget(new LabelWidget(31, 190, () -> {
            if (sceneWidget.selected == null) return "Aisle Repetition: ";
            int[] repeat = pattern.aisleRepetitions[sceneWidget.selected.a];
            return String.format("Aisle Repetition: (%d, %d)", repeat[0], repeat[1]);
        }).setTextColor(-1));
        bottomInfoGroup.addWidget(predicateGroup = new DraggableScrollableWidgetGroup(171, 166, 179, 58)
                .setBackground(new ColorRectTexture(bgColor))
                .setYScrollBarWidth(4)
                .setYBarStyle(null, new ColorRectTexture(-1)));

        updatePredicateSelector();
        for (SymbolTileEntity tile : sceneWidget.tiles.values()) {
            tile.updateRenderer();
        }
        if (sceneWidget != null) {
            sceneWidget.needCompileCache();
        }
    }

    public void updatePatternJson() {
        textBox.setContent(Collections.singletonList(isPretty ? Multiblocked.prettyJson(getPatternJson()) : getPatternJson()));
        tfGroup.computeMax();
    }

    public String getPatternJson() {
        return pattern.toJson();
    }

    public static int getColor(char symbol) {
        switch (symbol) {
            case '@' : return 0xff0000ff;
            case ' ' : return 0xff4EEDF7;
            case '-' : return 0xff1E2DF7;
            default: {
                return TextFormatting.values()[(symbol - 'A') % 16].getColor() | 0xff000000;
            }
        }
    }

    private void updatePredicateSelector() {
        predicateGroup.clearAllWidgets();
        if (sceneWidget.selected != null && pattern.symbolMap.containsKey(sceneWidget.selected.symbol)) {
            for (String predicateName : pattern.symbolMap.get(sceneWidget.selected.symbol)) {
                SimplePredicate predicate =  pattern.predicates.get(predicateName);
                if (predicate != null) {
                    predicateGroup.addWidget(new PredicateWidget(0, predicateGroup.widgets.size() * 21, predicate, predicateName, name -> {
                        if (sceneWidget.selected != null) {
                            pattern.symbolMap.get(sceneWidget.selected.symbol).remove(name);
                            needUpdatePredicateSelector = true;
                            for (SymbolTileEntity tile : sceneWidget.tiles.values()) {
                                tile.updateRenderer();
                            }
                            if (sceneWidget != null) {
                                sceneWidget.needCompileCache();
                            }
                        }
                    }));
                }
            }

        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (needUpdatePredicateSelector) {
            updatePredicateSelector();
            needUpdatePredicateSelector = false;
        }
    }

    private void updateSymbolButton() {
        symbolSelector.clearAllWidgets();
        int i = 0;
        for (final char c : pattern.symbolMap.keySet().stream().sorted().collect(Collectors.toList())) {
            symbolSelector.addWidget(new SymbolButton(13 * (i % 9) + 8, 13 * (i / 9) + 6, 10, 10, c, sceneWidget, cd -> {
                if (sceneWidget.selected != null && c != '@' && sceneWidget.selected.symbol != '@') {
                    sceneWidget.selected.symbol = c;
                    sceneWidget.selected.updateRenderer();
                    String old = pattern.pattern[sceneWidget.selected.a][sceneWidget.selected.b];
                    String newString = old.substring(0, sceneWidget.selected.c) + c + old.substring(sceneWidget.selected.c + 1);
                    pattern.pattern[sceneWidget.selected.a][sceneWidget.selected.b] = newString;
                    sceneWidget.updateCharacterView();
                    BlockPos pos = sceneWidget.selected.getBlockPos();
                    sceneWidget.onSelected(pos, Direction.NORTH);
                    sceneWidget.onSelected(pos, Direction.NORTH);
                }
            }).setHoverTooltips(c == '@' ? "controller" : c == ' ' ? "any" : c == '-' ? "air" : null));
            i++;
        }
        if (sceneWidget != null) {
            sceneWidget.needCompileCache();
        }
    }

    private void onDirChange(int index, String candidate) {
        RelativeDirection dir = RelativeDirection.valueOf(candidate);
        RelativeDirection[] newDirs = new RelativeDirection[3];
        newDirs[index] = dir;
        for (int i = 0; i < pattern.structureDir.length; i++) {
            if (pattern.structureDir[i].isSameAxis(dir) && i != index) {
                newDirs[i] = pattern.structureDir[index];
            } else if (i != index) {
                newDirs[i] = pattern.structureDir[i];
            }
            selectors[i].setValue(newDirs[i].name());
        }
        pattern.changeDir(newDirs[0], newDirs[1], newDirs[2]);
        sceneWidget.reloadBlocks();
    }

    public static void registerBlock() {
        PartDefinition definition = new PartDefinition(new ResourceLocation(Multiblocked.MODID, "symbol"), SymbolTileEntity::new);
        definition.properties.isOpaque = false;
        definition.properties.tabGroup = null;
        definition.showInJei = false;
        MbdComponents.registerComponent(definition);
        symbolBlock = MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(definition.location);
    }

    public class BlockPatternSceneWidget extends SceneWidget {
        public Map<BlockPos, SymbolTileEntity> tiles = new HashMap<>();
        public Set<SymbolTileEntity> sameSymbol = new HashSet<>();
        public Set<SymbolTileEntity> sameAisle = new HashSet<>();
        public SymbolTileEntity selected;
        public int[] centerOffset = new int[3];
        public int offset;
        public TextTexture texture;
        public int aisleRender = -1;
        public int viewMode;
        public DraggableScrollableWidgetGroup characterView;
        public WidgetGroup layerSwitch;

        TrackedDummyWorld world;
        public BlockPatternSceneWidget() {
            super(31, 31, 125, 125, null);
            texture = new TextTexture("", -1).setWidth(125).setType(TextTexture.TextType.ROLL);
            addWidget(characterView = new DraggableScrollableWidgetGroup(0, 0, 125, 125));
            reloadBlocks();
            addWidget(layerSwitch = new WidgetGroup(0, 0, 125, 125));
            layerSwitch.addWidget(new ImageWidget(5, 0, 125, 20, texture));
            layerSwitch.addWidget(new ButtonWidget(5, 50, 10, 10, new ResourceTexture("multiblocked:textures/gui/up.png"), cd -> addAisle(1)).setHoverTooltips("multiblocked.gui.dialogs.pattern.next_aisle"));
            layerSwitch. addWidget(new LabelWidget(5, 60, () -> aisleRender == -1 ? "all" : aisleRender + "").setTextColor(-1));
            layerSwitch.addWidget(new ButtonWidget(5, 70, 10, 10, new ResourceTexture("multiblocked:textures/gui/down.png"), cd -> addAisle(-1)).setHoverTooltips("multiblocked.gui.dialogs.pattern.last_aisle"));

            addWidget(new ButtonWidget(110, 110, 10, 10, new ResourceTexture("multiblocked:textures/gui/button_view.png"), this::switchPatternView).setHoverTooltips("multiblocked.gui.tips.switch_view"));
        }

        public void updateCharacterView () {
            characterView.clearAllWidgets();
            int x = 5, y = 5;
            for (int i = 0; i < pattern.pattern.length; i++) {
                for (int j = 0; j < pattern.pattern[0].length; j++) {
                    for (int k = 0; k < pattern.pattern[0][0].length(); k++) {
                        char c = pattern.pattern[i][j].charAt(k);
                        BlockPos pos = pattern.getActualPosOffset(k - centerOffset[2], j - centerOffset[1], i - centerOffset[0], Direction.NORTH).offset(offset, offset, offset);
                        characterView.addWidget(new SymbolButton(x, y, 10, 10, c, this, cd -> onSelected(pos, Direction.NORTH)));
                        x += 13;
                    }
                    x += 5;
                }
                y += 13;
                x = 5;
            }
            characterView.setVisible(viewMode == 2);
        }

        private void switchPatternView(ClickData clickData) {
            viewMode = (viewMode + 1) % 3;
            if (viewMode == 2) {
                characterView.setVisible(true);
                characterView.setActive(true);
                layerSwitch.setVisible(false);
                layerSwitch.setActive(false);
            } else {
                characterView.setVisible(false);
                characterView.setActive(false);
                layerSwitch.setVisible(true);
                layerSwitch.setActive(true);
            }
            if (viewMode == 1 || viewMode == 2) {
                sceneWidget.needCompileCache();
            }
        }

        private void addAisle(int add) {
            if (aisleRender + add >= -1 && aisleRender + add < pattern.pattern.length) {
                aisleRender += add;
            }
            sceneWidget.needCompileCache();
        }

        public void updateTips(String tips) {
            texture.updateText(tips);
        }

        public void reloadBlocks() {
            updateTips("");
            aisleRender = -1;
            selected = null;
            tiles.clear();
            sameSymbol.clear();
            sameAisle.clear();
            createScene(world = new TrackedDummyWorld());
            useCacheBuffer();
            world.setRenderFilter(pos -> {
                if (aisleRender > -1) {
                    return tiles.containsKey(pos) && tiles.get(pos).a == aisleRender;
                }
                return true;
            });
            centerOffset = pattern.getCenterOffset();
            String[][] pattern = JsonBlockPatternWidget.this.pattern.pattern;
            Set<BlockPos> posSet = new HashSet<>();
            offset = Math.max(pattern.length, Math.max(pattern[0].length, pattern[0][0].length()));
            for (int i = 0; i < pattern.length; i++) {
                for (int j = 0; j < pattern[0].length; j++) {
                    for (int k = 0; k < pattern[0][0].length(); k++) {
                        char c = pattern[i][j].charAt(k);
                        BlockPos pos = JsonBlockPatternWidget.this.pattern.getActualPosOffset(k - centerOffset[2], j - centerOffset[1], i - centerOffset[0], Direction.NORTH).offset(offset, offset, offset);
                        world.addBlock(pos, BlockInfo.fromBlockState(symbolBlock.defaultBlockState()));
                        SymbolTileEntity tileEntity = (SymbolTileEntity) world.getBlockEntity(pos);
                        assert tileEntity != null;
                        tileEntity.init(c, JsonBlockPatternWidget.this, i, j, k);
                        tileEntity.setDefinition((PartDefinition) symbolBlock.definition);
                        tileEntity.setLevelAndPosition(world, pos);
                        posSet.add(pos);
                        tiles.put(pos, tileEntity);
                        tileEntity.updateRenderer();
                    }
                }
            }
            setRenderedCore(posSet, null);
            setOnSelected(this::onSelected);
            setRenderFacing(false);
            updateCharacterView();
            needCompileCache();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void renderBlockOverLay(WorldSceneRenderer renderer) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuilder();
            RenderSystem.enableBlend();
            RenderSystem.disableTexture();

            if (viewMode == 1) { // render pattern style
                RenderUtils.useLightMap(240, 240, () -> {
                    if (selected != null) {
                        for (SymbolTileEntity tile : sameSymbol) {
                            drawSymbolTE(tessellator, buffer, tile, getColor(tile.symbol), 1);
                        }
                    }

                    if (selected != null) {
                        RenderSystem.depthMask(false);
                    }
                    for (SymbolTileEntity tile : tiles.values()) {
                        if (aisleRender > -1 && tile.a != aisleRender) continue;
                        if (sameSymbol.contains(tile)) continue;
                        float dd = Math.abs(System.currentTimeMillis() % 3000);
                        drawSymbolTE(tessellator, buffer, tile, getColor(tile.symbol), selected == null ? 1 : ((((dd > 1500) ? (3000 - dd) : dd) / 1500f) * 0.3f));
                    }
                    if (selected != null) {
                        RenderSystem.depthMask(true);
                    }
                });
            }

            super.renderBlockOverLay(renderer);

            RenderSystem.enableBlend();
            RenderSystem.disableTexture();

            if (selected != null) {
                int[] minPos = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
                int[] maxPos = new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
                for (SymbolTileEntity symbol : sameAisle) {
                    BlockPos pos = symbol.getBlockPos();
                    if (pos.getX() > maxPos[0]) maxPos[0] = pos.getX();
                    if (pos.getY() > maxPos[1]) maxPos[1] = pos.getY();
                    if (pos.getZ() > maxPos[2]) maxPos[2] = pos.getZ();

                    if (pos.getX() < minPos[0]) minPos[0] = pos.getX();
                    if (pos.getY() < minPos[1]) minPos[1] = pos.getY();
                    if (pos.getZ() < minPos[2]) minPos[2] = pos.getZ();
                }
                RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                RenderUtils.renderCubeFace(new MatrixStack(), buffer, minPos[0] - 0.01f, minPos[1] - 0.01f, minPos[2] - 0.01f, maxPos[0] + 1.01f, maxPos[1] + 1.01f, maxPos[2] + 1.01f, 0.3f, 0.5f, 0.7f, 1);
                tessellator.end();

                RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
            RenderSystem.enableTexture();

        }

        private void drawSymbolTE(Tessellator tessellator, BufferBuilder buffer, SymbolTileEntity tile, int color, float a) {
            float r = ((color & 0xFF0000) >> 16) / 255f;
            float g = ((color & 0xFF00) >> 8) / 255f;
            float b = ((color & 0xFF)) / 255f;
            float scale = 0.8f;
            MatrixStack matrixStack = new MatrixStack();
            matrixStack.translate((tile.getBlockPos().getX() + 0.5), (tile.getBlockPos().getY() + 0.5), (tile.getBlockPos().getZ() + 0.5));
            matrixStack.scale(scale, scale, scale);

            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            RenderBufferUtils.renderCubeFace(matrixStack, buffer, -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, r, g, b, a, true);
            tessellator.end();

            matrixStack.scale(1 / scale, 1 / scale, 1 / scale);
            matrixStack.translate(-(tile.getBlockPos().getX() + 0.5), -(tile.getBlockPos().getY() + 0.5), -(tile.getBlockPos().getZ() + 0.5));
        }

        private void onSelected(BlockPos pos, Direction facing) {
            if (selected == tiles.get(pos)) {
                this.selectedPosFace = null;
                selected = null;
                sameSymbol.clear();
                sameAisle.clear();
                updateTips("");
                repeats[0].setActive(false); repeats[1].setActive(false);
            } else {
                this.selectedPosFace = new BlockPosFace(pos, facing);
                selected = tiles.get(pos);
                sameSymbol.clear();
                sameAisle.clear();
                for (SymbolTileEntity symbol : tiles.values()) {
                    if (symbol.symbol == selected.symbol) {
                        sameSymbol.add(symbol);
                    }
                    if (selected.a == symbol.a) {
                        sameAisle.add(symbol);
                    }
                }
                repeats[0].setActive(true); repeats[1].setActive(true);
            }
            needUpdatePredicateSelector = true;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (viewMode == 2) {
                clickPosFace = null;
                dragging = false;
                return false;
            }
            boolean result = false;
            if (button != 1) {
                result = super.mouseReleased(mouseX, mouseY, button);
            }
            if (!result && isMouseOverElement(mouseX, mouseY) && hoverPosFace == null && selectedPosFace != null && button == 0) {
                onSelected(selectedPosFace.pos, selectedPosFace.facing);
                return true;
            }
            return result;
        }
    }

    public class PredicateWidget extends WidgetGroup {
        public SimplePredicate predicate;
        public CycleItemStackHandler itemHandler;
        public String name;
        public boolean isSelected;
        public AtomicReference<PredicateWidget> atomicReference;

        public PredicateWidget(int x, int y, SimplePredicate predicate, String name, int xC) {
            super(x, y, 175, 20);
            setClientSideWidget();
            this.predicate = predicate;
            this.name = name;

            itemHandler = new CycleItemStackHandler(Collections.singletonList(predicate.getCandidates()));
            addWidget(new ImageWidget(0, 0, 179, 20, new ResourceTexture("multiblocked:textures/gui/predicate_selector_bar.png")));
            addWidget(new SlotWidget(itemHandler, 0, 1, 1, false, false));
            addWidget(new ImageWidget(20, 0, 120, 20, new TextTexture(name, 0xaf000000).setWidth(120).setType(TextTexture.TextType.ROLL))); // 106
            if (name.equals("controller") || name.equals("air") || name.equals("any")) return;
            addWidget(new ButtonWidget(xC, 3, 14, 14, new ResourceTexture("multiblocked:textures/gui/option.png"), cd -> {
                DialogWidget dialogWidget = new DialogWidget(JsonBlockPatternWidget.this, true).setOnClosed(() -> JsonBlockPatternWidget.this.sceneWidget.tiles.values().forEach(
                        SymbolTileEntity::updateRenderer));
                dialogWidget.addWidget(new ImageWidget(0, 0, 384, 256, new ColorRectTexture(0xaf333333)));
                int yOffset = 30;
                int xOffset = 30;
                for (WidgetGroup widget : predicate.getConfigWidget(new ArrayList<>())) {
                    widget.addSelfPosition(xOffset, yOffset);
                    dialogWidget.addWidget(widget);
                    yOffset += widget.getSize().height + 5;
                }
            }).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.tips.configuration"));
        }

        public PredicateWidget(int x, int y, SimplePredicate predicate, String name, Consumer<String> closeCallBack) {
            this(x, y, predicate, name, 144);
            if (name.equals("controller")) return;
            addWidget(new ButtonWidget(160, 3, 14, 14, new ResourceTexture("multiblocked:textures/gui/remove.png"), cd -> {if(closeCallBack != null) closeCallBack.accept(name);}).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.dialogs.pattern.remove_predicate"));
        }

        public PredicateWidget(int x, int y, SimplePredicate predicate, String name, AtomicReference<PredicateWidget> atomicReference) {
            this(x, y, predicate, name, 160);
            this.atomicReference = atomicReference;
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawInBackground(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            super.drawInBackground(matrixStack, mouseX, mouseY, partialTicks);
            if (isSelected) {
                DrawerHelper.drawBorder(matrixStack, getPosition().x + 1, getPosition().y + 1, getSize().width - 2, getSize().height - 2, 0xff00aa00, 1);
            }
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isMouseOverElement(mouseX, mouseY) && atomicReference != null) {
                if (atomicReference.get() == this) {
                    atomicReference.set(null);
                    this.isSelected = false;
                } else {
                    if (atomicReference.get() != null) {
                        atomicReference.get().isSelected = false;
                    }
                    atomicReference.set(this);
                    this.isSelected = true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void updateScreen() {
            super.updateScreen();
            itemHandler.updateStacks(Collections.singletonList(predicate.getCandidates()));
        }
    }

    public static class SymbolButton extends ButtonWidget{

        private final BlockPatternSceneWidget sceneWidget;
        private final char c;

        public SymbolButton(int xPosition, int yPosition, int width, int height, char c, BlockPatternSceneWidget sceneWidget, Consumer<ClickData> onPressed) {
            super(xPosition, yPosition, width, height, new TextTexture(c + "", getColor(c)), onPressed);
            this.c = c;
            this.sceneWidget = sceneWidget;
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawInBackground(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            Position position = getPosition();
            Size size = getSize();
            DrawerHelper.drawBorder(matrixStack, position.x, position.y, size.width, size.height, sceneWidget.selected != null && sceneWidget.selected.symbol == c ? 0xff00ff00 : -1, 1);
            super.drawInBackground(matrixStack, mouseX, mouseY, partialTicks);
        }
    }

    public static class SymbolTileEntity extends PartTileEntity<PartDefinition> {
        public char symbol;
        public IMultiblockedRenderer renderer;
        public JsonBlockPatternWidget widget;
        public int a,b,c;

        public SymbolTileEntity(PartDefinition definition) {
            super(definition);
        }

        public void init(char symbol, JsonBlockPatternWidget widget, int a, int b, int c) {
            this.symbol = symbol;
            this.widget = widget;
            this.a = a;
            this.b = b;
            this.c = c;
        }
        
        public void setDefinition(PartDefinition definition) {
            this.definition = definition;
        }

        public void updateRenderer() {
            if (widget.pattern.symbolMap.containsKey(symbol)) {
                Set<BlockInfo> candidates = new HashSet<>();
                for (String s : widget.pattern.symbolMap.get(symbol)) {
                    SimplePredicate predicate = widget.pattern.predicates.get(s);
                    if (predicate instanceof PredicateComponent && ((PredicateComponent) predicate).definition != null) {
                        renderer = ((PredicateComponent) predicate).definition.baseRenderer;
                        candidates = null;
                        break;
                    } else if (predicate != null && predicate.candidates != null) {
                        candidates.addAll(Arrays.asList(predicate.candidates.get()));
                    }
                }
                if (candidates != null) {
                    if (candidates.size() == 1) {
                        renderer = new MBDBlockStateRenderer(candidates.toArray(new BlockInfo[0])[0]);
                    } else if (!candidates.isEmpty()){
                        renderer = new CycleBlockStateRenderer(candidates.toArray(new BlockInfo[0]));
                    } else {
                        renderer = null;
                    }
                }
            }
        }

        @Override
        public boolean isFormed() {
            return false;
        }

        @Override
        public IMultiblockedRenderer getRenderer() {
            return widget == null ? null : widget.sceneWidget.viewMode == 1 ? null : renderer;
        }
        
    }
}
