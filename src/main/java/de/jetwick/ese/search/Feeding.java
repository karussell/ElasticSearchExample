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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import de.jetwick.ese.domain.MyTweet;
import de.jetwick.ese.util.DefaultModule;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.elasticsearch.common.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.Query;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

/**
 *
 * @author Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net
 */
public class Feeding {

    public static void main(String[] args) {
        new Feeding().start(false);
    }
    private Random rand = new Random();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void start(boolean fake) {
        Module module = new DefaultModule();
        Injector injector = Guice.createInjector(module);
        MySearch tws = injector.getInstance(MySearch.class);
        Collection<MyTweet> tweets;
        if (fake)
            tweets = createFake();
        else
            tweets = createReal();

        logger.info("Now feeding");
        StopWatch sw = new StopWatch().start();
        tws.bulkUpdate(tweets, tws.getIndexName());
        long time = sw.stop().totalTime().seconds();
        if (time == 0)
            time = 1;
        logger.info("Feeded " + tweets.size() + " users => " + tweets.size() / time + " users/sec");
    }

    String createRandomWord(int chars) {
        String word = "";
        for (int i = 0; i < chars; i++) {
            word = word + (char) (rand.nextInt(58) + 65);
        }
        return word;
    }

    public Collection<MyTweet> createFake() {
        List<MyTweet> tweets = new ArrayList<MyTweet>();
        int MAX = 20000;
        for (int i = 0; i < MAX; i++) {
            MyTweet tweet = new MyTweet(i, "peter " + i);
            tweet.setFromUserId(i % 100);
            tweet.setCreatedAt(new Date(i));
            tweet.setText(createRandomWord(4) + " test " + createRandomWord(10));
            tweets.add(tweet);
        }
        return tweets;
    }

    public Collection<MyTweet> createReal() {
        List<MyTweet> tweets = new ArrayList<MyTweet>();
        try {
            // get some tweets about java
            Twitter twitter4j = new TwitterFactory().getInstance();
            for (int i = 0; i < 3; i++) {
                Query q = new Query("java");
                q.setRpp(100);
                for (Tweet tw : twitter4j.search(q).getTweets()) {
                    MyTweet myTw = new MyTweet(tw.getId(), tw.getFromUser());
                    myTw.setText(tw.getText());
                    myTw.setCreatedAt(tw.getCreatedAt());
                    myTw.setFromUserId(tw.getFromUserId());
                    tweets.add(myTw);
                }
                Thread.sleep(1000);
            }
        } catch (Exception ex) {
            logger.error("Error while grabbing tweets from twitter!", ex);
        }

        return tweets;
    }
}
