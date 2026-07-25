package io.github.outakeshi.indexsmith.generator.plugin;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Objects;

/** Parses the allow-listed generator XML configuration format. */
public final class XmlUtil {
    private static final long MAX_CONFIGURATION_BYTES = 1024L * 1024L;
    private static final XStream XSTREAM;

    static {
        XStream xStream = new XStream(new StaxDriver());
        Class<?>[] configurationTypes = {GeneratorConfiguration.class, TargetPackage.class, Cluster.class,
                Connection.class, IndexInfo.class};
        xStream.processAnnotations(configurationTypes);
        xStream.allowTypes(configurationTypes);
        xStream.setClassLoader(XmlUtil.class.getClassLoader());
        XSTREAM = xStream;
    }

    private XmlUtil() { }

    @SuppressWarnings("unchecked")
    public static <T> T parse(File file, T t) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(t, "target");
        if (Files.size(file.toPath()) > MAX_CONFIGURATION_BYTES) {
            throw new IllegalArgumentException("Configuration file exceeds 1 MiB");
        }
        String xml = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        return parse(xml, t);
    }

    @SuppressWarnings("unchecked")
    public static <T> T parse(String xmlString, T t) {
        Objects.requireNonNull(xmlString, "xmlString");
        Objects.requireNonNull(t, "target");
        String normalized = xmlString.toUpperCase(Locale.ROOT);
        if (normalized.contains("<!DOCTYPE") || normalized.contains("<!ENTITY")) {
            throw new IllegalArgumentException("DOCTYPE and ENTITY declarations are not allowed");
        }
        synchronized (XSTREAM) {
            XSTREAM.setClassLoader(GeneratorConfiguration.class.getClassLoader());
            return (T) XSTREAM.fromXML(xmlString, t);
        }
    }
}
