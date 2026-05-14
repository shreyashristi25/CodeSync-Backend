package com.codesync.execution.service;

import com.codesync.execution.entity.ExecutionLanguage;
import com.codesync.execution.entity.SupportedLanguage;
import com.codesync.execution.exception.BadRequestException;
import com.codesync.execution.repository.SupportedLanguageRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DockerContainerService {
    private final SupportedLanguageRepository languageRepository;

    @Value("${codesync.execution.timeout-seconds:10}")
    private int timeoutSeconds;

    @Value("${codesync.execution.memory-limit-mb:256}")
    private int memoryLimitMb;

    private static final String MEMORY_LIMIT = "256m";
    private static final String CPU_LIMIT = "1.0";
    private static final String NETWORK_DISABLED = "none";
    private static final String WORKING_DIR = "/tmp";

    public DockerExecutionResult executeInContainer(
            String code, ExecutionLanguage language, String stdin) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Code is required");
        }

        SupportedLanguage lang = languageRepository.findByCode(language.name())
                .orElseThrow(() -> new BadRequestException("Language not supported: " + language.name()));

        String dockerImage = lang.getDockerImage() != null ? lang.getDockerImage() : "ubuntu:22.04";
        String entryPoint = lang.getEntryPoint() != null ? lang.getEntryPoint() : language.name().toLowerCase();

        try {
            return runInDockerContainer(code, dockerImage, entryPoint, language.name(), stdin);
        } catch (Exception e) {
            log.error("Docker execution failed", e);
            return new DockerExecutionResult(1, "", "Execution error: " + e.getMessage(), 0L, 0L);
        }
    }

    private DockerExecutionResult runInDockerContainer(
            String code, String dockerImage, String entryPoint, String language, String stdin) throws Exception {
        
        String containerName = "codesync-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        
        Path codeFile = Files.createTempFile("codesync-", getFileExtension(language));
        Files.writeString(codeFile, code, StandardCharsets.UTF_8);
        
        Path stdinFile = null;
        if (stdin != null && !stdin.isBlank()) {
            stdinFile = Files.createTempFile("codesync-stdin-", ".txt");
            Files.writeString(stdinFile, stdin, StandardCharsets.UTF_8);
        }

        try {
            String dockerCmd = buildDockerCommand(containerName, dockerImage, entryPoint, language, code, codeFile, stdinFile);
            
            ProcessBuilder pb = new ProcessBuilder(dockerCmd);
            pb.redirectErrorStream(true);
            
            long startTime = System.currentTimeMillis();
            
            Process process = pb.start();
            
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            long executionTimeMs = System.currentTimeMillis() - startTime;
            
            int exitCode;
            if (!finished) {
                process.destroyForcibly();
                executeCommand("docker kill " + containerName);
                executeCommand("docker rm -f " + containerName);
                return new DockerExecutionResult(124, output + "\n[Execution timed out]", "", executionTimeMs, memoryLimitMb * 1024);
            }
            
            exitCode = process.exitValue();
            
            executeCommand("docker rm -f " + containerName);
            
            long memoryUsedKb = estimateMemoryUsage(output);
            
            return new DockerExecutionResult(exitCode, output, "", executionTimeMs, memoryUsedKb);
            
        } finally {
            Files.deleteIfExists(codeFile);
            if (stdinFile != null) {
                Files.deleteIfExists(stdinFile);
            }
        }
    }

    private String buildDockerCommand(String containerName, String dockerImage, String entryPoint, 
            String language, String code, Path codeFile, Path stdinFile) {
        
        StringBuilder cmd = new StringBuilder();
        cmd.append("docker run --rm ");
        cmd.append("--name ").append(containerName).append(" ");
        cmd.append("--memory ").append(MEMORY_LIMIT).append(" ");
        cmd.append("--cpus ").append(CPU_LIMIT).append(" ");
        cmd.append("--network ").append(NETWORK_DISABLED).append(" ");
        cmd.append("-v ").append(codeFile.getParent()).append(":").append(WORKING_DIR).append(":ro ");
        
        if (stdinFile != null) {
            cmd.append("-v ").append(stdinFile.getParent()).append(":").append(WORKING_DIR).append(":ro ");
        }
        
        cmd.append("-w ").append(WORKING_DIR).append(" ");
        cmd.append("--ulimit fsize=1048576 "); // 1GB file size limit
        
        cmd.append(dockerImage).append(" ");
        
        switch (language.toUpperCase()) {
            case "PYTHON" -> cmd.append("python3 ").append(codeFile.getFileName());
            case "NODE" -> cmd.append("node ").append(codeFile.getFileName());
            case "JAVA" -> {
                String className = extractJavaClassName(code);
                cmd.append("sh -c 'javac ").append(codeFile.getFileName());
                cmd.append(" && java ").append(className).append("'");
            }
            case "C" -> cmd.append("gcc -o main ").append(codeFile.getFileName()).append(" && ./main");
            case "CPP" -> cmd.append("g++ -o main ").append(codeFile.getFileName()).append(" && ./main");
            case "GO" -> cmd.append("go run ").append(codeFile.getFileName());
            case "RUST" -> cmd.append("rustc -o main ").append(codeFile.getFileName()).append(" && ./main");
            case "RUBY" -> cmd.append("ruby ").append(codeFile.getFileName());
            case "TYPESCRIPT" -> cmd.append("npx ts-node ").append(codeFile.getFileName());
            case "PHP" -> cmd.append("php ").append(codeFile.getFileName());
            case "KOTLIN" -> cmd.append("kotlinc ").append(codeFile.getFileName()).append(" -include-runtime -d main.jar && java -jar main.jar");
            case "SWIFT" -> cmd.append("swift ").append(codeFile.getFileName());
            case "R" -> cmd.append("Rscript ").append(codeFile.getFileName());
            default -> cmd.append(entryPoint).append(" ").append(codeFile.getFileName());
        }
        
        if (stdinFile != null) {
            cmd.append(" < ").append(stdinFile.getFileName());
        }
        
        return cmd.toString();
    }

    private String getFileExtension(String language) {
        return switch (language.toUpperCase()) {
            case "PYTHON" -> ".py";
            case "NODE", "TYPESCRIPT" -> ".js";
            case "JAVA", "KOTLIN" -> ".java";
            case "C" -> ".c";
            case "CPP" -> ".cpp";
            case "GO" -> ".go";
            case "RUST" -> ".rs";
            case "RUBY" -> ".rb";
            case "PHP" -> ".php";
            case "SWIFT" -> ".swift";
            case "R" -> ".R";
            default -> ".txt";
        };
    }

    private String executeCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getInputStream().readAllBytes();
            p.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Command execution failed: {}", command);
        }
        return "";
    }

    private long estimateMemoryUsage(String output) {
        return memoryLimitMb * 512;
    }

    private String extractJavaClassName(String code) {
        if (code == null || code.isBlank()) {
            return "Main";
        }
        String[] lines = code.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("public class ")) {
                return line.replace("public class ", "").replace("{", "").trim();
            }
        }
        return "Main";
    }

    public record DockerExecutionResult(int exitCode, String stdout, String stderr, long executionTimeMs, long memoryUsedKb) {}
}