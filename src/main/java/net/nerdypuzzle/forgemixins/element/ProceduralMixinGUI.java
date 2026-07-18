package net.nerdypuzzle.forgemixins.element;

import net.mcreator.blockly.data.Dependency;
import net.mcreator.element.parts.procedure.Procedure;
import net.mcreator.io.zip.ZipIO;
import net.mcreator.java.ProjectJarManager;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.component.SearchableComboBox;
import net.mcreator.ui.component.util.PanelUtils;
import net.mcreator.ui.modgui.ModElementGUI;
import net.mcreator.ui.procedure.ProcedureSelector;
import net.mcreator.ui.procedure.AbstractProcedureSelector;
import net.mcreator.ui.validation.AggregatedValidationResult;
import net.mcreator.workspace.elements.ModElement;
import net.mcreator.workspace.elements.VariableType;
import net.mcreator.workspace.elements.VariableTypeLoader;
import net.mcreator.ui.init.L10N;

import javax.annotation.Nullable;

import org.fife.rsta.ac.java.buildpath.SourceLocation;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ProceduralMixinGUI extends ModElementGUI<ProceduralMixin> {

    private SearchableComboBox<String> classNameSelector;
    private SearchableComboBox<String> methodNameSelector;
    private JComboBox<String> headTailSelector;
    private ProcedureSelector procedure;
    private JPanel procWrap;
    private List<MethodSource<JavaClassSource>> currentMethods = new ArrayList<>();
    private String currentLoadedClass = null;

    private String initialClassName = null;
    private String initialMethodName = null;
    private Procedure initialProcedure = null;

    public ProceduralMixinGUI(MCreator mcreator, ModElement modElement, boolean editingMode) {
        super(mcreator, modElement, editingMode);
        this.initGUI();
        super.finalizeGUI();
        this.loadClassesAsync();
    }

    @Override
    protected void initGUI() {
        classNameSelector = new SearchableComboBox<>();
        methodNameSelector = new SearchableComboBox<>();
        headTailSelector = new JComboBox<>(new String[]{"HEAD", "TAIL", "RETURN"});

        classNameSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof String str) {
                    int lastDot = str.lastIndexOf('.');
                    value = lastDot == -1 ? str : str.substring(lastDot + 1);
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });

        procedure = new ProcedureSelector(this.withEntry(L10N.t("elementgui.proceduralmixin.")), this.mcreator,
                L10N.t("elementgui.proceduralmixin.procedure"), AbstractProcedureSelector.Side.BOTH,
                true, VariableTypeLoader.BuiltInTypes.LOGIC).makeReturnValueOptional();
        procedure.refreshList(null);

        classNameSelector.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (e.getItem() instanceof String newClass) {
                    if (!newClass.equals(currentLoadedClass)) {
                        currentLoadedClass = newClass;
                        updateMethods(newClass);
                    }
                }
            }
        });

        methodNameSelector.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateProcedureDependencies();
            }
        });

        JPanel labelPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        labelPanel.setOpaque(false);
        labelPanel.add(L10N.label("elementgui.proceduralmixin.class"));
        labelPanel.add(L10N.label("elementgui.proceduralmixin.method"));
        labelPanel.add(L10N.label("elementgui.proceduralmixin.position"));

        JPanel controlPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        controlPanel.setOpaque(false);
        controlPanel.add(classNameSelector);
        controlPanel.add(methodNameSelector);
        controlPanel.add(headTailSelector);

        JPanel selectionPanel = new JPanel(new BorderLayout(15, 0));
        selectionPanel.setOpaque(false);
        selectionPanel.add(labelPanel, BorderLayout.WEST);
        selectionPanel.add(controlPanel, BorderLayout.CENTER);

        procWrap = new JPanel(new FlowLayout(FlowLayout.LEFT));
        procWrap.setOpaque(false);
        procWrap.add(procedure);

        JPanel topSection = new JPanel(new BorderLayout(0, 10));
        topSection.setOpaque(false);
        topSection.add(PanelUtils.northAndCenterElement(selectionPanel, PanelUtils.totalCenterInPanel(procWrap)));

        JPanel pane1 = new JPanel(new BorderLayout());
        pane1.setOpaque(false);
        pane1.add("Center", PanelUtils.totalCenterInPanel(topSection));

        addPage("Procedural Mixin", pane1).lazyValidate(() -> (classNameSelector.getSelectedItem() != null && methodNameSelector.getSelectedItem() != null && procedure.getSelectedProcedure() != null)
                ? new AggregatedValidationResult.PASS() : new AggregatedValidationResult.FAIL(L10N.t("elementgui.error.select_options")));
    }

    @Override
    public @Nullable URI contextURL() throws URISyntaxException {
        return null;
    }

    private void loadClassesAsync() {
        new Thread(() -> {
            List<String> classes = new ArrayList<>();
            try {
                var jarManager = mcreator.getGenerator().getProjectJarManager();
                if (jarManager != null && jarManager.getClasspath() != null) {
                    for (var entry : jarManager.getClasspath()) {
                        String srcPath = entry.getSrc(mcreator.getWorkspace());
                        if (srcPath != null && (srcPath.endsWith(".jar") || srcPath.endsWith(".zip"))) {
                            File srcFile = new File(srcPath);
                            if (srcFile.exists()) {
                                try (ZipFile zipFile = new ZipFile(srcFile)) {
                                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                                    while (entries.hasMoreElements()) {
                                        ZipEntry zipEntry = entries.nextElement();
                                        if (zipEntry.getName().endsWith(".java") && !zipEntry.getName().contains("package-info")) {
                                            String className = zipEntry.getName().replace("/", ".").replace(".java", "");
                                            classes.add(className);
                                        }
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            SwingUtilities.invokeLater(() -> {
                String current = (String) classNameSelector.getSelectedItem();
                if (current == null && initialClassName != null) {
                    current = initialClassName;
                }
                
                String currentMethod = (String) methodNameSelector.getSelectedItem();
                if (currentMethod == null && initialMethodName != null) {
                    currentMethod = initialMethodName;
                }

                classNameSelector.setItems(classes);
                
                if (current != null) {
                    classNameSelector.setSelectedItem(current);
                }
                
                if (currentMethod != null) {
                    methodNameSelector.setSelectedItem(currentMethod);
                }
            });
        }).start();
    }

    private void updateMethods(String className) {
        methodNameSelector.removeAllItems();
        currentMethods.clear();
        if (className == null || className.isEmpty()) return;

        try {
            ProjectJarManager jarManager = mcreator.getGenerator().getProjectJarManager();
            if (jarManager != null) {
                SourceLocation sourceLocation = jarManager.getSourceLocForClass(className);
                if (sourceLocation != null) {
                    String code = ZipIO.readCodeInZip(
                            new File(sourceLocation.getLocationAsString()),
                            className.replace(".", "/") + ".java");
                    if (code != null) {
                        JavaClassSource classSource = (JavaClassSource) Roaster.parse(code);
                        currentMethods = new ArrayList<>(classSource.getMethods());
                        for (MethodSource<JavaClassSource> method : currentMethods) {
                            String params = method.getParameters().stream()
                                    .map(p -> p.getType().getName())
                                    .reduce((a, b) -> a + ", " + b).orElse("");
                            String methodDesc = method.getName() + "(" + params + ")";
                            methodNameSelector.addItem(methodDesc);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final Set<String> VANILLA_ENTITIES = new HashSet<>(Arrays.asList(
        "Parrot", "Ocelot", "Wolf", "Cat", "Villager", "Zombie", "Skeleton", "Creeper", "Spider", "Slime", 
        "Ghast", "Enderman", "Pig", "Sheep", "Cow", "Chicken", "Squid", "Bat", "Mooshroom", "IronGolem", 
        "SnowGolem", "Horse", "Donkey", "Mule", "SkeletonHorse", "ZombieHorse", "Rabbit", "PolarBear", 
        "Llama", "Fox", "Panda", "Turtle", "Dolphin", "Bee", "Strider", "Hoglin", "Zoglin", "Piglin", 
        "PiglinBrute", "Warden", "Frog", "Tadpole", "Allay", "Camel", "Sniffer", "Axolotl", "GlowSquid", 
        "Goat", "Vindicator", "Evoker", "Illusioner", "Pillager", "Ravager", "Witch", "Vex", "Guardian", 
        "ElderGuardian", "Shulker", "Silverfish", "Endermite", "Phantom", "Drowned", "Husk", "Stray", 
        "WitherSkeleton", "Blaze", "MagmaCube", "WitherBoss", "EnderDragon", "ZombieVillager", "TraderLlama", 
        "WanderingTrader", "CaveSpider", "Breeze", "Bogged", "Armadillo", "Creaking"
    ));

    private String mapToMcType(String javaType) {
        if (javaType == null) return null;
        if (javaType.equals("int") || javaType.equals("float") || javaType.equals("double")
                || javaType.equals("long") || javaType.equals("short") || javaType.equals("byte")) {
            return "number";
        }
        if (javaType.equals("boolean")) {
            return "logic";
        }
        if (javaType.equals("String")) {
            return "string";
        }
        if (javaType.equals("Entity") || javaType.equals("LivingEntity")
                || javaType.equals("Player") || javaType.equals("ServerPlayer")
                || javaType.equals("Mob") || javaType.equals("PathfinderMob")
                || javaType.equals("Animal") || javaType.equals("AbstractHurtingProjectile")
                || javaType.endsWith("Entity") || javaType.endsWith("Player")
                || javaType.endsWith("Mob") || VANILLA_ENTITIES.contains(javaType)) {
            return "entity";
        }
        if (javaType.equals("Level") || javaType.equals("ServerLevel")
                || javaType.equals("ClientLevel") || javaType.equals("World")
                || javaType.endsWith("Level") || javaType.endsWith("World")) {
            return "world";
        }
        if (javaType.equals("Vec3")) {
            return "vector";
        }
        if (javaType.equals("ItemStack")) {
            return "itemstack";
        }
        if (javaType.equals("BlockState")) {
            return "blockstate";
        }
        if (javaType.equals("DamageSource")) {
            return "damagesource";
        }
        return null;
    }

    private boolean isEntityClass(String fqcn) {
        if (fqcn == null) return false;
        
        if (fqcn.startsWith("net.minecraft.world.entity.") || fqcn.startsWith("net.minecraft.client.player.")) {
            if (!fqcn.contains("EntityType") && !fqcn.contains("EntityDimensions") 
                && !fqcn.contains("EntitySelector") && !fqcn.contains("EntityData") 
                && !fqcn.contains("EntityEvent")) {
                return true;
            }
        }
        
        int lastDot = fqcn.lastIndexOf('.');
        String simpleName = lastDot == -1 ? fqcn : fqcn.substring(lastDot + 1);
        
        return simpleName.equals("Entity") || simpleName.equals("LivingEntity")
                || simpleName.equals("Player") || simpleName.equals("ServerPlayer")
                || simpleName.equals("Mob") || simpleName.equals("PathfinderMob")
                || simpleName.equals("Animal") || simpleName.equals("AbstractHurtingProjectile")
                || simpleName.endsWith("Entity") || simpleName.endsWith("Player")
                || simpleName.endsWith("Mob") || VANILLA_ENTITIES.contains(simpleName);
    }

    private boolean isItemStackClass(String fqcn) {
        if (fqcn == null) return false;
        int lastDot = fqcn.lastIndexOf('.');
        String simpleName = lastDot == -1 ? fqcn : fqcn.substring(lastDot + 1);
        return simpleName.equals("ItemStack");
    }

    private void updateProcedureDependencies() {
        int index = methodNameSelector.getSelectedIndex();
        if (index >= 0 && index < currentMethods.size()) {
            MethodSource<JavaClassSource> method = currentMethods.get(index);
            List<Dependency> dependencies = new ArrayList<>();
            boolean hasEntityDependency = false;
            boolean hasEntityName = false;
            boolean hasWorldDependency = false;

            boolean hasItemStackDependency = false;
            boolean hasItemstackName = false;

            for (var param : method.getParameters()) {
                String type = param.getType().getName();
                String name = param.getName();
                
                if (name.equals("entity")) {
                    hasEntityName = true;
                }
                if (name.equals("itemstack")) {
                    hasItemstackName = true;
                }

                if (type.equals("BlockPos")) {
                    dependencies.add(new Dependency(name + "_vector", "vector"));
                    continue;
                }

                String mcType = mapToMcType(type);
                if (mcType == null) continue;
                
                if (mcType.equals("world")) {
                    if (!hasWorldDependency) {
                        dependencies.add(new Dependency("world", "world"));
                        hasWorldDependency = true;
                    }
                    continue;
                }
                
                if (mcType.equals("entity")) {
                    hasEntityDependency = true;
                }
                if (mcType.equals("itemstack")) {
                    hasItemStackDependency = true;
                }

                dependencies.add(new Dependency(name, mcType));
            }
            
            String currentClass = classNameSelector.getSelectedItem() instanceof String str ? str : "";
            if (isEntityClass(currentClass) && !hasEntityDependency) {
                String depName = hasEntityName ? "mixinEntity" : "entity";
                dependencies.add(new Dependency(depName, "entity"));
            }
            if (isItemStackClass(currentClass) && !hasItemStackDependency) {
                String depName = hasItemstackName ? "mixinItemstack" : "itemstack";
                dependencies.add(new Dependency(depName, "itemstack"));
            }

            String methodReturnType = method.getReturnType() != null ? method.getReturnType().getName() : "void";
            String mcTypeString = mapToMcType(methodReturnType);
            
            VariableType expectedReturnType = null;
            if (mcTypeString == null || mcTypeString.equals("world")) {
                expectedReturnType = VariableTypeLoader.BuiltInTypes.LOGIC;
            } else {
                switch (mcTypeString) {
                    case "number": expectedReturnType = VariableTypeLoader.BuiltInTypes.NUMBER; break;
                    case "logic": expectedReturnType = VariableTypeLoader.BuiltInTypes.LOGIC; break;
                    case "string": expectedReturnType = VariableTypeLoader.BuiltInTypes.STRING; break;
                    case "entity": expectedReturnType = VariableTypeLoader.BuiltInTypes.ENTITY; break;
                    case "itemstack": expectedReturnType = VariableTypeLoader.BuiltInTypes.ITEMSTACK; break;
                    case "blockstate": expectedReturnType = VariableTypeLoader.BuiltInTypes.BLOCKSTATE; break;
                    case "damagesource": expectedReturnType = VariableTypeLoader.BuiltInTypes.DAMAGESOURCE; break;
                    case "vector": expectedReturnType = VariableTypeLoader.BuiltInTypes.VECTOR; break;
                    default: expectedReturnType = VariableTypeLoader.BuiltInTypes.LOGIC; break;
                }
            }

            final VariableType finalReturnType = expectedReturnType;
            Dependency[] deps = dependencies.toArray(new Dependency[0]);
            SwingUtilities.invokeLater(() -> {
                Procedure currentProc = procedure != null ? procedure.getSelectedProcedure() : null;
                
                if (currentProc == null && initialProcedure != null) {
                    currentProc = initialProcedure;
                }

                procWrap.removeAll();
                
                if (finalReturnType != null) {
                    procedure = new ProcedureSelector(this, this.mcreator,
                            "Mixin procedure", AbstractProcedureSelector.Side.BOTH, true,
                            finalReturnType, deps).makeReturnValueOptional();
                } else {
                    // No return type expected (could be unsupported non-void)
                    procedure = new ProcedureSelector(this, this.mcreator,
                            "Mixin procedure", AbstractProcedureSelector.Side.BOTH, true,
                            deps).makeReturnValueOptional();
                }
                
                procedure.refreshList(null);
                
                if (currentProc != null) {
                    procedure.setSelectedProcedure(currentProc);
                }
                
                procWrap.add(procedure);
                procWrap.revalidate();
                procWrap.repaint();
            });
        }
    }

    @Override
    public void openInEditingMode(ProceduralMixin generatableElement) {
        initialClassName = generatableElement.className;
        initialMethodName = generatableElement.methodName;
        initialProcedure = generatableElement.procedure;

        if (generatableElement.className != null && !generatableElement.className.isEmpty()) {
            classNameSelector.setSelectedItem(generatableElement.className);
        }
        if (generatableElement.methodName != null && !generatableElement.methodName.isEmpty()) {
            methodNameSelector.setSelectedItem(generatableElement.methodName);
        }
        headTailSelector.setSelectedItem(generatableElement.headTail);
        if (generatableElement.procedure != null) {
            procedure.setSelectedProcedure(generatableElement.procedure);
        }
    }

    @Override
    public ProceduralMixin getElementFromGUI() {
        ProceduralMixin mixin = new ProceduralMixin(modElement);
        mixin.className = classNameSelector.getSelectedItem() instanceof String str ? str : "";
        mixin.methodName = methodNameSelector.getSelectedItem() instanceof String str ? str : "";

        int index = methodNameSelector.getSelectedIndex();
        if (index >= 0 && index < currentMethods.size()) {
            MethodSource<JavaClassSource> method = currentMethods.get(index);
            mixin.rawMethodName = method.isConstructor() ? "<init>" : method.getName();
            mixin.methodReturnType = method.getReturnType() != null ? method.getReturnType().getName() : "void";
            mixin.methodParameters = method.getParameters().stream()
                    .map(p -> p.getType().getName())
                    .collect(Collectors.toList());
            mixin.methodParameterNames = method.getParameters().stream()
                    .map(p -> p.getName())
                    .collect(Collectors.toList());
        }

        mixin.headTail = headTailSelector.getSelectedItem() instanceof String str ? str : "HEAD";
        mixin.procedure = procedure.getSelectedProcedure();
        return mixin;
    }
}