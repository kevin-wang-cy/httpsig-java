package net.adamcin.httpsig.helpers.http4;

import net.adamcin.commons.testing.junit.TestBody;
import net.adamcin.httpsig.api.Constants;
import net.adamcin.httpsig.api.DefaultKeychain;
import net.adamcin.httpsig.helpers.HttpServerTestBody;
import net.adamcin.httpsig.jce.JCEKey;
import net.adamcin.httpsig.jce.KeyFormat;
import net.adamcin.httpsig.testutil.KeyTestUtil;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class Http4UtilTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Http4UtilTest.class);

    private static final String TEST_URL = "/index.html?path=/may/get/url/encoded&foo=bar";

    @Test
    public void testLogin() {
        TestBody.test(new HttpServerTestBody() {

            @Override protected void execute() throws Exception {

                List<String> headers = Arrays.asList(
                        Constants.HEADER_REQUEST_LINE,
                        Constants.HEADER_DATE);

                setServlet(new AdminServlet(headers));

                KeyPair keyPair = KeyTestUtil.getKeyPairFromProperties("b2048", "id_rsa");

                DefaultKeychain provider = new DefaultKeychain();
                provider.add(new JCEKey(KeyFormat.SSH_RSA, keyPair));

                DefaultHttpClient client = new DefaultHttpClient();

                Http4Util.enableAuth(client, provider, getKeyIdentifier());
                HttpUriRequest request = new HttpGet(getAbsoluteUrl(TEST_URL));
                HttpResponse response = client.execute(request);

                assertEquals("should return 200", 200, response.getStatusLine().getStatusCode());

            }
        });
    }

    @Test
    public void testAllHeaders() {
        TestBody.test(new HttpServerTestBody() {
            @Override protected void execute() throws Exception {

                List<String> headers = Arrays.asList(
                        Constants.HEADER_REQUEST_LINE,
                        Constants.HEADER_DATE,
                        "x-test"
                );

                setServlet(new AdminServlet(headers));

                KeyPair keyPair = KeyTestUtil.getKeyPairFromProperties("b2048", "id_rsa");

                DefaultKeychain provider = new DefaultKeychain();
                provider.add(new JCEKey(KeyFormat.SSH_RSA, keyPair));

                DefaultHttpClient client = new DefaultHttpClient();

                Http4Util.enableAuth(client, provider, getKeyIdentifier());

                HttpUriRequest badRequest = new HttpGet(getAbsoluteUrl(TEST_URL));
                HttpResponse badResponse = client.execute(badRequest);

                badResponse.getEntity().writeTo(new NullOutputStream());

                assertEquals("should return 401", 401, badResponse.getStatusLine().getStatusCode());

                HttpUriRequest goodRequest = new HttpGet(getAbsoluteUrl(TEST_URL));
                goodRequest.addHeader("x-test", "foo");
                HttpResponse goodResponse = client.execute(goodRequest);

                goodResponse.getEntity().writeTo(new NullOutputStream());
                assertEquals("should return 200", 200, goodResponse.getStatusLine().getStatusCode());
            }
        });
    }
}
