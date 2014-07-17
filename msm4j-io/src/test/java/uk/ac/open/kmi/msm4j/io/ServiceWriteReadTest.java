package uk.ac.open.kmi.msm4j.io;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.open.kmi.msm4j.Service;
import uk.ac.open.kmi.msm4j.io.impl.TransformerModule;
import uk.ac.open.kmi.msm4j.nfp.Forum;
import uk.ac.open.kmi.msm4j.nfp.Provider;
import uk.ac.open.kmi.msm4j.nfp.TwitterAccount;
import uk.ac.open.kmi.msm4j.vocabulary.CC;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.List;

/**
 * ServiceReaderTest
 *
 * @author <a href="mailto:carlos.pedrinaci@open.ac.uk">Carlos Pedrinaci</a> (KMi - The Open University)
 * @since 27/11/2013
 */
@RunWith(JukitoRunner.class)
public class ServiceWriteReadTest {

    private static final Logger log = LoggerFactory.getLogger(ServiceWriteReadTest.class);

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
    public void testReadWrite(ServiceReader reader, ServiceWriter writer) throws Exception {
        FileInputStream fis = new FileInputStream(ServiceWriteReadTest.class.getResource("/twitter.n3").getFile());
        List<Service> services = reader.parse(fis,"http://iserve.kmi.open.ac.uk/iserve/id/services/ec6f7ee4-bbbe-4463-9cfa-3b5f54b6cc99/twitter",Syntax.N3);
        for(Service service:services){
            //seeAlso test
            Assert.assertTrue(service.getSeeAlsos().size() == 1);
            service.getSeeAlsos().remove(URI.create("http://www.programmableweb.com/api/twitter"));
            Assert.assertTrue(service.getSeeAlsos().size() == 0);
            service.addSeeAlso(URI.create("http://www.programmableweb.com/api/twitter"));
            service.addSeeAlso(URI.create("https://dev.twitter.com/docs/api/1.1"));

            //enrich the service description
            Provider provider = new Provider(URI.create("http://www.freebase.com/m/0289n8t"));
            provider.setLabel("Twitter");
            provider.setPopularity(3.5);
            service.setProvider(provider);
            service.setRecentMashups(3);
            service.setTotalMashups(254);
            service.setTwitterAccount(new TwitterAccount(URI.create("http://twitter.com/twitterapi")));
            Forum forum = new Forum(URI.create("http://iserve.com/twitter_forum"));
            forum.setVitality(5.22);
            forum.setSite(new URL("http://groups.google.com/group/twitter-development-talk"));
            service.setForum(forum);
            service.addLicense(URI.create(CC.ShareAlike.getURI()));
            service.addLicense(URI.create(CC.Attribution.getURI()));
            service.setLabel("Twitter API");
            service.setIssued(new Date());

            //write the service description
            File ontoFile = new File("temp.n3");
            FileOutputStream fos = new FileOutputStream(ontoFile);
            writer.serialise(service,fos,Syntax.N3);
            fos.flush();
            fos.close();

            //read the service description
            List<Service> servicesNt = reader.parse(new FileInputStream(ontoFile), "http://iserve.kmi.open.ac.uk/iserve/id/services/ec6f7ee4-bbbe-4463-9cfa-3b5f54b6cc99/twitter", Syntax.N3);

            // match if the service is equal
            Assert.assertTrue(service.equals(servicesNt.get(0)));
            ontoFile.delete();
        }
    }
}
