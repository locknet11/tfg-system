package com.spulido.agent.worker.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Extracts bundled native tool binaries from classpath resources into a
 * runtime-writable temp directory at startup and resolves their absolute
 * paths by declared {@link BundledTool} role.
 *
 * <p>The provisioner reads {@code os.name} / {@code os.arch} system
 * properties at construction time, normalises them to a directory name
 * (e.g. {@code linux-amd64}, {@code darwin-arm64}), and extracts every
 * binary listed in the matching {@code tools/<os-arch>/} classpath
 * subdirectory. Extraction is idempotent across restarts via
 * {@link Files#createTempDirectory}.
 */
public class BundledToolProvisioner {

    private static final Logger log = LoggerFactory.getLogger(BundledToolProvisioner.class);

    private static final String RESOURCE_ROOT = "tools";

    /**
     * Non-executable data files extracted alongside the tool binaries. nmap
     * resolves its data files from the directory of its own executable, so
     * placing these next to the extracted {@code nmap} binary lets {@code -sV}
     * version detection work without any {@code --datadir}/{@code NMAPDIR} flag.
     * Files absent for a platform (e.g. macOS, which uses the host nmap) are
     * skipped.
     */
    private static final List<String> DATA_FILES = List.of(
            "nmap-service-probes",
            "nmap-services",
            "nmap-protocols",
            "nse_main.lua");

    /**
     * The nmap static build initializes NSE at startup even for {@code -sV}, so
     * it needs {@code nse_main.lua}, the {@code nselib/} tree, and a (possibly
     * empty) {@code scripts/} directory next to the binary. {@code nselib/} is
     * shipped zipped and expanded at extraction time.
     */
    private static final String NSE_ARCHIVE = "nmap-nselib.zip";

    private final String osArchDir;
    private final Path extractionDir;
    private final Map<BundledTool, Path> resolvedPaths = new EnumMap<>(BundledTool.class);

    /**
     * Constructs a provisioner for the current platform and extracts all
     * tool binaries into a unique temp directory.
     *
     * @throws ToolExtractionException when no resource directory matches the
     *         current platform or extraction fails
     */
    public BundledToolProvisioner() {
        String archDir;
        try {
            archDir = resolveOsArchDir();
        } catch (ToolExtractionException e) {
            log.warn("Cannot resolve OS/arch for bundled tools: {}. No tools will be available.", e.getMessage());
            archDir = null;
        }
        this.osArchDir = archDir;
        this.extractionDir = createExtractionDir();
        if (archDir != null) {
            extractAll();
        } else {
            log.warn("Bundled tools not provisioned: unsupported platform");
        }
        log.info("Bundled tools provisioned: {} binaries in {}", resolvedPaths.size(), extractionDir);
    }

    /**
     * Absolute path to the directory containing extracted executable tool binaries.
     */
    public Path getExtractionDirectory() {
        return extractionDir;
    }

    /**
     * Absolute path to the extracted executable for the given tool role,
     * or {@code null} if the tool is not available on this platform.
     */
    public Path getResolvedPath(BundledTool tool) {
        return resolvedPaths.get(tool);
    }

    /**
     * Returns the resolved binary path, throwing if the tool was not extracted.
     */
    public Path requireResolvedPath(BundledTool tool) {
        Path path = resolvedPaths.get(tool);
        if (path == null) {
            throw new ToolExtractionException(
                    "Bundled tool " + tool.getCapabilityDescription()
                            + " is not available for platform " + osArchDir);
        }
        return path;
    }

    // --- internal helpers ---

    private void extractAll() {
        for (BundledTool tool : BundledTool.values()) {
            String resourcePath = RESOURCE_ROOT + "/" + osArchDir + "/" + tool.getBinaryName();
            Resource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                log.debug("Bundled tool resource not found (skipping): {}", resourcePath);
                continue;
            }
            Path dest = extractionDir.resolve(tool.getBinaryName());
            try (InputStream in = resource.getInputStream()) {
                Files.copy(in, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                setExecutable(dest);
                resolvedPaths.put(tool, dest.toAbsolutePath());
                log.info("Extracted bundled tool {} -> {}", tool.getBinaryName(), dest.toAbsolutePath());
            } catch (IOException e) {
                log.warn("Failed to extract bundled tool {} for platform {}: {}",
                        tool.getBinaryName(), osArchDir, e.getMessage());
            }
        }
        if (resolvedPaths.isEmpty()) {
            log.warn("No bundled tool binaries found for platform {} under classpath:{}/{}/."
                            + " Steps depending on bundled tools will fail with TOOL_ERROR.",
                    osArchDir, RESOURCE_ROOT, osArchDir);
        }
        extractDataFiles();
        extractNmapNse();
    }

    /**
     * Extracts the tool data files (currently nmap's) into the same directory as
     * the binaries, so tools that resolve data relative to their own executable
     * find them with no extra configuration.
     */
    private void extractDataFiles() {
        for (String dataFile : DATA_FILES) {
            String resourcePath = RESOURCE_ROOT + "/" + osArchDir + "/" + dataFile;
            Resource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                log.debug("Bundled data file not found (skipping): {}", resourcePath);
                continue;
            }
            Path dest = extractionDir.resolve(dataFile);
            try (InputStream in = resource.getInputStream()) {
                Files.copy(in, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                log.info("Extracted bundled data file {} -> {}", dataFile, dest.toAbsolutePath());
            } catch (IOException e) {
                log.warn("Failed to extract bundled data file {} for platform {}: {}",
                        dataFile, osArchDir, e.getMessage());
            }
        }
    }

    /**
     * Expands the bundled {@code nselib/} zip next to the binaries and creates an
     * empty {@code scripts/} directory, both required for nmap's NSE
     * initialization. Skipped on platforms without a bundled NSE archive.
     */
    private void extractNmapNse() {
        String resourcePath = RESOURCE_ROOT + "/" + osArchDir + "/" + NSE_ARCHIVE;
        Resource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            log.debug("Bundled NSE archive not found (skipping): {}", resourcePath);
            return;
        }
        try (java.util.zip.ZipInputStream zip =
                     new java.util.zip.ZipInputStream(resource.getInputStream())) {
            java.util.zip.ZipEntry entry;
            int files = 0;
            while ((entry = zip.getNextEntry()) != null) {
                Path dest = extractionDir.resolve(entry.getName()).normalize();
                if (!dest.startsWith(extractionDir)) {
                    continue; // guard against zip-slip
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(zip, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    files++;
                }
                zip.closeEntry();
            }
            // nmap requires the scripts directory to exist for NSE init, even empty.
            Files.createDirectories(extractionDir.resolve("scripts"));
            log.info("Extracted bundled NSE library ({} files) + scripts dir into {}",
                    files, extractionDir);
        } catch (IOException e) {
            log.warn("Failed to extract bundled NSE archive for platform {}: {}",
                    osArchDir, e.getMessage());
        }
    }

    private Path createExtractionDir() {
        try {
            return Files.createTempDirectory("agent-tools");
        } catch (IOException e) {
            throw new ToolExtractionException("Failed to create temp directory for bundled tools", e);
        }
    }

    private static void setExecutable(Path file) throws IOException {
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);
        Files.setPosixFilePermissions(file, perms);
    }

    static String resolveOsArchDir() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        String osArch = System.getProperty("os.arch", "").toLowerCase();

        String osToken;
        if (osName.contains("linux")) {
            osToken = "linux";
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            osToken = "darwin";
        } else {
            throw new ToolExtractionException(
                    "Unsupported operating system: " + osName
                            + " — bundled tools are only available for Linux and macOS");
        }

        String archToken;
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            archToken = "amd64";
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            archToken = "arm64";
        } else {
            throw new ToolExtractionException(
                    "Unsupported architecture: " + osArch
                            + " — bundled tools are only available for amd64 and arm64");
        }

        return osToken + "-" + archToken;
    }
}
