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

import de.jetwick.ese.search.MyQuery;
import java.util.Collection;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;

/**
 *
 * @author Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net
 */
public class SearchBox extends Panel {

    private String query;
    private final Form form;
    private TextField queryTF;

    // for test
    public SearchBox(String id) {
        this(id, null, null);
    }

    public SearchBox(String id, final String loggedInUser, String searchTypeAsStr) {
        super(id);

        form = new StatelessForm("searchform") {

            @Override
            public void onSubmit() {
                setResponsePage(getApplication().getHomePage(), getParams(query));
            }
        };
        form.setMarkupId("queryform");
        add(form);

        AutoCompleteSettings config = new AutoCompleteSettings().setUseHideShowCoveredIEFix(false);
        config.setThrottleDelay(200);

        // connect the form's textfield with the java property        
        queryTF = new org.apache.wicket.markup.html.form.TextField<Object>("textField",
                new PropertyModel(this, "query"));
        //queryTF.add(new DefaultFocusBehaviour());        
        form.add(queryTF);
        form.add(new BookmarkablePageLink("homelink", HomePage.class));
    }

    public void init(MyQuery query) {
        this.query = query.getQueryString();
    }

    public String getQuery() {
        if (query == null)
            query = "";

        return query;
    }

    protected void onSelectionChange(AjaxRequestTarget target, String str) {
    }

    protected Collection<String> getQueryChoices(String input) {
        throw new RuntimeException();
    }

    protected Collection<String> getUserChoices(String input) {
        throw new RuntimeException();
    }

    public PageParameters getParams(String tmpQuery) {
        PageParameters params = new PageParameters();
        if (tmpQuery != null && !tmpQuery.isEmpty())
            params.add("q", tmpQuery);

        return params;
    }
}
