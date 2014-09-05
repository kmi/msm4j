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

import uk.ac.open.kmi.msm4j.vocabulary.MSM;

import java.net.URI;

/**
 * Created by Luca Panziera on 04/09/2014.
 * <p/>
 * Generic definition of grounding
 */
public abstract class Grounding {

    // Default grounding type
    private URI groundingType = URI.create(MSM.isGroundedIn.getURI());

    public Grounding() {
    }

    public Grounding(URI groundingType) {
        this.groundingType = groundingType;
    }

    public URI getGroundingType() {
        return groundingType;
    }

    public void setGroundingType(URI groundingType) {
        this.groundingType = groundingType;
    }
}
