package uk.ac.open.kmi.msm4j.nfp;

import uk.ac.open.kmi.msm4j.Resource;

import java.net.URI;

/**
 * Created by Luca Panziera on 02/07/2014.
 */
public class Provider extends Resource {
    private Double popularity;
    public Provider(URI uri) {
        super(uri);
    }

    public Double getPopularity() {
        return popularity;
    }

    public void setPopularity(Double popularity) {
        this.popularity = popularity;
    }
}
