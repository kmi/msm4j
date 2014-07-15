package uk.ac.open.kmi.msm4j.nfp;

import uk.ac.open.kmi.msm4j.Resource;

import java.net.URI;
import java.net.URL;

/**
 * Created by Luca Panziera on 02/07/2014.
 */
public class Forum extends Resource {
    private Double vitality;
    private URL site;

    public Forum(URI uri) {
        super(uri);
    }

    public Double getVitality() {
        return vitality;
    }

    public void setVitality(Double vitality) {
        this.vitality = vitality;
    }

    public URL getSite() {
        return site;
    }

    public void setSite(URL site) {
        this.site = site;
    }
}
