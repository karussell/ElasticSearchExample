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

import org.elasticsearch.index.query.xcontent.FilterBuilders;
import org.elasticsearch.index.query.xcontent.XContentFilterBuilder;
import java.util.Set;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import org.elasticsearch.index.query.xcontent.QueryStringQueryBuilder.Operator;
import org.elasticsearch.index.query.xcontent.XContentQueryBuilder;
import de.jetwick.ese.util.Helper;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import org.elasticsearch.client.action.search.SearchRequestBuilder;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.xcontent.QueryBuilders;
import org.elasticsearch.search.facet.FacetBuilders;
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
    private Set<String> termFacets = new LinkedHashSet<String>();
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

        if (!Helper.isEmpty(sort)) {
            String[] sorts = sort.split(" ");
            if (sorts.length == 2) {
                if ("desc".equalsIgnoreCase(sorts[1]))
                    builder.addSort(sorts[0], SortOrder.DESC);
                else
                    builder.addSort(sorts[0], SortOrder.ASC);
            }
        }

        for (String tf : termFacets) {
            builder.addFacet(FacetBuilders.termsFacet(tf).field(tf));
        }

        XContentFilterBuilder fb = null;
        for (Entry<String, Object> e : filters.entrySet()) {
            XContentFilterBuilder tmp = FilterBuilders.termFilter(e.getKey(), e.getValue());
            if (fb != null)
                fb = FilterBuilders.andFilter(fb, tmp);
            else
                fb = tmp;
        }

        if (fb != null)
            qb = QueryBuilders.filteredQuery(qb, fb);

        builder.setFrom(page * hitsPerPage).setSize(hitsPerPage);
        builder.setQuery(qb);
        return builder;
    }

    public MyQuery addFilter(String key, Object val) {
        filters.put(key, val);
        return this;
    }

    public Collection<String> getFilters() {
        if (filters.isEmpty())
            return Collections.EMPTY_LIST;

        List<String> ret = new ArrayList<String>();
        for (Entry<String, Object> e : filters.entrySet()) {
            ret.add(e.getKey() + ":" + e.getValue().toString());
        }
        return ret;
    }

    public void changeFilter(String filter, Object val, boolean selected) {
        if (selected) {
            filters.remove(filter);
        } else {
            filters.put(filter, val);
        }
    }

    public void enableFacets() {
        // add more default facets 'HERE'
        termFacets.add(MySearch.NAME);
    }

    public MyQuery addTermFacet(String field) {
        termFacets.add(field);
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
        return "q=" + queryString + " filter:" + filters + " facets:" + termFacets;
    }
}
