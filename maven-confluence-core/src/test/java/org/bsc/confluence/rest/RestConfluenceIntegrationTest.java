/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bsc.confluence.rest;

import java.util.List;
import javax.json.JsonObject;
import org.bsc.confluence.ConfluenceService.Credentials;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import org.hamcrest.core.IsNull;
import rx.functions.Func1;
import static java.lang.String.format;
import org.bsc.confluence.ConfluenceService.Model;
import rx.functions.Action1;
/**
 *
 * @author softphone
 */
public class RestConfluenceIntegrationTest {
    
    RESTConfluenceServiceImpl service;
    
    @Before
    public void initService() {
      
        final Credentials credentials = new Credentials("admin", "admin");
        
        service = new RESTConfluenceServiceImpl("http://192.168.99.100:8090/rest/api", credentials );
    }
    
    
    @Test
    public void getOrCreatePage() throws Exception  {
        
        final String spaceKey = "TEST";
        final String parentPageTitle = "Home";
        final String title = "test-storage";
        
        {
        TestSubscriber<JsonObject> test = new TestSubscriber<>();

        service.rxfindPage(spaceKey,title).subscribe(test);
        
        test.assertCompleted();
        test.assertValueCount(1);
        }
        
        
        {

            final TestSubscriber<List<JsonObject>> test = new TestSubscriber<>();

            Observable.concat( service.rxfindPage(spaceKey, parentPageTitle), service.rxfindPage(spaceKey,title) )
            .buffer(2)
            .subscribe(test);
            ;

            test.assertCompleted();
            test.assertValueCount(1);
            Assert.assertThat( test.getOnNextEvents().get(0).size(), IsEqual.equalTo(2) );
        }
        
        {
            final String parentPageTitle0 = "NOTEXISTS";
            
            final Exception ex = new Exception(format("parentPage [%s] doesn't exist!",parentPageTitle0));
            final Observable error =  Observable.error(ex);
            
            TestSubscriber<JsonObject> test = new TestSubscriber<>();
            
            service.rxfindPage(spaceKey, parentPageTitle0)
                    .switchIfEmpty( error )
                    .subscribe(test)
                    ;
            
            test.assertError(ex);
    
        }
        
        {
            final String title0 = "MyPage";
            
            final Observable error =  Observable.error(new Exception(format("parentPage [%s] doesn't exist!",parentPageTitle)));
            
            TestSubscriber<JsonObject> test = new TestSubscriber<>();
            
            service.rxfindPage(spaceKey, parentPageTitle)
                    .switchIfEmpty( error )
                    .doOnNext( new Action1<JsonObject>() {
                        @Override
                        public void call(JsonObject t) {
                            System.out.printf( "Parent Id: [%s]\n", t.getString("id"));
                        }                        
                    })
                    .flatMap(new Func1<JsonObject, Observable<JsonObject>>() {
                          @Override
                          public Observable<JsonObject> call(JsonObject parent) {
                            
                            final String id = parent.getString("id");
                            final JsonObject input = service.createPage(spaceKey, Integer.valueOf(id), title0);
                            
                            System.out.printf( "input\n%s\n", input.toString());
                            
                            return service.rxfindPage(spaceKey,title0)                                    
                                    .switchIfEmpty( service.rxCreatePage( input ));
                          }
                     })
                    .subscribe(test)
                    ;
            
            test.assertCompleted();
            test.assertValueCount(1);
            Assert.assertThat( test.getOnNextEvents().get(0), IsNull.notNullValue() );
            
            System.out.printf( "JsonObject\n%s", test.getOnNextEvents().get(0) );

    
        }
        
        {
            final Model.Page p = service.getOrCreatePage(spaceKey, parentPageTitle, "MyPage2");
            
            Assert.assertThat( p, IsNull.notNullValue());
        }
    }
}
