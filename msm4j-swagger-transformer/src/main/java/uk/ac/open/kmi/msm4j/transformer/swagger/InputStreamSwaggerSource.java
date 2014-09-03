package uk.ac.open.kmi.msm4j.transformer.swagger;

import com.smartbear.swagger4j.URISwaggerSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;

/**
 * Created by Luca Panziera on 28/08/2014.
 */
public class InputStreamSwaggerSource extends URISwaggerSource {

    private InputStream inputStream;

    public InputStreamSwaggerSource(InputStream inputStream, URI uri) {
        super(uri);
        this.inputStream = inputStream;
    }

    @Override
    public Reader readResourceListing() throws IOException {
        return new InputStreamReader(inputStream);
    }

    @Override
    public Reader readApiDeclaration(String basePath, String path) throws IOException {
        return new InputStreamReader(inputStream);

    }

}
