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

import org.apache.wicket.Page;
import org.apache.wicket.guice.GuiceComponentInjector;
import org.apache.wicket.protocol.http.WebApplication;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.jetwick.ese.util.DefaultModule;
import org.apache.wicket.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application object for your web application. If you want to run this application without deploying, run the Start class.
 *
 * @author Peter Karich
 */
public class ExampleApp extends WebApplication {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Injector injector;

    public ExampleApp() {
        this(Guice.createInjector(new DefaultModule()));
    }

    public ExampleApp(Injector inj) {
        injector = inj;
    }

    protected GuiceComponentInjector getGuiceInjector() {
        return new GuiceComponentInjector(this, injector);
    }

    @Override
    protected void init() {
        super.init();

//        getApplicationSettings().setPageExpiredErrorPage(SessionTimeout.class);
//        getApplicationSettings().setInternalErrorPage(ErrorPage.class);

        // default is <em> </em> for disabled links
        getMarkupSettings().setDefaultBeforeDisabledLink(null);
        getMarkupSettings().setDefaultAfterDisabledLink(null);

//        if ("development".equals(cfg.getStage()))
//            getDebugSettings().setDevelopmentUtilitiesEnabled(true);

//        mountBookmarkablePage("about", About.class);
//        mountBookmarkablePage("imprint", Imprint.class);
        addComponentInstantiationListener(getGuiceInjector());
    }

    @Override
    public Class<? extends Page> getHomePage() {
        return HomePage.class;
    }

    // enable production mode
    @Override
    public String getConfigurationType() {
//        if ("production".equals(cfg.getStage()))
//            return Application.DEPLOYMENT;
//        else
        return Application.DEVELOPMENT;
    }
}
