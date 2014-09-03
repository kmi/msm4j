
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
