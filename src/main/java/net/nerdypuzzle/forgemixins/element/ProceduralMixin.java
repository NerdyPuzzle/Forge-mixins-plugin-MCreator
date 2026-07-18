package net.nerdypuzzle.forgemixins.element;

import net.mcreator.element.GeneratableElement;
import net.mcreator.element.parts.procedure.Procedure;
import net.mcreator.workspace.elements.ModElement;

import java.util.ArrayList;
import java.util.List;

public class ProceduralMixin extends GeneratableElement {

    public List<String> mixins;
    
    public String className = "";
    public String methodName = "";
    public String rawMethodName = "";
    public String methodReturnType = "void";
    public List<String> methodParameters = new ArrayList<>();
    public List<String> methodParameterNames = new ArrayList<>();
    public String headTail = "HEAD";
    public Procedure procedure;

    public ProceduralMixin(ModElement element) {
        super(element);
        this.mixins = List.of(getModElement().getName() + "Mixin");
    }

}