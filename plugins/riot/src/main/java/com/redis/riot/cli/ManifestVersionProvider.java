package com.redis.riot.cli;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.IVersionProvider;

/**
 * {@link picocli.CommandLine.IVersionProvider} implementation that returns version information from the jar file's
 * {@code /META-INF/MANIFEST.MF} file.
 */
public class ManifestVersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        return new String[] {
                // @formatter:off
                "", "      ▀        █     @|fg(4;1;1) ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄|@",
                " █ ██ █  ███  ████   @|fg(4;2;1) ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄|@",
                " ██   █ █   █  █     @|fg(5;4;1) ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄|@",
                " █    █ █   █  █     @|fg(1;4;1) ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄|@",
                " █    █  ███    ██   @|fg(0;3;4) ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄|@" + "  v" + getVersionString(), "" };
        // @formatter:on
    }

    public String getVersionString() {
        try {
            Enumeration<URL> resources = ManifestVersionProvider.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                Optional<String> version = version(resources.nextElement());
                if (version.isPresent()) {
                    return version.get();
                }
            }
        } catch (IOException e) {
            // Fail silently
        }
        return "N/A";
    }

    private Optional<String> version(URL url) {
        try {
            Manifest manifest = new Manifest(url.openStream());
            if (isApplicableManifest(manifest)) {
                Attributes attr = manifest.getMainAttributes();
                return Optional.of(String.valueOf(get(attr, "Implementation-Version")));
            }
            return Optional.empty();
        } catch (IOException e) {
            Logger log = LoggerFactory.getLogger(ManifestVersionProvider.class);
            log.warn("Unable to read from {}", url, e);
            return Optional.of("N/A");
        }
    }

    private static boolean isApplicableManifest(Manifest manifest) {
        Attributes attributes = manifest.getMainAttributes();
        return "riot".equals(get(attributes, "Implementation-Title"));
    }

    private static Object get(Attributes attributes, String key) {
        return attributes.get(new Attributes.Name(key));
    }

}
