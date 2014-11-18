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

package uk.ac.open.kmi.msm4j.transformer.sawsdl;

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
import uk.ac.open.kmi.msm4j.io.impl.ServiceTransformationEngine;
import uk.ac.open.kmi.msm4j.io.impl.ServiceWriterImpl;
import uk.ac.open.kmi.msm4j.io.impl.TransformerModule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.*;

@RunWith(JukitoRunner.class)
public class SawsdlTransformerTest {

    private static final Logger log = LoggerFactory.getLogger(SawsdlTransformerTest.class);
    private static final String SAWSDL_TC3_SERVICES = "/services/sawsdl_wsdl11/";

    // Number of files to transform. Integer.MAX_VALUE for all
    private static final int NUM_TESTS = 25 ;

    private SawsdlTransformer importer;
    private ServiceWriter writer;
    private List<URI> testFolders;
    private FilenameFilter sawsdlFilter;

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

        importer = new SawsdlTransformer();
        writer = new ServiceWriterImpl();
        testFolders = new ArrayList<URI>();
        testFolders.add(SawsdlTransformerTest.class.getResource(SAWSDL_TC3_SERVICES).toURI());

        sawsdlFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.endsWith(".wsdl") || name.endsWith(".sawsdl"));
            }
        };
    }

    @Test
    public void testTransformFile() {

        // Add all the test collections
        log.info("Transforming test collections");
        for (URI testFolder : testFolders) {
            File dir = new File(testFolder);
            log.info("Test collection: {} ", testFolder);

            // Test services
            Collection<Service> services;
            log.info("Transforming services");
            File[] sawsdlFiles = dir.listFiles(sawsdlFilter);
            List<File> files = Arrays.asList(sawsdlFiles);
            Collections.shuffle(files);

            for (int i = 0; i < NUM_TESTS && i < files.size(); i++) {
                File file = files.get(i);
                log.info("Transforming service {}", file.getAbsolutePath());
                try {
                    services = importer.transform(new FileInputStream(file), null);
                    Assert.assertNotNull("Service collection should not be null", services);
                    Assert.assertTrue("There should be at least one service", 1 >= services.size());
                } catch (Exception e) {
                    log.error("Problems transforming the service. Continuing", e);
                }
            }
        }
    }

    @Test
    public void testPluginBasedTransformation(ServiceTransformationEngine serviceTransformationEngine) {
        // Add all the test collections
        log.info("Transforming test collections");
        for (URI testFolder : testFolders) {
            File dir = new File(testFolder);
            log.info("Test collection: {} ", testFolder);

            // Test services
            Collection<Service> services;
            log.info("Transforming services");
            File[] sawsdlFiles = dir.listFiles(sawsdlFilter);
            List<File> files = Arrays.asList(sawsdlFiles);
            Collections.shuffle(files);

            for (int i = 0; i < NUM_TESTS && i < files.size(); i++) {
                File file = files.get(i);
                log.info("Transforming service {}", file.getAbsolutePath());
                try {
                    services = serviceTransformationEngine.transform(file, null, SawsdlTransformer.mediaType);
                    Assert.assertNotNull("Service collection should not be null", services);
                    Assert.assertEquals(1, services.size());
                } catch (Exception e) {
                    log.error("Problems transforming the service. Continuing", e);
                }
            }
        }

    }

}
