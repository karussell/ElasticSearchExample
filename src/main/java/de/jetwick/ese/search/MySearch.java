/*
 *  Copyright 2010 Peter Karich jetwick_@_pannous_._info
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.jetwick.ese.search;

import org.elasticsearch.search.SearchHits;
import de.jetwick.ese.domain.MyUser;
import de.jetwick.ese.util.Helper;
import org.elasticsearch.client.Requests;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.action.bulk.BulkRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.elasticsearch.common.xcontent.XContentFactory.*;

/**
 * Provides search functionality via elasticsearch.
 * 
 * @author Peter Karich, jetwick_@_pannous_._info
 */
public class MySearch extends AbstractElasticSearch {

    public static final String NAME = "name";
    public static final String BIO = "bio";
    public static final String CREATED_AT = "created_at";
    private Logger logger = LoggerFactory.getLogger(getClass());

    public MySearch() {
    }

    public MySearch(String url, int port) {
        super(url, port);
    }

    public MySearch(Client client) {
        super(client);
    }

    @Override
    public String getIndexName() {
        return "myindex";
    }

    @Override
    public String getIndexType() {
        return "user";
    }

    SearchResponse query(MyQuery query) {
        return query.initRequestBuilder(client.prepareSearch(getIndexName())).
                execute().actionGet();
    }

    public XContentBuilder createDoc(MyUser u) {
        try {
            XContentBuilder b = jsonBuilder().startObject();
            b.field(BIO, u.getBio());
            b.field("age", u.getAge());
            if (u.getCreatedAt() != null)
                b.field(CREATED_AT, u.getCreatedAt());
            b.field(NAME, u.getName());
            b.endObject();
            return b;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public MyUser readDoc(Map<String, Object> source, String idAsStr) {
        // if we use in mapping: "_source" : {"enabled" : false}
        // we need to include all fields in query to use doc.getFields() 
        // instead of doc.getSource()

        String name = (String) source.get(NAME);
        long id = -1;
        try {
            id = Long.parseLong(idAsStr);
        } catch (Exception ex) {
            logger.error("Couldn't parse id:" + idAsStr);
        }
        MyUser user = new MyUser(id, name);
        user.setBio((String) source.get(BIO));
        user.setCreatedAt(Helper.toDateNoNPE((String) source.get(CREATED_AT)));

        return user;
    }

    public Collection<MyUser> search(String str) {
        List<MyUser> user = new ArrayList<MyUser>();
        search(user, new MyQuery().setQueryString(str));
        return user;
    }

    public SearchResponse search(MyQuery request) {
        return search(new ArrayList(), request);
    }

    public SearchResponse search(Collection<MyUser> users, MyQuery request) {
        SearchResponse rsp = query(request);
        SearchHit[] docs = rsp.getHits().getHits();
        for (SearchHit sd : docs) {
//            System.out.println(sd.getExplanation().toString());
            MyUser u = readDoc(sd.getSource(), sd.getId());
            users.add(u);
        }

        return rsp;
    }

    void update(MyUser user, boolean refresh) {
        try {
            XContentBuilder b = createDoc(user);
            if (b != null)
                feedDoc(Long.toString(user.getId()), b);

            if (refresh)
                refresh();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void bulkUpdate(Collection<MyUser> users, String indexName) {
        // now using bulk API instead of feeding each doc separate with feedDoc
        BulkRequestBuilder brb = client.prepareBulk();
        for (MyUser user : users) {
            String id = Long.toString(user.getId());
            XContentBuilder source = createDoc(user);
            brb.add(Requests.indexRequest(indexName).type(getIndexType()).id(id).source(source));
        }
        if (brb.numberOfActions() > 0) {
//            System.out.println("actions:" + brb.numberOfActions());
            brb.execute().actionGet();
        }
    }

    public MyUser findById(Long twitterId) {
        try {
            GetResponse rsp = client.prepareGet(getIndexName(), getIndexType(), "" + twitterId).
                    execute().actionGet();
            return readDoc(rsp.getSource(), rsp.getId());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public MyUser findByName(String uName) {
        try {
            List<MyUser> list = new ArrayList<MyUser>();
            search(list, new MyQuery().addFilter("user", uName));

            if (list.isEmpty())
                return null;

            return list.get(0);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * All indices has to be created before!
     */
    public void mergeIndices(Collection<String> indexList, String intoIndex, boolean forceRefresh) {
        if (forceRefresh)
            refresh(indexList);

        for (String index : indexList) {
            try {
                bulkUpdate(collectTweets(query(new MyQuery())), intoIndex);
            } catch (Exception ex) {
                logger.error("Failed to copy data from index " + index + " into " + intoIndex, ex);
            }
        }

        if (forceRefresh)
            refresh(intoIndex);
    }

    public List<MyUser> collectTweets(SearchResponse rsp) {
        SearchHits docs = rsp.getHits();
        List<MyUser> list = new ArrayList<MyUser>();
        for (SearchHit sd : docs) {
            list.add(readDoc(sd.getSource(), sd.getId()));
        }

        return list;
    }
}
