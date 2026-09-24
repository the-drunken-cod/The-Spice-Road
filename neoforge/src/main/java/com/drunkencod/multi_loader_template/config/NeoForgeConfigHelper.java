package com.drunkencod.multi_loader_template.config;

import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import com.drunkencod.multi_loader_template.config.IConfigHelper;

public class NeoForgeConfigHelper implements IConfigHelper {

    // -------------------------------------------------------------------------
    // Common (startup) config
    // -------------------------------------------------------------------------

    public static final CommonConfig COMMON;
    private static final ModConfigSpec COMMON_SPEC;

    static {
        Pair<CommonConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder()
                .configure(CommonConfig::new);
        COMMON = specPair.getLeft();
        COMMON_SPEC = specPair.getRight();
    }

    // -------------------------------------------------------------------------
    // Server config
    // -------------------------------------------------------------------------

    public static final ServerConfig SERVER;
    private static final ModConfigSpec SERVER_SPEC;

    static {
        Pair<ServerConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder()
                .configure(ServerConfig::new);
        SERVER = specPair.getLeft();
        SERVER_SPEC = specPair.getRight();
    }

    // -------------------------------------------------------------------------
    // Client config
    // -------------------------------------------------------------------------

    public static final ClientConfig CLIENT;
    private static final ModConfigSpec CLIENT_SPEC;

    static {
        Pair<ClientConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder()
                .configure(ClientConfig::new);
        CLIENT = specPair.getLeft();
        CLIENT_SPEC = specPair.getRight();
    }

    // -------------------------------------------------------------------------
    // Registration — called from MultiLoaderTemplateMod constructor
    // -------------------------------------------------------------------------

    /**
     * Must be called in the NeoForge mod constructor with the injected
     * {@link ModContainer}
     * so that configs are registered before the world loads.
     */
    public void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }

    // -------------------------------------------------------------------------
    // IConfigHelper implementation
    // -------------------------------------------------------------------------

    @Override
    public boolean getExampleStartupBool() {
        return COMMON.exampleStartupBool.get();
    }

    @Override
    public boolean getExampleServerBool() {
        return SERVER.exampleServerBool.get();
    }

    @Override
    public boolean getExampleClientBool() {
        return CLIENT.exampleClientBool.get();
    }

    // -------------------------------------------------------------------------
    // Inner config classes
    // -------------------------------------------------------------------------

    public static class CommonConfig {
        public final ModConfigSpec.BooleanValue exampleStartupBool;

        CommonConfig(ModConfigSpec.Builder builder) {
            exampleStartupBool = builder
                    .comment("Example common (startup) config boolean")
                    .define("exampleStartupBool", false);
        }
    }

    public static class ServerConfig {
        public final ModConfigSpec.BooleanValue exampleServerBool;

        ServerConfig(ModConfigSpec.Builder builder) {
            exampleServerBool = builder
                    .comment("Example server config boolean")
                    .define("exampleServerBool", false);
        }
    }

    public static class ClientConfig {
        public final ModConfigSpec.BooleanValue exampleClientBool;

        ClientConfig(ModConfigSpec.Builder builder) {
            exampleClientBool = builder
                    .comment("Example client config boolean")
                    .define("exampleClientBool", false);
        }
    }
}
