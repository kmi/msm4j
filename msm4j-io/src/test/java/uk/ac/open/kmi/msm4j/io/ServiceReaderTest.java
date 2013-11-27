package uk.ac.open.kmi.msm4j.io;

import junit.framework.Assert;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.open.kmi.msm4j.Operation;
import uk.ac.open.kmi.msm4j.Service;
import uk.ac.open.kmi.msm4j.io.impl.TransformerModule;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * ServiceReaderTest
 *
 * @author <a href="mailto:carlos.pedrinaci@open.ac.uk">Carlos Pedrinaci</a> (KMi - The Open University)
 * @since 27/11/2013
 */
@RunWith(JukitoRunner.class)
public class ServiceReaderTest {

    private static final Logger log = LoggerFactory.getLogger(ServiceReaderTest.class);
    private static final String OWLS_TC4_MSM = "/OWLS-TC4_PDDL-MSM/";

    private static final String[] fileNames = new String[]{"1personbicycle4wheeledcar_price_service.ttl"};

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
    public void testParse(ServiceReader reader) throws Exception {

        File file;
        InputStream in;
        List<Service> services;
        Service svc;
        List<Operation> operations;
        String comment;
        Date created;
        URI creator;

        log.info("Parsing {} files", fileNames.length);
        for (String fileName : fileNames) {
            log.info("Parsing {}", fileName);
            in = ServiceReaderTest.class.getResourceAsStream(OWLS_TC4_MSM + fileName);
            services = reader.parse(in, null, Syntax.TTL);
            Assert.assertNotNull(services);
            Assert.assertEquals(1, services.size());
            svc = services.get(0);
            operations = svc.getOperations();
            Assert.assertNotNull(operations);
            Assert.assertEquals(1, operations.size());

            comment = svc.getComment();
            Assert.assertNotNull(comment);
            log.info("Comment: {}", comment);

            created = svc.getCreated();
            Assert.assertNotNull(created);
            Calendar cal = new GregorianCalendar(2013, 11, 27);
            Date refDate = cal.getTime();
            Assert.assertTrue(!created.before(refDate) || !created.after(refDate));
            log.info("Created: {}", created);
        }


    }
}
