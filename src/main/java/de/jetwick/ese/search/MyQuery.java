/*
 * Copyright 2011 Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.jetwick.ese.search;

import java.util.Map;
import org.elasticsearch.index.query.xcontent.QueryStringQueryBuilder.Operator;
import org.elasticsearch.index.query.xcontent.XContentQueryBuilder;
import de.jetwick.ese.util.Helper;
import java.io.Serializable;
import java.util.LinkedHashMap;
import org.elasticsearch.client.action.search.SearchRequestBuilder;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.xcontent.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import static org.elasticsearch.common.xcontent.XContentFactory.*;

/**
 *
 * @author Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net
 */
public class MyQuery implements Serializable {

    private String queryString = "";
    private String sort = "";
    private Map<String, Object> filters = new LinkedHashMap<String, Object>();
    private int page;
    private int hitsPerPage;

    public MyQuery() {
    }

    public MyQuery setQueryString(String queryStr) {
        this.queryString = queryStr;
        return this;
    }

    public String getQueryString() {
        return queryString;
    }

    public SearchRequestBuilder initRequestBuilder(SearchRequestBuilder builder) {
        XContentQueryBuilder qb;

        if (!Helper.isEmpty(queryString)) {
            qb = QueryBuilders.queryString(queryString).defaultOperator(Operator.AND).
                    field(MySearch.TWEET_TXT).field(MySearch.NAME, 0).
                    allowLeadingWildcard(false).analyzer(getDefaultAnalyzer()).useDisMax(true);
        } else {
            qb = QueryBuilders.matchAllQuery();
        }

        // TODO set page, filters, ...
        
        if(!Helper.isEmpty(sort)) {
            String[] sorts = sort.split(" ");
            if(sorts.length == 2) {
                if("desc".equalsIgnoreCase(sorts[1]))
                    builder.addSort(sorts[0], SortOrder.DESC);
                else
                    builder.addSort(sorts[0], SortOrder.ASC);
            }
        }
                
        builder.setQuery(qb);
        return builder;
    }

    public MyQuery addFilter(String key, Object val) {
        filters.put(key, val);
        return this;
    }

    public static String toString(ToXContent tmp) {
        try {
            XContentBuilder builder = jsonBuilder();
            tmp.toXContent(builder, ToXContent.EMPTY_PARAMS);
            return builder.prettyPrint().string();
        } catch (Exception ex) {
            return "<ERROR:" + ex.getMessage() + ">";
        }
    }

    public String getDefaultAnalyzer() {
        return "search_analyzer";
    }

    public void setSort(String sortStr) {
        sort = sortStr;
    }

    public String getSort() {
        return sort;
    }

    public void setPaging(int page, int hitsPerPage) {
        this.page = page;
        this.hitsPerPage = hitsPerPage;
    }

    @Override
    public String toString() {
        return "q=" + queryString;
    }        
}
