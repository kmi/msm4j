/*
 * Created by IntelliJ IDEA.
 * User: cp3982
 * Date: 31/10/2013
 * Time: 16:52
 */
package uk.ac.open.kmi.msm4j.io;

import com.google.inject.AbstractModule;
import uk.ac.open.kmi.msm4j.io.impl.ServiceReaderImpl;
import uk.ac.open.kmi.msm4j.io.impl.ServiceWriterImpl;
import uk.ac.open.kmi.msm4j.io.util.PluginModuleLoader;

public class TransformerModule extends AbstractModule {

    @Override
    protected void configure() {
        install(PluginModuleLoader.of(TransformationPluginModule.class));
        bind(ServiceReader.class).to(ServiceReaderImpl.class);
        bind(ServiceWriter.class).to(ServiceWriterImpl.class);
    }
}
