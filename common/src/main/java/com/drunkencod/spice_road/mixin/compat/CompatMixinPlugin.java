package com.drunkencod.spice_road.mixin.compat;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.drunkencod.spice_road.Constants;

/**
 * Only applies a compat mixin when the mod it targets is loaded. Compat
 * mixins live in {@code mixin.compat.<modid>} packages, the mod ID being taken
 * from the package name. Each loader subclasses this with its own
 * mod-presence check, since mod lists aren't accessible through the usual
 * services this early.
 */
public abstract class CompatMixinPlugin implements IMixinConfigPlugin {

    private static final String COMPAT_PACKAGE = CompatMixinPlugin.class.getPackageName() + ".";

    /**
     * @param modId A mod ID.
     * @return Whether that mod is loaded. Called while mixins are applied,
     *         before mods are constructed.
     */
    protected abstract boolean isModLoaded(String modId);

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith(COMPAT_PACKAGE))
            return true;
        String rest = mixinClassName.substring(COMPAT_PACKAGE.length());
        String modId = rest.substring(0, rest.indexOf('.'));
        boolean loaded = isModLoaded(modId);
        if (loaded)
            Constants.LOG.debug("Applying compat mixin {}", mixinClassName);
        return loaded;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
