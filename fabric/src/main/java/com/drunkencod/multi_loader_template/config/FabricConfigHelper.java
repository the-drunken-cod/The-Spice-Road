package com.drunkencod.multi_loader_template.config;

import com.drunkencod.multi_loader_template.Constants;
import com.drunkencod.multi_loader_template.config.IConfigHelper;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public class FabricConfigHelper implements IConfigHelper {

    // -------------------------------------------------------------------------
    // Registration — called during mod initialization
    // -------------------------------------------------------------------------

    public void register() {
        AutoConfig.register(CommonConfigData.class, GsonConfigSerializer::new);
        AutoConfig.register(ServerConfigData.class, GsonConfigSerializer::new);
        AutoConfig.register(ClientConfigData.class, GsonConfigSerializer::new);
    }

    // -------------------------------------------------------------------------
    // IConfigHelper implementation
    // -------------------------------------------------------------------------

    @Override
    public boolean getExampleStartupBool() {
        return AutoConfig.getConfigHolder(CommonConfigData.class).getConfig().exampleStartupBool;
    }

    @Override
    public boolean getExampleServerBool() {
        return AutoConfig.getConfigHolder(ServerConfigData.class).getConfig().exampleServerBool;
    }

    @Override
    public boolean getExampleClientBool() {
        return AutoConfig.getConfigHolder(ClientConfigData.class).getConfig().exampleClientBool;
    }

    // -------------------------------------------------------------------------
    // Config data classes
    // -------------------------------------------------------------------------

    @Config(name = Constants.MOD_ID + "_common")
    public static class CommonConfigData implements ConfigData {
        @ConfigEntry.Gui.Tooltip
        public boolean exampleStartupBool = false;
    }

    @Config(name = Constants.MOD_ID + "_server")
    public static class ServerConfigData implements ConfigData {
        @ConfigEntry.Gui.Tooltip
        public boolean exampleServerBool = false;
    }

    @Config(name = Constants.MOD_ID + "_client")
    public static class ClientConfigData implements ConfigData {
        @ConfigEntry.Gui.Tooltip
        public boolean exampleClientBool = false;
    }
}
