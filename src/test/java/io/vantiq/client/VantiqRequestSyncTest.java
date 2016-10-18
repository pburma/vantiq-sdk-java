package io.vantiq.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Mocked unit tests that exercise the request and
 * response processing support for Vantiq.  These tests
 * use the synchronous form for the methods.
 */
public class VantiqRequestSyncTest extends VantiqTestBase {

    private Vantiq vantiq;

    public VantiqRequestSyncTest() {
        super(false);
    }

    @Before
    public void setUpVantiq() throws Exception {
        vantiq = new Vantiq(server.url("/").toString());

        // Mock out the authentication
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder()
                                                .addProperty("accessToken", "234592dadf23412")
                                                .json()));

        // Run the authentication request
        vantiq.authenticate("joe", "no-one-will-guess");
        assertThat("Authenticated", vantiq.isAuthenticated(), is(true));
        server.takeRequest();
    }

    @After
    public void tearDownVantiq() {
        vantiq = null;
    }

    @Test
    public void testSelectQuery() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonArrayBuilder()
                                                .add(new JsonObjectBuilder().addProperty("a", 1).addProperty("b", "bingo").obj())
                                                .add(new JsonObjectBuilder().addProperty("a", 2).addProperty("b", "jenga").obj())
                                                .json()));

        VantiqResponse response = vantiq.select("MyType", null, null, null);
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body", ((List<JsonObject>) response.getBody()).get(0).get("a").getAsInt(), is(1));
    }

    @Test
    public void testSelectQueryWithConstraints() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonArrayBuilder()
                                                .add(new JsonObjectBuilder().addProperty("a", 1).addProperty("b", "bingo").obj())
                                                .json()));

        List<String>  props = Arrays.asList("_id", "b");
        JsonObject    where = new JsonObjectBuilder().addProperty("a", 1).obj();
        SortSpec       sort = new SortSpec("a", true);

        VantiqResponse response = vantiq.select("MyType", props, where, sort);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid props", url.queryParameter("props"), is("[\"_id\",\"b\"]"));
        assertThat("Valid where", url.queryParameter("where"), is("{\"a\":1}"));
        assertThat("Valid sort",  url.queryParameter("sort"),  is("{\"a\":-1}"));

        // Check the response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body", ((List<JsonObject>) response.getBody()).get(0).get("a").getAsInt(), is(1));
        assertThat("Valid body", ((List<JsonObject>) response.getBody()).get(0).get("b").getAsString(), is("bingo"));
    }

    @Test
    public void testSelectOne() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder()
                                                .addProperty("a", 1)
                                                .addProperty("b", "bingo")
                                                .json()));

        VantiqResponse response = vantiq.selectOne("MyType", "abc123");

        // We first check the request
        RecordedRequest request = server.takeRequest();
        assertThat("Valid path", request.getPath(), is("/api/v1/resources/MyType/abc123"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body", ((JsonObject) response.getBody()).get("a").getAsInt(), is(1));
        assertThat("Valid body", ((JsonObject) response.getBody()).get("b").getAsString(), is("bingo"));
    }

    @Test
    public void testCountQuery() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setHeader("X-Total-Count", 2)
                               .setBody(new JsonArrayBuilder()
                                                .add(new JsonObjectBuilder().addProperty("a", 1).addProperty("b", "bingo").obj())
                                                .add(new JsonObjectBuilder().addProperty("a", 2).addProperty("b", "jenga").obj())
                                                .json()));

        VantiqResponse response = vantiq.count("MyType", null);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Count parameter", url.queryParameter("count"), is("true"));
        assertThat("Props",           url.queryParameter("props"), is("[\"_id\"]"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Count", (Integer) response.getBody(), is(2));
    }

    @Test
    public void testCountQueryWithConstraints() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setHeader("X-Total-Count", 1)
                               .setBody(new JsonArrayBuilder()
                                                .add(new JsonObjectBuilder().addProperty("a", 1).addProperty("b", "bingo").obj())
                                                .json()));

        JsonObject where = new JsonObjectBuilder().addProperty("a", 1).obj();

        VantiqResponse response = vantiq.count("MyType", where);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Count parameter", url.queryParameter("count"), is("true"));
        assertThat("Props",           url.queryParameter("props"), is("[\"_id\"]"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Count", (Integer) response.getBody(), is(1));
    }

    @Test
    public void testInsert() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder()
                                                .addProperty("a", 1)
                                                .addProperty("b", "foo")
                                                .json()));

        JsonObject obj = new JsonObjectBuilder()
                            .addProperty("a", 1)
                            .addProperty("b", "foo")
                            .obj();

        VantiqResponse response = vantiq.insert("MyType", obj);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        assertThat("Valid path", request.getPath(), is("/api/v1/resources/MyType"));

        JsonObject reqObj = gson.fromJson(readAll(request.getBody()), JsonObject.class);
        assertThat("Valid request body", reqObj.get("a").getAsInt(),    is(1));
        assertThat("Valid request body", reqObj.get("b").getAsString(), is("foo"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body", ((JsonObject) response.getBody()).get("a").getAsInt(), is(1));
        assertThat("Valid body", ((JsonObject) response.getBody()).get("b").getAsString(), is("foo"));
    }

    @Test
    public void testUpdate() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder()
                                                .addProperty("a", 1)
                                                .addProperty("b", "foo")
                                                .json()));

        JsonObject obj = new JsonObjectBuilder()
                .addProperty("a", 1)
                .addProperty("b", "foo")
                .obj();
        String key = "12345";

        VantiqResponse response = vantiq.update("MyType", key, obj);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        assertThat("Valid path", request.getPath(), is("/api/v1/resources/MyType/12345"));

        JsonObject reqObj = gson.fromJson(readAll(request.getBody()), JsonObject.class);
        assertThat("Valid request body", reqObj.get("a").getAsInt(),    is(1));
        assertThat("Valid request body", reqObj.get("b").getAsString(), is("foo"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body", ((JsonObject) response.getBody()).get("a").getAsInt(), is(1));
        assertThat("Valid body", ((JsonObject) response.getBody()).get("b").getAsString(), is("foo"));
    }

    @Test
    public void testUpsert() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder()
                                                .addProperty("a", 1)
                                                .addProperty("b", "foo")
                                                .json()));

        JsonObject obj = new JsonObjectBuilder()
                .addProperty("a", 1)
                .addProperty("b", "foo")
                .obj();

        VantiqResponse response = vantiq.upsert("MyType", obj);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path",       url.encodedPath(),            is("/api/v1/resources/MyType"));
        assertThat("Upsert parameter", url.queryParameter("upsert"), is("true"));

        JsonObject reqObj = gson.fromJson(readAll(request.getBody()), JsonObject.class);
        assertThat("Valid request body", reqObj.get("a").getAsInt(),    is(1));
        assertThat("Valid request body", reqObj.get("b").getAsString(), is("foo"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body", ((JsonObject) response.getBody()).get("a").getAsInt(), is(1));
        assertThat("Valid body", ((JsonObject) response.getBody()).get("b").getAsString(), is("foo"));
    }

    @Test
    public void testDelete() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(204)
                               .setHeader("X-Total-Count", 2));


        JsonObject where = new JsonObjectBuilder().addProperty("a", 1).obj();

        VantiqResponse response = vantiq.delete("MyType", where);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path",      url.encodedPath(),           is("/api/v1/resources/MyType"));
        assertThat("Count parameter", url.queryParameter("count"), is("true"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Result", ((Boolean) response.getBody()), is(true));
    }

    @Test
    public void testDeleteOne() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(204));

        VantiqResponse response = vantiq.deleteOne("MyType", "12345");

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path",      url.encodedPath(),           is("/api/v1/resources/MyType/12345"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Result", ((Boolean) response.getBody()), is(true));
    }

    @Test
    public void testPublishTopic() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200));

        JsonObject msg = new JsonObjectBuilder().addProperty("a", 1).obj();

        VantiqResponse response = vantiq.publish(Vantiq.SystemResources.TOPICS.value(), "/foo/bar", msg);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/topics//foo/bar"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Result", ((Boolean) response.getBody()), is(true));
    }

    @Test
    public void testPublishSource() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200));

        JsonObject msg = new JsonObjectBuilder().addProperty("a", 1).obj();

        VantiqResponse response = vantiq.publish(Vantiq.SystemResources.SOURCES.value(), "foo", msg);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/sources/foo"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Result", ((Boolean) response.getBody()), is(true));
    }

    @Test
    public void testPreventPublishOnOtherTypes() throws Exception {
        try {
            vantiq.publish(Vantiq.SystemResources.TYPES.value(), "MyType", null);
            fail("Should only allow publish on sources and topics");
        } catch(IllegalArgumentException ex) {
            assertThat(ex.getMessage(), is("Only 'sources' and 'topics' support publish"));
        }
    }

    @Test
    public void testExecuteProcedure() throws Exception {
        server.enqueue(new MockResponse()
                                .setHeader("Content-Type", "application/json")
                                .setResponseCode(200)
                                .setBody(new JsonObjectBuilder()
                                    .addProperty("total", 3)
                                    .json()));

        JsonObject params = new JsonObjectBuilder()
                .addProperty("arg1", 1)
                .addProperty("arg2", 2)
                .obj();

        VantiqResponse response = vantiq.execute("adder", params);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/procedures/adder"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body", ((JsonObject) response.getBody()).get("total").getAsInt(), is(3));
    }

    @Test
    public void testMissingProcedure() throws Exception {
        server.enqueue(new MockResponse()
                                .setResponseCode(404));

        JsonObject params = new JsonObjectBuilder()
                .addProperty("arg1", 1)
                .addProperty("arg2", 2)
                .obj();

        VantiqResponse response = vantiq.execute("whoops", params);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/procedures/whoops"));

        // Check response
        assertTrue("Error response",    !response.isSuccess());
        assertThat("Missing procedure", response.getStatusCode(), is(404));
    }


    @Test
    public void testEvaluateModel() throws Exception {
        server.enqueue(new MockResponse()
                                .setHeader("Content-Type", "application/json")
                                .setResponseCode(200)
                                .setBody(new JsonObjectBuilder()
                                    .addProperty("total", 3)
                                    .json()));

        JsonObject params = new JsonObjectBuilder()
                .addProperty("arg1", 1)
                .addProperty("arg2", 2)
                .obj();

        VantiqResponse response = vantiq.evaluate("testModel", params);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/analyticsmodels/testModel"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body", ((JsonObject) response.getBody()).get("total").getAsInt(), is(3));
    }

    @Test
    public void testQuerySource() throws Exception {
        server.enqueue(new MockResponse()
                               .setHeader("Content-Type", "application/json")
                               .setResponseCode(200)
                               .setBody(new JsonObjectBuilder()
                                                .addProperty("total", 3)
                                                .json()));

        JsonObject params = new JsonObjectBuilder()
                .addProperty("arg1", 1)
                .addProperty("arg2", 2)
                .obj();

        VantiqResponse response = vantiq.query("adder", params);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/sources/adder/query"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body", ((JsonObject) response.getBody()).get("total").getAsInt(), is(3));
    }
}