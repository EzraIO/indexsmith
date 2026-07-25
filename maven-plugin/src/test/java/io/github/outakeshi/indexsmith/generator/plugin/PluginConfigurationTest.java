package io.github.outakeshi.indexsmith.generator.plugin;

import io.github.outakeshi.indexsmith.generator.model.EsConfigContext;
import io.github.outakeshi.indexsmith.generator.model.EsIndexInfo;
import io.github.outakeshi.indexsmith.generator.model.WriteMode;
import io.github.outakeshi.indexsmith.client.ClientConfiguration;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PluginConfigurationTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void parsesConfigurationFromStringAndFile() throws Exception {
        String xml = configurationXml();
        GeneratorConfiguration fromString = XmlUtil.parse(xml, new GeneratorConfiguration());
        assertConfiguration(fromString);

        File file = temporaryFolder.newFile("esConfig.xml");
        Files.write(file.toPath(), xml.getBytes(StandardCharsets.UTF_8));
        assertConfiguration(XmlUtil.parse(file, new GeneratorConfiguration()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsXmlExternalEntityDeclarations() {
        XmlUtil.parse("<!DOCTYPE generatorConfiguration [<!ENTITY xxe SYSTEM 'file:///etc/passwd'>]>"
                + "<generatorConfiguration>&xxe;</generatorConfiguration>", new GeneratorConfiguration());
    }

    @Test
    public void interpolatesEnvironmentValuesWithoutChangingUnrelatedFields() {
        GeneratorConfiguration configuration = XmlUtil.parse(configurationXml(), new GeneratorConfiguration());
        configuration.getClusters().get(0).getConnection().setPassWord("${env.ES_PASSWORD}");

        ConfigurationInterpolator.interpolate(configuration,
                Collections.singletonMap("ES_PASSWORD", "test-password"));

        assertEquals("test-password", configuration.getClusters().get(0).getConnection().getPassWord());
        assertEquals("FAIL", configuration.getWriteMode());
    }

    @Test
    public void interpolatesApiKeyEnvironmentValuesBeforeBuildingClient() {
        GeneratorConfiguration configuration = XmlUtil.parse(configurationXml(), new GeneratorConfiguration());
        Connection connection = configuration.getClusters().get(0).getConnection();
        connection.setUserName(null);
        connection.setPassWord(null);
        connection.setApiKeyId("${env.ES_API_KEY_ID}");
        connection.setApiKeySecret("${env.ES_API_KEY_SECRET}");
        Map<String, String> environment = new HashMap<>();
        environment.put("ES_API_KEY_ID", "id");
        environment.put("ES_API_KEY_SECRET", "secret");

        ConfigurationInterpolator.interpolate(configuration, environment);
        ClientConfiguration client = EsClientUtil.buildConfiguration(connection);

        assertEquals("aWQ6c2VjcmV0", client.getApiKey());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOutputDirectoryOutsideProject() {
        MavenProject project = new MavenProject();
        project.setFile(new File(temporaryFolder.getRoot(), "pom.xml"));
        BeanConvertUtil.resolveOutputDirectory(project, "../outside");
    }

    @Test
    public void convertsIndexConfigurationAndTrimsLists() {
        IndexInfo input = new IndexInfo();
        input.setName("orders");
        input.setArrFields("items, tags ");
        input.setQueryConditionFields("status, items.skuId");

        EsIndexInfo result = BeanConvertUtil.convert2EsIndexInfo(input);

        assertEquals("orders", result.getIndexNames());
        assertTrue(result.getArrFieldPath().contains("tags"));
        assertTrue(result.getQueryConditionFields().contains("items.skuId"));
    }

    @Test
    public void buildsClientConfigurationForAllConfiguredHosts() throws Exception {
        GeneratorConfiguration configuration = XmlUtil.parse(configurationXml(), new GeneratorConfiguration());

        ClientConfiguration client = EsClientUtil.buildConfiguration(
                configuration.getClusters().get(0).getConnection());

        assertEquals(2, client.getEndpoints().size());
        assertEquals("http://localhost:9200", client.getEndpoints().get(0));
        assertEquals("elastic", client.getUsername());
    }

    @Test
    public void buildsApiKeyClientConfiguration() {
        Connection connection = new Connection();
        connection.setHostPorts("https://localhost:9200");
        connection.setApiKeyId("id");
        connection.setApiKeySecret("secret");

        ClientConfiguration client = EsClientUtil.buildConfiguration(connection);

        assertEquals("aWQ6c2VjcmV0", client.getApiKey());
        assertEquals(null, client.getUsername());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMixedAuthenticationInGeneratorConfiguration() {
        Connection connection = new Connection();
        connection.setHostPorts("localhost:9200");
        connection.setUserName("elastic");
        connection.setPassWord("password");
        connection.setApiKeyId("id");
        connection.setApiKeySecret("secret");

        EsClientUtil.buildConfiguration(connection);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPartialApiKeyInGeneratorConfiguration() {
        Connection connection = new Connection();
        connection.setHostPorts("localhost:9200");
        connection.setApiKeyId("id");

        EsClientUtil.buildConfiguration(connection);
    }

    @Test
    public void buildsGeneratorContextFromEs7MappingResponse() throws Exception {
        assertMappingContext("7.17.22");
    }

    @Test
    public void buildsGeneratorContextFromEs8MappingResponse() throws Exception {
        assertMappingContext("8.17.3");
    }

    private void assertMappingContext(String version) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            if ("/".equals(exchange.getRequestURI().getPath())) {
                respond(exchange, "{\"version\":{\"number\":\"" + version + "\"}}");
            } else {
                respond(exchange, "{\"orders\":{\"aliases\":{},\"mappings\":{\"properties\":{"
                        + "\"status\":{\"type\":\"keyword\"}}},\"settings\":{}}}");
            }
        });
        server.start();

        Connection connection = new Connection();
        connection.setHostPorts("localhost:" + server.getAddress().getPort());
        connection.setUserName("");
        connection.setPassWord("");
        connection.setConnectTimeOut(1000);
        connection.setSocketTimeout(1000);
        connection.setConnectionRequestTimeout(1000);
        IndexInfo index = new IndexInfo();
        index.setName("orders");
        Cluster cluster = new Cluster();
        cluster.setConnection(connection);
        cluster.setIndexes(Collections.singletonList(index));
        TargetPackage packages = new TargetPackage();
        packages.setModel("example.model");
        packages.setFieldEnum("example.fields");
        packages.setEsQueryModel("example.request");
        packages.setClient("example.client");
        GeneratorConfiguration configuration = new GeneratorConfiguration();
        configuration.setSourceDir("target/generated-sources");
        configuration.setWriteMode("skip");
        configuration.setTargetPackage(packages);
        MavenProject project = new MavenProject();
        project.setFile(new File(temporaryFolder.getRoot(), "pom.xml"));

        EsConfigContext context = null;
        try {
            context = BeanConvertUtil.buildEsContext(project, configuration, cluster);
            assertEquals(new File(temporaryFolder.getRoot(), "target/generated-sources").getAbsolutePath(),
                    context.getTargetProject());
            assertEquals("example.model", context.getModelTargetPackage().asString());
            assertEquals("keyword", context.getFieldNode("orders").getFieldMap().get("status").getEsType());
            assertEquals(WriteMode.SKIP, context.getWriteMode());
        } finally {
            if (context != null) context.close();
            server.stop(0);
        }
    }

    @Test
    public void convertsMissingOptionalFieldListsToEmptySets() {
        IndexInfo input = new IndexInfo();
        input.setName("orders");

        EsIndexInfo result = BeanConvertUtil.convert2EsIndexInfo(input);

        assertTrue(result.getArrFieldPath().isEmpty());
        assertTrue(result.getQueryConditionFields().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsConnectionWithoutEndpoints() {
        EsClientUtil.buildConfiguration(new Connection());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankIndexName() {
        BeanConvertUtil.convert2EsIndexInfo(new IndexInfo());
    }

    private static void assertConfiguration(GeneratorConfiguration configuration) {
        assertEquals("/src/main/java", configuration.getSourceDir());
        assertEquals("FAIL", configuration.getWriteMode());
        assertEquals("example.model", configuration.getTargetPackage().getModel());
        assertEquals(2, configuration.getClusters().get(0).getIndexes().size());
        assertEquals(1000, configuration.getClusters().get(0).getConnection().getConnectTimeOut());
    }

    private static String configurationXml() {
        return "<generatorConfiguration>"
                + "<sourceDir>/src/main/java</sourceDir>"
                + "<writeMode>FAIL</writeMode>"
                + "<targetPackage><model>example.model</model><fieldEnum>example.field</fieldEnum>"
                + "<esQueryModel>example.request</esQueryModel><client>example.client</client></targetPackage>"
                + "<clusters><cluster><connection><hostPorts>localhost:9200,localhost:9201</hostPorts>"
                + "<userName>elastic</userName><passWord>secret</passWord><connectTimeOut>1000</connectTimeOut>"
                + "<socketTimeout>2000</socketTimeout><connectionRequestTimeout>3000</connectionRequestTimeout>"
                + "</connection><indexes><index><name>orders</name></index><index><name>products</name></index>"
                + "</indexes></cluster></clusters></generatorConfiguration>";
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
