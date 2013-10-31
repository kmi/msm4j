/*
 * Copyright (c) 2013. Knowledge Media Institute - The Open University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package es.usc.citius.composit.transformer.wsc.wscxml;


import junit.framework.Assert;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.open.kmi.msm4j.Service;
import uk.ac.open.kmi.msm4j.io.Transformer;
import uk.ac.open.kmi.msm4j.io.TransformerModule;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

@RunWith(JukitoRunner.class)
public class WSCTransformerTest {
    private static final Logger log = LoggerFactory.getLogger(WSCTransformerTest.class);

    private static final String WSC08_01 = "/WSC08/wsc08_datasets/01/";
    private static final String WSC08_01_SERVICES = WSC08_01 + "services.xml";
    private static final String WSC08_01_TAXONOMY = WSC08_01 + "taxonomy.owl";
    private static final String WSC08_01_TAXONOMY_XML = WSC08_01 + "taxonomy.xml";

    /**
     * JukitoModule.
     */
    public static class InnerModule extends JukitoModule {
        @Override
        protected void configureTest() {

            // Ensure configuration is loaded
            install(new TransformerModule());
        }
    }

    @Test
    public void testPluginTransform(Transformer genericTransformer) throws Exception {
        // Add all the test collections
        log.info("Transforming test collections");
        // Get base url
        URL base = this.getClass().getResource(WSC08_01);
        log.info("Reading {}", base.toURI().toASCIIString());
        // Services
        URL services = new URL(base.toURI().toASCIIString() + "services.xml");
        InputStream stream = services.openStream();
        Assert.assertNotNull("Cannot open services.xml", stream);
        List<Service> result = genericTransformer.transform(stream, base.toURI().toASCIIString(), WSCTransformer.mediaType);
        Assert.assertEquals(158, result.size());

    }

    @Test
    public void testTransform() throws Exception {
        // Add all the test collections
        log.info("Transforming test collections");
        // NOTE: Ontology URL is not required to be reachable
        InputStream taxonomyStream = this.getClass().getResourceAsStream(WSC08_01_TAXONOMY_XML);
        InputStream servicesStream = this.getClass().getResourceAsStream(WSC08_01_SERVICES);
        Assert.assertNotNull("Cannot open taxonomy.xml", taxonomyStream);
        Assert.assertNotNull("Cannot open services.xml", servicesStream);

        // Get base url
        URL base = this.getClass().getResource(WSC08_01);


        WSCTransformer importer = new WSCTransformer();
        List<Service> result = importer.transform(servicesStream, base.toURI().toASCIIString());
        Assert.assertEquals(158, result.size());
    }

}

