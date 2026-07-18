# Procedural Mixin Procedure

The procedure executed when this Mixin triggers during gameplay. This allows you to insert custom logic directly into the native Minecraft methods you hook into.

### Procedure Execution & Dependencies
When your procedure runs, it will be automatically provided with:
*   **Original Method Parameters:** Any variables passed into the original Java method you hooked into will be mapped into procedure dependencies, provided their types are supported by MCreator (such as `number`, `logic`, `string`, `itemstack`, `entity`, `world`, etc.).
*   **Implicit Contexts:** If you hook into an `Entity` class (like `Player` or `Parrot`) or the `ItemStack` class, but the target method doesn't naturally pass that object as a parameter, the generator will automatically provide it to your procedure as a generic `entity` or `itemstack` dependency.
*   **Vector Conversions:** `BlockPos` parameters are automatically converted and provided as `vector` dependencies.

### Return Values
Depending on the **target method**, your procedure may need to return a value:
*   **Void Methods:** If the target method has no return value, your procedure doesn't need to return anything. It will simply execute alongside the original code. Optionally, your procedure can return false to cancel the execution of the rest of the vanilla code, similarly to cancelling a global trigger in a procedure.
*   **Methods with Return Values:** If the target method returns a value (e.g., a `boolean` or `int`), you **can** optionally set your procedure to return a matching type (e.g., `logic` or `number`).
    *   If you return a value, the Mixin will cancel the rest of the original method and return your value instead.
    *   If you do NOT return a value (void procedure block), the original method will continue executing normally after your procedure fires.
*   **Unsupported Return Types:** If the target method returns a complex Java object not supported by MCreator, the procedure selector will expect a `logic` return type. Returning `false` in your procedure will cancel the original method execution, effectively skipping it without returning a specific overridden value.