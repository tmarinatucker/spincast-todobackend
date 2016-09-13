package org.spincast.todobackend.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.spincast.core.json.IJsonArray;
import org.spincast.core.json.IJsonObjectMutable;
import org.spincast.core.utils.ContentTypeDefaults;
import org.spincast.plugins.httpclient.IHttpResponse;
import org.spincast.shaded.org.apache.http.HttpStatus;

/**
 * We test the specs exactly as Todo-Backend does (except for the Cors part):
 * https://github.com/TodoBackend/todo-backend-js-spec/blob/master/js/specs.js 
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TodoBackendSpecsTest extends AppIntegrationTestBase {

    //====================================================================================
    // The pre-requisites
    //====================================================================================

    //==========================================
    // The api root responds to a GET (i.e. the server is 
    // up and accessible, CORS headers are set up)
    //==========================================
    @Test
    public void t01() throws Exception {
        IHttpResponse response = GET("/").send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());
    }

    //==========================================
    // The api root responds to a POST with the todo 
    // which was posted to it
    //==========================================
    @Test
    public void t02() throws Exception {

        IJsonObjectMutable jsonObject = getJsonManager().create();
        jsonObject.put("title", "a todo");

        IHttpResponse response = POST("/").setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());
    }

    //==========================================
    // The api root responds successfully to a DELETE
    //==========================================
    @Test
    public void t03() throws Exception {

        IHttpResponse response = DELETE("/").send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());
    }

    //==========================================
    // After a DELETE the api root responds to a GET 
    // with a JSON representation of an empty array
    //==========================================
    @Test
    public void t04() throws Exception {

        IHttpResponse response = DELETE("/").send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        response = GET("/").send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        assertEquals(ContentTypeDefaults.JSON.getMainVariationWithUtf8Charset(), response.getContentType());
        assertEquals("[]", response.getContentAsString());

        IJsonArray jsonArray = this.jsonManager.createArray(response.getContentAsString());
        assertNotNull(jsonArray);
        assertEquals(0, jsonArray.size());
    }

    //====================================================================================
    // Storing new todos by posting to the root url
    //====================================================================================

    //==========================================
    // Adds a new todo to the list of todos 
    // at the root url
    //==========================================
    @Test
    public void t05() throws Exception {

        deleteAllTodos();

        IJsonObjectMutable jsonObject = getJsonManager().create();
        jsonObject.put("title", "walk the dog");

        IHttpResponse response = POST("/").setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        IJsonArray allTodos = getAllTodos();
        assertEquals(1, allTodos.size());
        assertEquals("walk the dog", allTodos.getJsonObject(0).getString("title"));
    }

    //==========================================
    // Sets up a new todo as initially not completed
    //==========================================
    @Test
    public void t06() throws Exception {

        deleteAllTodos();

        IJsonObjectMutable jsonObject = getJsonManager().create();
        jsonObject.put("title", "blah");

        IHttpResponse response = POST("/").setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        jsonObject = this.jsonManager.create(response.getContentAsString());
        assertNotNull(jsonObject);
        assertEquals(false, jsonObject.getBoolean("completed"));
    }

    //==========================================
    // Each new todo has a url
    //==========================================
    @Test
    public void t07() throws Exception {

        deleteAllTodos();

        IJsonObjectMutable jsonObject = getJsonManager().create();
        jsonObject.put("title", "blah");

        IHttpResponse response = POST("/").setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        jsonObject = this.jsonManager.create(response.getContentAsString());
        assertNotNull(jsonObject);
        assertNotNull(jsonObject.getString("url"));
    }

    //==========================================
    // Each new todo has a url, which returns a todo
    //==========================================
    @Test
    public void t08() throws Exception {

        deleteAllTodos();

        IJsonObjectMutable jsonObject = getJsonManager().create();
        jsonObject.put("title", "my todo");

        IHttpResponse response = POST("/").setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        jsonObject = this.jsonManager.create(response.getContentAsString());
        assertNotNull(jsonObject);

        String url = jsonObject.getString("url");

        response = GET(url, true).send();

        jsonObject = this.jsonManager.create(response.getContentAsString());
        assertNotNull(jsonObject);
        assertEquals("my todo", jsonObject.getString("title"));
    }

    //====================================================================================
    // Working with an existing todo
    //====================================================================================

    //==========================================
    // Can navigate from a list of todos to an 
    // individual todo via urls
    //==========================================
    @Test
    public void t09() throws Exception {

        deleteAllTodos();

        IJsonObjectMutable jsonObject = getJsonManager().create();
        jsonObject.put("title", "todo the first");

        IHttpResponse response = POST("/").setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        jsonObject = getJsonManager().create();
        jsonObject.put("title", "todo the second");

        response = POST("/").setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        IJsonArray allTodos = getAllTodos();
        assertNotNull(allTodos);
        assertEquals(2, allTodos.size());

        response = GET(allTodos.getJsonObject(0).getString("url"), true).send();

        jsonObject = this.jsonManager.create(response.getContentAsString());
        assertNotNull(jsonObject);
        assertNotNull(jsonObject.getString("title"));
    }

    //==========================================
    // Can change the todo's title by PATCHing 
    // to the todo's url
    //==========================================
    @Test
    public void t10() throws Exception {

        deleteAllTodos();

        IJsonObjectMutable jsonObject = getJsonManager().create();
        jsonObject.put("title", "initial title");

        IHttpResponse response = POST("/").setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        jsonObject = this.jsonManager.create(response.getContentAsString());
        assertNotNull(jsonObject);

        String url = jsonObject.getString("url");

        jsonObject = getJsonManager().create();
        jsonObject.put("title", "bathe the cat");

        response = PATCH(url, true).setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        jsonObject = this.jsonManager.create(response.getContentAsString());
        assertNotNull(jsonObject);
        assertEquals("bathe the cat", jsonObject.getString("title"));
    }

    //==========================================
    // Can change the todo's completedness by 
    // PATCHing to the todo's url
    //==========================================
    @Test
    public void t11() throws Exception {

        deleteAllTodos();

        IJsonObjectMutable jsonObject = getJsonManager().create();
        jsonObject.put("title", "blah");

        IHttpResponse response = POST("/").setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        jsonObject = this.jsonManager.create(response.getContentAsString());
        assertNotNull(jsonObject);

        String url = jsonObject.getString("url");

        jsonObject = getJsonManager().create();
        jsonObject.put("completed", true);

        response = PATCH(url, true).setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        jsonObject = this.jsonManager.create(response.getContentAsString());
        assertNotNull(jsonObject);
        assertEquals(true, jsonObject.getBoolean("completed"));
    }

    //==========================================
    // Changes to a todo are persisted and show 
    // up when re-fetching the todo
    //==========================================
    @Test
    public void t12() throws Exception {

        deleteAllTodos();

        IJsonObjectMutable jsonObject = getJsonManager().create();
        jsonObject.put("title", "blah");

        IHttpResponse response = POST("/").setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        jsonObject = this.jsonManager.create(response.getContentAsString());
        assertNotNull(jsonObject);

        String url = jsonObject.getString("url");

        jsonObject = getJsonManager().create();
        jsonObject.put("title", "changed title");
        jsonObject.put("completed", true);

        response = PATCH(url, true).setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        response = GET(url, true).send();

        jsonObject = this.jsonManager.create(response.getContentAsString());
        assertNotNull(jsonObject);
        assertEquals(true, jsonObject.getBoolean("completed"));
        assertEquals("changed title", jsonObject.getString("title"));

        IJsonArray allTodos = getAllTodos();
        assertEquals(1, allTodos.size());
        assertEquals(true, allTodos.getJsonObject(0).getBoolean("completed"));
        assertEquals("changed title", allTodos.getJsonObject(0).getString("title"));
    }

    //==========================================
    // Can delete a todo making a DELETE request 
    // to the todo's url
    //==========================================
    @Test
    public void t13() throws Exception {

        deleteAllTodos();

        IJsonObjectMutable jsonObject = getJsonManager().create();
        jsonObject.put("title", "blah");

        IHttpResponse response = POST("/").setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        jsonObject = this.jsonManager.create(response.getContentAsString());
        assertNotNull(jsonObject);

        String url = jsonObject.getString("url");

        response = DELETE(url, true).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        IJsonArray allTodos = getAllTodos();
        assertEquals(0, allTodos.size());
    }

    //====================================================================================
    // Tracking todo order
    //====================================================================================
    @Test
    public void t14() throws Exception {

        IJsonObjectMutable jsonObject = getJsonManager().create();
        jsonObject.put("title", "blah");
        jsonObject.put("order", 523);

        IHttpResponse response = POST("/").setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        jsonObject = this.jsonManager.create(response.getContentAsString());
        assertNotNull(jsonObject);
        assertEquals(Integer.valueOf(523), jsonObject.getInteger("order"));
    }

    //==========================================
    // Can PATCH a todo to change its order
    //==========================================
    @Test
    public void t15() throws Exception {

        IJsonObjectMutable jsonObject = getJsonManager().create();
        jsonObject.put("order", 10);

        IHttpResponse response = POST("/").setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        jsonObject = this.jsonManager.create(response.getContentAsString());
        assertNotNull(jsonObject);

        String url = jsonObject.getString("url");

        jsonObject = getJsonManager().create();
        jsonObject.put("order", 95);

        response = PATCH(url, true).setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        jsonObject = this.jsonManager.create(response.getContentAsString());
        assertNotNull(jsonObject);
        assertEquals(Integer.valueOf(95), jsonObject.getInteger("order"));
    }

    //==========================================
    // Remembers changes to a todo's order
    //==========================================
    @Test
    public void t16() throws Exception {

        IJsonObjectMutable jsonObject = getJsonManager().create();
        jsonObject.put("order", 10);

        IHttpResponse response = POST("/").setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        jsonObject = this.jsonManager.create(response.getContentAsString());
        assertNotNull(jsonObject);

        String url = jsonObject.getString("url");

        jsonObject = getJsonManager().create();
        jsonObject.put("order", 95);

        response = PATCH(url, true).setEntityJson(jsonObject).send();
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        jsonObject = this.jsonManager.create(response.getContentAsString());
        url = jsonObject.getString("url");
        response = GET(url, true).send();

        jsonObject = this.jsonManager.create(response.getContentAsString());
        assertNotNull(jsonObject);
        assertEquals(Integer.valueOf(95), jsonObject.getInteger("order"));
    }

}
