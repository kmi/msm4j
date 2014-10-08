/*
 * Copyright (c) 2014. Knowledge Media Institute - The Open University
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

package uk.ac.open.kmi.msm4j.transformer.swagger.test;

import junit.framework.Assert;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.open.kmi.msm4j.Service;
import uk.ac.open.kmi.msm4j.io.TransformationException;
import uk.ac.open.kmi.msm4j.io.impl.ServiceTransformationEngine;
import uk.ac.open.kmi.msm4j.io.impl.TransformerModule;
import uk.ac.open.kmi.msm4j.transformer.swagger.SwaggerTransformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.Collection;

@RunWith(JukitoRunner.class)
public class SwaggerTransformerTest {

    private static final Logger log = LoggerFactory.getLogger(SwaggerTransformerTest.class);

    private SwaggerTransformer importer;
    private FilenameFilter swaggerFilter;

    @Before
    public void setUp() throws Exception {

        importer = new SwaggerTransformer();
        swaggerFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.endsWith(".json"));
            }
        };
    }

    @Test
    public void testTransformRemoteDescription() {
        Collection<Service> services = null;

        try {
            services = importer.transform(null, "https://api.kixeye.com/api/v2/api-spec");
        } catch (TransformationException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull("Service collection should not be null", services);
        Assert.assertTrue("There should be at least one service", 1 >= services.size());

        try {
            services = importer.transform(null, "http://petstore.swagger.wordnik.com/api/api-docs");
        } catch (TransformationException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull("Service collection should not be null", services);
        Assert.assertTrue("There should be at least one service", 1 >= services.size());
    }

    @Test
    public void testTransformFile() {
        // Add all the test collections
        log.info("Transforming test collections");

        File dir = new File(this.getClass().getResource("/").getFile());

        // Test services
        Collection<Service> services;
        log.info("Transforming services");
        File[] swaggerFiles = dir.listFiles(swaggerFilter);
        for (File file : swaggerFiles) {
            log.info("Transforming service {}", file.getAbsolutePath());
            try {
                services = importer.transform(new FileInputStream(file), "http://localhost:8000/" + file.getName());
                Assert.assertNotNull("Service collection should not be null", services);
                Assert.assertTrue("There should be at least one service", 1 >= services.size());
            } catch (Exception e) {
                log.error("Problems transforming the service. Continuing", e);
            }
        }

    }

    @Test
    public void testPluginBasedTransformation(ServiceTransformationEngine serviceTransformationEngine) {
        // Add all the test collections
        log.info("Transforming test collections");
        File dir = new File(this.getClass().getResource("/").getFile());

        // Test services
        Collection<Service> services;
        log.info("Transforming services");
        File[] swaggerFiles = dir.listFiles(swaggerFilter);
        for (File file : swaggerFiles) {
            log.info("Transforming service {}", file.getAbsolutePath());
            try {
                services = serviceTransformationEngine.transform(file, null, SwaggerTransformer.mediaType);
                Assert.assertNotNull("Service collection should not be null", services);
                Assert.assertEquals(1, services.size());
            } catch (Exception e) {
                log.error("Problems transforming the service. Continuing", e);
            }
        }


    }

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

}
