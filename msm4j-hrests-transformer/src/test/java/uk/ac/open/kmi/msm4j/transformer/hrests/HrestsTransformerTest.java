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

package uk.ac.open.kmi.msm4j.transformer.hrests;

import junit.framework.Assert;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.open.kmi.msm4j.Service;
import uk.ac.open.kmi.msm4j.io.ServiceWriter;
import uk.ac.open.kmi.msm4j.io.Transformer;
import uk.ac.open.kmi.msm4j.io.TransformerModule;
import uk.ac.open.kmi.msm4j.io.impl.ServiceWriterImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * HrestsImporterTest
 * <p/>
 * Author: Carlos Pedrinaci (KMi - The Open University)
 * Date: 14/06/2013
 * Time: 19:29
 */
@RunWith(JukitoRunner.class)
public class HrestsTransformerTest {

    private static final Logger log = LoggerFactory.getLogger(HrestsTransformerTest.class);
    private static final String JGD_SERVICES = "/jgd-services";

    private HrestsTransformer transformer;
    private ServiceWriter writer;
    private List<URI> testFolders;
    private FilenameFilter htmlFilter;

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

    @Before
    public void setUp() throws Exception {

        transformer = new HrestsTransformer();
        writer = new ServiceWriterImpl();
        testFolders = new ArrayList<URI>();
        testFolders.add(HrestsTransformerTest.class.getResource(JGD_SERVICES).toURI());

        htmlFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.endsWith(".htm") || name.endsWith(".html"));
            }
        };

    }

    @Test
    public void testTransform() throws Exception {

        // Add all the test collections
        log.info("Transforming test collections");
        for (URI testFolder : testFolders) {
            File dir = new File(testFolder);
            log.info("Test collection: " + testFolder);

            // Test services
            Collection<Service> services;
            log.info("Transforming services");
            File[] servicesFile = dir.listFiles(htmlFilter);
            for (File file : servicesFile) {
                log.info("Transforming file: " + file.getAbsolutePath());
                services = transformer.transform(new FileInputStream(file), file.toURI().toASCIIString());
                Assert.assertNotNull("Service collection should not be null", services);
                Assert.assertEquals(1, services.size());
            }
        }
    }

    @Test
    public void testPluginBasedTransformation(Transformer genericTransformer) {
        // Add all the test collections
        log.info("Transforming test collections");
        for (URI testFolder : testFolders) {
            File dir = new File(testFolder);
            log.info("Test collection: {} ", testFolder);

            // Test services
            Collection<Service> services;
            log.info("Transforming services");
            File[] owlsFiles = dir.listFiles(htmlFilter);
            for (File file : owlsFiles) {
                log.info("Transforming service {}", file.getAbsolutePath());
                try {
                    services = genericTransformer.transform(file, file.toURI().toASCIIString(), HrestsTransformer.mediaType);
                    Assert.assertNotNull("Service collection should not be null", services);
                    Assert.assertEquals(1, services.size());
                } catch (Exception e) {
                    log.error("Problems transforming the service. Continuing", e);
                }
            }
        }

    }
}
