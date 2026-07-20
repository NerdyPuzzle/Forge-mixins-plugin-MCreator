package ${package}.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(${data.className}.class)
public abstract class ${name}Mixin {

<#function boxType type>
    <#if type == "int">
        <#return "Integer">
    <#elseif type == "float">
        <#return "Float">
    <#elseif type == "double">
        <#return "Double">
    <#elseif type == "long">
        <#return "Long">
    <#elseif type == "short">
        <#return "Short">
    <#elseif type == "byte">
        <#return "Byte">
    <#elseif type == "boolean">
        <#return "Boolean">
    <#elseif type == "char">
        <#return "Character">
    <#else>
        <#return type>
    </#if>
</#function>

<#assign callbackClass = (data.methodReturnType == "void")?then("CallbackInfo", "CallbackInfoReturnable<" + boxType(data.methodReturnType) + ">")>
<#assign callbackVar = (data.methodReturnType == "void")?then("ci", "cir")>

    @Inject(method = "${data.rawMethodName}", at = @At("${data.headTail}"), cancellable = true)
    private <#if data.isStatic>static </#if>void executeProcedure${name}(<#list data.methodParameters as type>${type} ${data.methodParameterNames[type?index]}, </#list>${callbackClass} ${callbackVar}) {
        <#if data.procedure?? && data.procedure.getName()?? && data.procedure.getName()?has_content && data.procedure.getName() != "null" && w.hasModElement(data.procedure.getName())>
            <#assign procName = data.procedure.getName()>
            <#assign depsBuilder = []>
            <#assign needsImplicitEntity = "">
            <#assign needsImplicitItemstack = "">
            <#list data.procedure.getDependencies(generator.getWorkspace()) as dep>
                <#assign depsBuilder += [dep.getName()]>
                <#if dep.getName() == "entity" && !data.methodParameterNames?seq_contains("entity")>
                    <#assign needsImplicitEntity = "entity">
                <#elseif dep.getName() == "mixinEntity" && !data.methodParameterNames?seq_contains("mixinEntity")>
                    <#assign needsImplicitEntity = "mixinEntity">
                </#if>
                <#if dep.getName() == "itemstack" && !data.methodParameterNames?seq_contains("itemstack")>
                    <#assign needsImplicitItemstack = "itemstack">
                <#elseif dep.getName() == "mixinItemstack" && !data.methodParameterNames?seq_contains("mixinItemstack")>
                    <#assign needsImplicitItemstack = "mixinItemstack">
                </#if>
            </#list>
            
            <#if needsImplicitEntity?has_content>
                Entity ${needsImplicitEntity} = (Entity) (Object) this;
            </#if>
            <#if needsImplicitItemstack?has_content>
                ItemStack ${needsImplicitItemstack} = (ItemStack) (Object) this;
            </#if>
            
            <#assign hasWorld = false>
            <#list data.methodParameters as type>
                <#assign paramName = data.methodParameterNames[type?index]>
                <#if paramName != "callbackInfo" && paramName != "callbackInfoReturnable">
                    <#if type == "BlockPos">
                Vec3 ${paramName}_vector = Vec3.atCenterOf(${paramName});
                    </#if>
                    <#if (type == "Level" || type == "ServerLevel" || type == "ClientLevel" || type == "LevelAccessor" || type == "World") && !hasWorld>
                        <#assign hasWorld = true>
                LevelAccessor world = (LevelAccessor) ${paramName};
                    </#if>
                </#if>
            </#list>
            
            <#if !hasWorld && depsBuilder?seq_contains("world")>
                <#assign entityVarName = needsImplicitEntity>
                <#if entityVarName == "">
                    <#assign entityVarName = data.getFirstEntityDependencyOrNull()>
                </#if>
                <#if entityVarName?? && entityVarName?has_content>
                LevelAccessor world = ${entityVarName}.level();
                </#if>
            </#if>
            
            <#assign procReturnType = data.procedure.getReturnValueType(generator.getWorkspace())!"">
            
            <#if data.methodReturnType != "void" && depsBuilder?seq_contains("returnValue")>
                <#if data.methodReturnType == "int">
                double returnValue = (double) ${callbackVar}.getReturnValueI();
                <#elseif data.methodReturnType == "float">
                double returnValue = (double) ${callbackVar}.getReturnValueF();
                <#elseif data.methodReturnType == "boolean">
                boolean returnValue = ${callbackVar}.getReturnValueZ();
                <#elseif data.methodReturnType == "double">
                double returnValue = ${callbackVar}.getReturnValueD();
                <#elseif data.methodReturnType == "long">
                double returnValue = (double) ${callbackVar}.getReturnValueJ();
                <#elseif data.methodReturnType == "short">
                double returnValue = (double) ${callbackVar}.getReturnValueS();
                <#elseif data.methodReturnType == "byte">
                double returnValue = (double) ${callbackVar}.getReturnValueB();
                <#elseif data.methodReturnType == "char">
                String returnValue = String.valueOf(${callbackVar}.getReturnValueC());
                <#elseif data.methodReturnType == "BlockPos">
                Vec3 returnValue = ${callbackVar}.getReturnValue() != null ? Vec3.atCenterOf((BlockPos) ${callbackVar}.getReturnValue()) : Vec3.ZERO;
                <#else>
                ${data.methodReturnType} returnValue = (${data.methodReturnType}) ${callbackVar}.getReturnValue();
                </#if>
            </#if>
            
            
            <#if procReturnType == "logic" && data.methodReturnType != "boolean">
                if (!${procName}Procedure.execute(<#list depsBuilder as dep>${dep}<#sep>, </#list>)) {
                    ${callbackVar}.cancel();
                }
            <#elseif procReturnType == "number" || procReturnType == "logic" || procReturnType == "string" || procReturnType == "entity" || procReturnType == "itemstack" || procReturnType == "blockstate" || procReturnType == "damagesource" || procReturnType == "world" || procReturnType == "vector">
                <#if data.methodReturnType == "int" || data.methodReturnType == "float" || data.methodReturnType == "short" || data.methodReturnType == "byte" || data.methodReturnType == "long">
                    ${callbackVar}.setReturnValue((${data.methodReturnType}) ${procName}Procedure.execute(<#list depsBuilder as dep>${dep}<#sep>, </#list>));
                <#else>
                    ${callbackVar}.setReturnValue(${procName}Procedure.execute(<#list depsBuilder as dep>${dep}<#sep>, </#list>));
                </#if>
            <#else>
                ${procName}Procedure.execute(<#list depsBuilder as dep>${dep}<#sep>, </#list>);
            </#if>
        </#if>
    }
}