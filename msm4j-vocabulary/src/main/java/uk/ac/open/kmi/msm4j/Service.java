/*
 * Copyright (c) 2015. Knowledge Media Institute - The Open University
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

import uk.ac.open.kmi.msm4j.nfp.Forum;
import uk.ac.open.kmi.msm4j.nfp.Provider;
import uk.ac.open.kmi.msm4j.nfp.TwitterAccount;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class capturing Services
 * <p/>
 * Author: Carlos Pedrinaci (KMi - The Open University)
 * Date: 20/05/2013
 * Time: 17:08
 */
public class Service extends InvocableEntity {

    private List<Operation> operations;
    private String address;

    // MSM-NFP extension
    private Forum forum;
    private TwitterAccount twitterAccount;
    private Provider provider;
    private Integer totalMashups;
    private Integer recentMashups;
    private URL documentation;

    public Service(URI uri) {
        super(uri);
        this.operations = new ArrayList<Operation>();
        // By default initialise to now
        this.setCreated(new Date());
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public void setOperations(List<Operation> operations) {
        this.operations = operations;
    }


    public boolean addOperation(Operation op) {
        if (op != null) {
            return this.operations.add(op);
        }
        return false;
    }

    public boolean removeOperation(Operation op) {
        if (op != null) {
            return this.operations.remove(op);
        }
        return false;
    }


    public Forum getForum() {
        return forum;
    }

    public void setForum(Forum forum) {
        this.forum = forum;
    }

    public TwitterAccount getTwitterAccount() {
        return twitterAccount;
    }

    public void setTwitterAccount(TwitterAccount twitterAccount) {
        this.twitterAccount = twitterAccount;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }


    public Integer getTotalMashups() {
        return totalMashups;
    }

    public void setTotalMashups(Integer totalMashups) {
        this.totalMashups = totalMashups;
    }

    public Integer getRecentMashups() {
        return recentMashups;
    }

    public void setRecentMashups(Integer recentMashups) {
        this.recentMashups = recentMashups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Service service = (Service) o;

        if (forum != null ? !forum.equals(service.forum) : service.forum != null) return false;
        if (operations != null ? !operations.equals(service.operations) : service.operations != null) return false;
        if (provider != null ? !provider.equals(service.provider) : service.provider != null) return false;
        if (recentMashups != null ? !recentMashups.equals(service.recentMashups) : service.recentMashups != null)
            return false;
        if (totalMashups != null ? !totalMashups.equals(service.totalMashups) : service.totalMashups != null)
            return false;
        if (twitterAccount != null ? !twitterAccount.equals(service.twitterAccount) : service.twitterAccount != null)
            return false;
        if (documentation != null ? !documentation.equals(service.documentation) : service.documentation != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (operations != null ? operations.hashCode() : 0);
        result = 31 * result + (forum != null ? forum.hashCode() : 0);
        result = 31 * result + (twitterAccount != null ? twitterAccount.hashCode() : 0);
        result = 31 * result + (provider != null ? provider.hashCode() : 0);
        result = 31 * result + (totalMashups != null ? totalMashups.hashCode() : 0);
        result = 31 * result + (recentMashups != null ? recentMashups.hashCode() : 0);
        return result;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public URL getDocumentation() {
        return documentation;
    }

    public void setDocumentation(URL documentation) {
        this.documentation = documentation;
    }
}
