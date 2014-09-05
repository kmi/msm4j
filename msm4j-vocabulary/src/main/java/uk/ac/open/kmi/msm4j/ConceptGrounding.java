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

package uk.ac.open.kmi.msm4j;

import java.net.URI;

/**
 * Created by Luca Panziera on 04/09/2014.
 * <p/>
 * Implementation of the grounding as a concept identified by a URI
 */

// Implementation of the
public class ConceptGrounding extends Grounding {
    private URI uri;

    public ConceptGrounding(URI uri, URI groundingType) {
        super(groundingType);
        this.uri = uri;
    }

    public ConceptGrounding(URI uri) {
        this.uri = uri;
    }

    public URI getUri() {
        return uri;
    }
}
