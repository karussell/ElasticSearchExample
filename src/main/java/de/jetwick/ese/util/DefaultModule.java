/**
 * Copyright (C) 2010 Peter Karich <jetwick_@_pannous_._info>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.jetwick.ese.util;

import com.google.inject.AbstractModule;
import de.jetwick.ese.search.AbstractElasticSearch;
import de.jetwick.ese.search.ElasticNode;
import de.jetwick.ese.search.MySearch;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultModule extends AbstractModule {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public DefaultModule() {
    }

    @Override
    protected void configure() {
        installSearchModule();
    }

    public void installSearchModule() {
        try {
            Client client = AbstractElasticSearch.createClient(ElasticNode.CLUSTER,
                    "127.0.0.1", ElasticNode.PORT);

            MySearch tweetSearch = new MySearch(client);
            tweetSearch.nodeInfo();
            bind(MySearch.class).toInstance(tweetSearch);
        } catch (Exception ex) {
            logger.error("Start ElasticNode first!", ex);
        }
    }
}
