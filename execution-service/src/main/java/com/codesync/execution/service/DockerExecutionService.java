package com.codesync.execution.service;

import com.codesync.execution.entity.ExecutionLanguage;
import com.codesync.execution.exception.BadRequestException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DockerExecutionService {
    private static final Logger log = LoggerFactory.getLogger(DockerExecutionService.class);

    @Value("${codesync.execution.timeout-seconds:10}")
    private int timeoutSeconds;

    @Value("${codesync.execution.python-command:python}")
    private String pythonCommand;

    @Value("${codesync.execution.node-command:node}")
    private String nodeCommand;

    @Value("${codesync.execution.java-command:java}")
    private String javaCommand;

    /** Tracks active OS processes by jobId for hard-kill cancellation. */
    private final ConcurrentHashMap<String, Process> activeProcesses = new ConcurrentHashMap<>();

    /**
     * Run code in an ephemeral working directory.
     *
     * @param jobId    unique job identifier used for cancellation tracking
     * @param code     source code to execute
     * @param language target runtime
     * @param stdin    optional standard input to pipe into the process (may be null/blank)
     */
    public ExecutionRunResult runInEphemeralEnvironment(
            String jobId, String code, ExecutionLanguage language, String stdin) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Code is required");
        }
        try {
            Path workDir = Files.createTempDirectory("codesync-exec-");
            try {
                List<String> command = buildCommand(workDir, code, language);
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(workDir.toFile());
                // Do NOT merge streams — capture stdout and stderr separately
                Process process = pb.start();
                activeProcesses.put(jobId, process);

                // Pipe stdin asynchronously before reading output (prevents deadlock)
                if (stdin != null && !stdin.isBlank()) {
                    try (OutputStream os = process.getOutputStream()) {
                        os.write(stdin.getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    } catch (IOException ignored) {
                        // Process may have already exited
                    }
                }
                // Close stdin to signal EOF - programs waiting on input will get empty input
                try {
                    process.getOutputStream().close();
                } catch (IOException ignored) {
                    // Already closed
                }

                // Read stdout and stderr concurrently to prevent buffer deadlocks
                ExecutorService ioPool = Executors.newFixedThreadPool(2);
                Future<String> stdoutFuture = ioPool.submit(
                        () -> new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                Future<String> stderrFuture = ioPool.submit(
                        () -> new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
                ioPool.shutdown();

                boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
                activeProcesses.remove(jobId);

                String stdoutStr = "";
                String stderrStr = "";
                try {
                    stdoutStr = stdoutFuture.get();
                    stderrStr = stderrFuture.get();
                } catch (Exception ignored) {
                    // Best-effort; process output may be partial
                }

                if (!finished) {
                    process.destroyForcibly();
                    ioPool.shutdownNow();
                    return new ExecutionRunResult(124, stdoutStr + System.lineSeparator() + "[timeout]", stderrStr);
                }

                int exit = process.exitValue();
                return new ExecutionRunResult(exit, stdoutStr, stderrStr);
            } finally {
                activeProcesses.remove(jobId);
                deleteRecursive(workDir);
            }
        } catch (IOException e) {
            log.warn("Execution IO failure", e);
            activeProcesses.remove(jobId);
            return new ExecutionRunResult(1, "", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            activeProcesses.remove(jobId);
            return new ExecutionRunResult(1, "", "Interrupted");
        }
    }

    /**
     * Hard-kill the OS process for a running job.
     * Safe to call even if the job has already finished.
     */
    public void cancelJob(String jobId) {
        Process process = activeProcesses.remove(jobId);
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            log.info("Forcibly killed process for job {}", jobId);
        }
    }

    private List<String> buildCommand(Path workDir, String code, ExecutionLanguage language) throws IOException {
        List<String> cmd = new ArrayList<>();
        switch (language) {
            case PYTHON -> {
                Path script = workDir.resolve("main.py");
                Files.writeString(script, code, StandardCharsets.UTF_8);
                cmd.add(pythonCommand);
                cmd.add(script.toAbsolutePath().toString());
            }
            case NODE -> {
                Path script = workDir.resolve("main.js");
                Files.writeString(script, code, StandardCharsets.UTF_8);
                cmd.add(nodeCommand);
                cmd.add(script.toAbsolutePath().toString());
            }
            case JAVA -> {
                String className = extractJavaClassName(code);
                Path mainScript = workDir.resolve(className + ".java");
                Files.writeString(mainScript, code, StandardCharsets.UTF_8);

                // Compile all .java files in the working directory - explicitly list files
                List<Path> javaFiles = Files.list(workDir)
                        .filter(p -> p.toString().endsWith(".java"))
                        .toList();
                if (javaFiles.isEmpty()) {
                    throw new IOException("No Java files found");
                }
                List<String> compileCmd = new ArrayList<>();
                compileCmd.add(javaCommand.replace("java", "javac"));
                javaFiles.forEach(f -> compileCmd.add(f.toAbsolutePath().toString()));

                ProcessBuilder compilePb = new ProcessBuilder(compileCmd);
                compilePb.directory(workDir.toFile());
                compilePb.redirectErrorStream(true);
                Process compileProcess = compilePb.start();
                String compileOutput = new String(compileProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                try {
                    int compileExit = compileProcess.waitFor();
                    if (compileExit != 0) {
                        throw new IOException("Compilation failed: " + compileOutput);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Compilation interrupted");
                }
                // Run the compiled class with current directory in classpath
                cmd.add(javaCommand);
                cmd.add("-cp");
                cmd.add(".");
                cmd.add(className);
            }
            default -> throw new BadRequestException("Unsupported language");
        }
        return cmd;
    }

    private String extractJavaClassName(String code) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("public\\s+class\\s+([A-Za-z0-9_]+)").matcher(code);
        if (m.find()) {
            return m.group(1);
        }
        return "Main";
    }

    private void deleteRecursive(Path root) {
        try {
            if (Files.isDirectory(root)) {
                try (var stream = Files.walk(root)) {
                    stream.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                            // best-effort cleanup
                        }
                    });
                }
            }
        } catch (IOException ignored) {
            // best-effort
        }
    }

    public record ExecutionRunResult(int exitCode, String stdout, String stderr) {}
}

