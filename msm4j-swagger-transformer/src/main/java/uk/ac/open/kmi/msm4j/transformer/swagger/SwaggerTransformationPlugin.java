

package uk.ac.open.kmi.msm4j.transformer.swagger;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.open.kmi.msm4j.io.ServiceTransformer;
import uk.ac.open.kmi.msm4j.io.TransformationPluginModule;

import java.io.IOException;
import java.util.Properties;

/**
 * SawsdlTransformationPlugin is a Guice module providing support for transforming SAWSDL services into MSM
 *
 * @author <a href="mailto:carlos.pedrinaci@open.ac.uk">Carlos Pedrinaci</a> (KMi - The Open University)
 * @since 19/07/2013
 */
public class SwaggerTransformationPlugin extends AbstractModule implements TransformationPluginModule {

    private static final Logger log = LoggerFactory.getLogger(SwaggerTransformationPlugin.class);

    @Override
    protected void configure() {
        MapBinder<String, ServiceTransformer> binder = MapBinder.newMapBinder(binder(), String.class, ServiceTransformer.class);
        binder.addBinding(SwaggerTransformer.mediaType).to(SwaggerTransformer.class);
        // Bind the configuration as well
        Names.bindProperties(binder(), getProperties());
    }

    private Properties getProperties() {
        try {
            Properties properties = new Properties();
            properties.load(this.getClass().getClassLoader().getResourceAsStream("swagger-transformer.properties"));
            return properties;
        } catch (IOException ex) {
            log.error("Error obtaining plugin properties", ex);
        }
        return new Properties();
    }
}
