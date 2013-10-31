package uk.ac.open.kmi.msm4j.io.util;

/**
 * ModuleLoader generic implementation for a Module Loader that uses Java's Service Loader behind the scenes.
 * Supports the automated discovery and loading of modules.
 * Solution based on http://stackoverflow.com/questions/902639/has-anyone-used-serviceloader-together-with-guice
 *
 * @author Mark Renouf
 * @since 31/10/2013
 */

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import java.util.ServiceLoader;

public class PluginModuleLoader<M extends Module> extends AbstractModule {

    private final Class<M> type;

    public PluginModuleLoader(Class<M> type) {
        this.type = type;
    }

    public static <M extends Module> PluginModuleLoader<M> of(Class<M> type) {
        return new PluginModuleLoader<M>(type);
    }

    @Override
    protected void configure() {
        ServiceLoader<M> modules = ServiceLoader.load(type);
        for (Module module : modules) {
            install(module);
        }
    }
}