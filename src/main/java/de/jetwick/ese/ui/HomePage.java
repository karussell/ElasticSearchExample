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
package de.jetwick.ese.ui;

import com.google.inject.Inject;
import com.google.inject.Provider;
import de.jetwick.ese.domain.MyTweet;
import de.jetwick.ese.search.MyQuery;
import de.jetwick.ese.search.MySearch;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO clean up this bloated class
 * @author Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net
 */
public class HomePage extends WebPage {

    private static final long serialVersionUID = 1L;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private MyQuery lastQuery;
    private int hitsPerPage = 10;
    private FeedbackPanel feedbackPanel;
    private ResultsPanel resultsPanel;
    private SearchBox searchBox;
    private FacetPanel facetPanel;
    @Inject
    private Provider<MySearch> searchProvider;

    // for testing
    HomePage() {
    }

    public HomePage(final PageParameters parameters) {
        init(createQuery(parameters), parameters, 0);
    }

    @Override
    protected void configureResponse() {
        super.configureResponse();
        // 1. searchAndGetUsers for wikileak
        // 2. apply de filter
        // 3. Show latest tweets (of user sebringl)
        // back button + de filter => WicketRuntimeException: component filterPanel:filterNames:1:filterValues:2:filterValueLink not found on page de.jetwick.ui.HomePage
        // http://www.richardnichols.net/2010/03/apache-wicket-force-page-reload-to-fix-ajax-back/
        // http://blogs.atlassian.com/developer/2007/12/cachecontrol_nostore_considere.html

        // TODO M2.1
        WebResponse response = getWebRequestCycle().getWebResponse();
        response.setHeader("Cache-Control", "no-cache, max-age=0,must-revalidate, no-store");
    }

    public MySearch getSearch() {
        return searchProvider.get();
    }

    public MyQuery createQuery(PageParameters parameters) {
        String queryStr = parameters.getString("q");
        if (queryStr == null)
            queryStr = "";

        return new MyQuery().setQueryString(queryStr);
    }

    public void updateAfterAjax(AjaxRequestTarget target, boolean updateSearchBox) {
        if (target != null) {
            target.addComponent(facetPanel);
            target.addComponent(resultsPanel);
            if (updateSearchBox)
                target.addComponent(searchBox);
            target.addComponent(feedbackPanel);
        }
    }

    public void init(MyQuery query, PageParameters parameters, int page) {
        setStatelessHint(true);
        feedbackPanel = new FeedbackPanel("feedback");
        add(feedbackPanel.setOutputMarkupId(true));
        add(new Label("title", new Model() {

            @Override
            public Serializable getObject() {
                String str = "";
                if (!searchBox.getQuery().isEmpty())
                    str += searchBox.getQuery() + " ";

                if (str.isEmpty())
                    return "Example Search";

                return "ElasticSearch Example | " + str + "| You know for search!";
            }
        }));

        resultsPanel = new ResultsPanel("results") {

            @Override
            public void onSortClicked(AjaxRequestTarget target, String sortStr) {
                if (lastQuery != null) {
                    lastQuery.setSort(sortStr);
                    doSearch(lastQuery, 0);
                    updateAfterAjax(target, false);
                }
            }
        };
        add(resultsPanel.setOutputMarkupId(true));

        searchBox = new SearchBox("searchbox");
        add(searchBox.setOutputMarkupId(true));
        
        facetPanel = new FacetPanel("filterPanel") {

            @Override
            public void onFacetChange(AjaxRequestTarget target, String filter, Object val, boolean selected) {
                if (lastQuery != null) {                    
                    lastQuery.changeFilter(filter, val, selected);
                } else {
                    logger.error("last query cannot be null but was! ... when clicking on facets!?");
                    return;
                }

                doOldSearch(0);
                updateAfterAjax(target, false);
            }            
        };
        add(facetPanel.setOutputMarkupId(true));
        
        query.enableFacets();
        
        doSearch(query, page);
    }

    /**
     * used from facets (which adds filter queries) and
     * from footer which changes the page
     */
    public void doOldSearch(int page) {
        logger.info(addIP("[stats] change old search. page:" + page));
        doSearch(lastQuery, page);
    }

    public void doSearch(MyQuery query, int page) {
        String queryString = searchBox.getQuery();

        // change text field
        searchBox.init(query);
        queryString = searchBox.getQuery();

        query.setPaging(page, hitsPerPage);

        long start = System.currentTimeMillis();
        long totalHits = 0;
        List<MyTweet> tweets = new ArrayList<MyTweet>();
        SearchResponse rsp = null;
        try {
            rsp = getSearch().search(tweets, query);
            totalHits = rsp.getHits().getTotalHits();
            logger.info(addIP("[stats] " + totalHits + " hits for: " + query.toString()));
        } catch (Exception ex) {
            logger.error("Error while searching " + query.toString(), ex);
        }

        resultsPanel.clear();
        String msg = "";
        if (totalHits > 0) {
            float time = (System.currentTimeMillis() - start) / 100.0f;
            time = Math.round(time) / 10f;
            msg = "Found " + totalHits + " tweets in " + time + " s";
        } else {
            if (tweets.isEmpty()) {
                if (!msg.isEmpty())
                    msg = " " + msg;
                msg = "Sorry, nothing found" + msg + ".";
            }
        }

        resultsPanel.setQueryMessage(msg);
        resultsPanel.setQuery(queryString);
        resultsPanel.setHitsPerPage(hitsPerPage);
        resultsPanel.setSort(query.getSort());
        for (MyTweet tweet : tweets) {
            resultsPanel.add(tweet);
        }

        facetPanel.update(rsp, query);
//        navigationPanel.setPage(page);
//        navigationPanel.setHits(totalHits);
//        navigationPanel.setHitsPerPage(hitsPerPage);
//        navigationPanel.updateVisibility(); 
        lastQuery = query;
        logger.info(addIP("Finished Constructing UI."));
    }

    String addIP(String str) {
        return str + " session=" + getWebRequestCycle().getSession().getId()
                + " " + lastQuery;
    }
}
