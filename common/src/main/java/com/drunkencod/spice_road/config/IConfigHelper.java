package com.drunkencod.multi_loader_template.config;

/**
 * Cross-loader config service interface.
 * <p>
 * Add config entries here as interface methods, then implement them in
 * {@code NeoForgeConfigHelper} (using {@code ModConfigSpec}) and
 * {@code FabricConfigHelper} (using Cloth Config / AutoConfig).
 *
 * <p>
 * Config is loaded via
 * {@link com.drunkencod.multi_loader_template.platform.Services#CONFIG}.
 */
public interface IConfigHelper {

    /**
     * Example startup (common) config value — read once during mod initialisation.
     *
     * @return {@code true} if the example startup option is enabled
     */
    boolean getExampleStartupBool();

    /**
     * Example server-side config value.
     *
     * @return {@code true} if the example server option is enabled
     */
    boolean getExampleServerBool();

    /**
     * Example client-side config value.
     *
     * @return {@code true} if the example client option is enabled
     */
    boolean getExampleClientBool();
}
