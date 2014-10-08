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

import uk.ac.open.kmi.msm4j.vocabulary.MEDIA_TYPES;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Operations
 * <p/>
 * Author: Carlos Pedrinaci (KMi - The Open University)
 * Date: 20/05/2013
 * Time: 17:35
 */
public class Operation extends InvocableEntity {

    private List<MessageContent> inputs;
    private List<MessageContent> outputs;
    private List<MessageContent> faults;
    private List<MessageContent> inputFaults;
    private List<MessageContent> outputFaults;

    private Set<URI> producesContentTypes;
    private Set<URI> acceptsContentTypes;
    private URI method;
    private String address;

    public Operation(URI uri) {
        super(uri);
        this.inputs = new ArrayList<MessageContent>();
        this.outputs = new ArrayList<MessageContent>();
        this.faults = new ArrayList<MessageContent>();
        this.inputFaults = new ArrayList<MessageContent>();
        this.outputFaults = new ArrayList<MessageContent>();
        this.producesContentTypes = new HashSet<URI>();
        this.acceptsContentTypes = new HashSet<URI>();
    }

    public List<MessageContent> getInputs() {
        return inputs;
    }

    public void setInputs(List<MessageContent> inputs) {
        this.inputs = inputs;
    }

    public List<MessageContent> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<MessageContent> outputs) {
        this.outputs = outputs;
    }

    public List<MessageContent> getFaults() {
        return faults;
    }

    public void setFaults(List<MessageContent> faults) {
        this.faults = faults;
    }

    @Deprecated
    public List<MessageContent> getInputFaults() {
        return inputFaults;
    }

    @Deprecated
    public void setInputFaults(List<MessageContent> inputFaults) {
        this.inputFaults = inputFaults;
    }

    @Deprecated
    public List<MessageContent> getOutputFaults() {
        return outputFaults;
    }

    @Deprecated
    public void setOutputFaults(List<MessageContent> outputFaults) {
        this.outputFaults = outputFaults;
    }

    public boolean addInput(MessageContent mc) {
        if (mc != null) {
            return this.inputs.add(mc);
        }
        return false;
    }

    public boolean removeInput(MessageContent mc) {
        if (mc != null) {
            return this.inputs.remove(mc);
        }
        return false;
    }

    public boolean addOutput(MessageContent mc) {
        if (mc != null) {
            return this.outputs.add(mc);
        }
        return false;
    }

    public boolean removeOutput(MessageContent mc) {
        if (mc != null) {
            return this.outputs.remove(mc);
        }
        return false;
    }

    public boolean addFault(MessageContent mc) {
        if (mc != null) {
            return this.faults.add(mc);
        }
        return false;
    }

    public boolean removeFault(MessageContent mc) {
        if (mc != null) {
            return this.faults.remove(mc);
        }
        return false;
    }

    @Deprecated
    public boolean addInputFault(MessageContent mc) {
        if (mc != null) {
            return this.inputFaults.add(mc);
        }
        return false;
    }

    @Deprecated
    public boolean removeInputFault(MessageContent mc) {
        if (mc != null) {
            return this.inputFaults.remove(mc);
        }
        return false;
    }

    @Deprecated
    public boolean addOutputFault(MessageContent mc) {
        if (mc != null) {
            return this.outputFaults.add(mc);
        }
        return false;
    }

    @Deprecated
    public boolean removeOutputFault(MessageContent mc) {
        if (mc != null) {
            return this.outputFaults.remove(mc);
        }
        return false;
    }

    public Set<URI> getProducesContentTypes() {
        return producesContentTypes;
    }

    public void setProducesContentTypes(Set<URI> producesContentTypes) {
        this.producesContentTypes = producesContentTypes;
    }

    public boolean addProducesContentType(URI mediaType) {
        return this.producesContentTypes.add(mediaType);
    }

    public boolean removeProducesContentType(URI mediaType) {
        return this.producesContentTypes.remove(mediaType);
    }

    public Set<URI> getAcceptsContentTypes() {
        return acceptsContentTypes;
    }

    public void setAcceptsContentTypes(Set<URI> acceptsContentTypes) {
        this.acceptsContentTypes = acceptsContentTypes;
    }

    public boolean addAcceptsContentType(URI mediaType) {
        return this.acceptsContentTypes.add(mediaType);
    }

    public boolean removeAcceptsContentType(URI mediaType) {
        return this.acceptsContentTypes.remove(mediaType);
    }

    public boolean addProducesContentType(String mediaType) {
        return this.producesContentTypes.add(URI.create(new StringBuilder(MEDIA_TYPES.NS).append(mediaType).toString()));
    }

    public boolean addAcceptsContentType(String mediaType) {
        return this.acceptsContentTypes.add(URI.create(new StringBuilder(MEDIA_TYPES.NS).append(mediaType).toString()));
    }

    public boolean removeProducesContentType(String mediaType) {
        return this.producesContentTypes.remove(URI.create(new StringBuilder(MEDIA_TYPES.NS).append(mediaType).toString()));
    }

    public boolean removeAcceptsContentType(String mediaType) {
        return this.acceptsContentTypes.remove(URI.create(new StringBuilder(MEDIA_TYPES.NS).append(mediaType).toString()));
    }


    public URI getMethod() {
        return method;
    }

    public void setMethod(URI method) {
        this.method = method;
    }

    public void setMethod(String method) {
        this.method = URI.create(new StringBuilder("http://www.w3.org/2011/http-methods#").append(method.toUpperCase()).toString());
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
